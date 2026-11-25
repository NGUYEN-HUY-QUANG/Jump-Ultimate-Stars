package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
import com.HUY.JumpUltimateStars.character.Fighter;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.effect.Effect;
import com.HUY.JumpUltimateStars.hethong.BotController;
import com.HUY.JumpUltimateStars.hethong.FightCamera;
import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.hethong.PlayerController;
import com.HUY.JumpUltimateStars.map.MapData;
import com.HUY.JumpUltimateStars.map.MapInfo;
import com.HUY.JumpUltimateStars.ui.HUD;
import com.HUY.JumpUltimateStars.ui.HitCounter;
import com.HUY.JumpUltimateStars.ui.RoundManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Màn hình trận đấu. Chỉ điều phối: tạo map + 2 Fighter + camera + HUD + RoundManager,
 * mỗi frame update rồi vẽ. (Xem ghi chú chi tiết ở bản cũ: thứ tự vẽ, hiệu ứng tối, va chạm...)
 *
 * CÁC CHẾ ĐỘ:
 *  - PVP:      2 người chơi thật.
 *  - PVE:      P1 là người, P2 là BotController (tự đánh).
 *  - TRAINING: 2 người chơi thật, hp / energy tự hồi đầy sau TRAINING_REFILL_DELAY giây,
 *              không có K.O nên không kết thúc round.
 *
 * Tài nguyên tạo trong show() và PHẢI dispose() trong dispose().
 */
public class GameScreen implements Screen {

    private static final String P1_MARKER_PATH = "HUD/1P.png";
    private static final String P2_MARKER_PATH = "HUD/2P.png";

    // Hiệu ứng tối khi ultimate
    private static final float DARK_MAX_ALPHA = 0.8f;
    private static final float DARK_FADE_SPEED = 4f;

    // Luyện tập: yên bao lâu thì tự hồi đầy (giây)
    private static final float TRAINING_REFILL_DELAY = 0.5f;

    private final MainGame game;
    private final CharacterSelectScreen.Mode mode;
    private String p1Id;
    private String p2Id;
    private final MapData map;

    private FightCamera fightCam;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private CharacterAnimationManager animManager;
    private HUD hud;
    private RoundManager rounds;
    private HitCounter hitCounter;

    private Fighter fighter1;
    private Fighter fighter2;

    // Bot điều khiển P2, chỉ khác null ở chế độ PVE
    private BotController bot;

    private float worldW, worldH;

    // Hiệu ứng tối
    private float darkTimer;
    private Fighter darkCaster;
    private float dimAlpha;
    private boolean ult1Was, ult2Was;

    // Luyện tập: theo dõi để biết khi nào "yên" (index 0 = P1, 1 = P2)
    private final float[] hpTimer = new float[2];
    private final float[] energyTimer = new float[2];
    private final int[] lastHp = new int[2];
    private final float[] lastEnergy = new float[2];

    public GameScreen(MainGame game, CharacterSelectScreen.Mode mode,
                      String p1Id, String p2Id, MapInfo mapInfo) {
        this.game = game;
        this.mode = mode;
        this.p1Id = p1Id;
        this.p2Id = p2Id;
        this.map = new MapData(mapInfo);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        animManager = new CharacterAnimationManager();

        map.load();
        worldW = map.getWorldWidth();
        worldH = map.getWorldHeight();

        fightCam = new FightCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), worldW, worldH);
        createFighters();
        fightCam.snapTo(fighter1, fighter2);

        hud = new HUD(p1Id, p2Id,
            findData(p1Id).avatar,
            p2Id != null ? findData(p2Id).avatar : null);
        hud.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        rounds = new RoundManager();
        rounds.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        hitCounter = new HitCounter();
        hitCounter.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /** Tạo (hoặc tạo lại) 2 fighter với máu đầy, đúng chỗ xuất phát. */
    private void createFighters() {
        if (fighter1 != null) fighter1.dispose();
        if (fighter2 != null) fighter2.dispose();
        bot = null;

        fighter1 = new Fighter(animManager, p1Id, PlayerController.p1(), map.getP1SpawnX(), false);
        fighter1.setMap(map);
        fighter1.setGroundY(map.getGroundY());
        fighter1.setMarker(P1_MARKER_PATH);

        if (p2Id != null) {
            // PVE: P2 do bot điều khiển. Còn lại: người chơi thật
            PlayerController c2;
            if (mode == CharacterSelectScreen.Mode.PVE) {
                bot = new BotController(BotController.Difficulty.HARD);
                c2 = bot;
            } else {
                c2 = PlayerController.p2();
            }

            fighter2 = new Fighter(animManager, p2Id, c2, map.getP2SpawnX(), true);
            fighter2.setMap(map);
            fighter2.setGroundY(map.getGroundY());
            fighter2.setMarker(P2_MARKER_PATH);

            fighter1.setOpponent(fighter2);
            fighter2.setOpponent(fighter1);

            if (bot != null) {
                bot.setSelf(fighter2);
                bot.setTarget(fighter1);
                bot.setMap(map);
            }
        }

        // Reset hiệu ứng tối
        darkTimer = 0f;
        dimAlpha = 0f;
        darkCaster = null;
        ult1Was = false;
        ult2Was = false;

        // Reset theo dõi luyện tập
        for (int i = 0; i < 2; i++) {
            hpTimer[i] = 0f;
            energyTimer[i] = 0f;
            lastHp[i] = -1;
            lastEnergy[i] = -1f;
        }

        if (hitCounter != null) hitCounter.reset();
    }

    /** Sang round mới: tạo lại fighter nhưng giữ nguyên energy của 2 bên. */
    private void startNextRound() {
        float e1 = fighter1.getEnergy();
        float e2 = fighter2 != null ? fighter2.getEnergy() : 0f;

        createFighters();
        fighter1.addEnergy(e1);
        if (fighter2 != null) fighter2.addEnergy(e2);

        fightCam.clearFocus();
        fightCam.snapTo(fighter1, fighter2);
    }

    /** Giữ dạng hiện tại khi tạo Fighter mới ở round tiếp theo và đồng bộ HUD. */
    private void syncCharacterForms() {
        String currentP1Id = fighter1.getCharId();
        if (!p1Id.equals(currentP1Id)) {
            p1Id = currentP1Id;
            hud.setP1Character(p1Id, findData(p1Id).avatar);
        }

        if (fighter2 != null) {
            String currentP2Id = fighter2.getCharId();
            if (!p2Id.equals(currentP2Id)) {
                p2Id = currentP2Id;
                hud.setP2Character(p2Id, findData(p2Id).avatar);
            }
        }
    }

    private static CharacterData findData(String id) {
        for (CharacterData d : CharacterRegistry.ALL) {
            if (d.id.equals(id)) return d;
        }
        throw new IllegalArgumentException("Không có nhân vật: " + id);
    }

    private static void applyEffectHits(Fighter attacker, Fighter target) {
        Array<Effect> effects = attacker.getEffects();
        for (int i = 0; i < effects.size; i++) {
            if (target.isInvulnerable()) continue;

            Effect e = effects.get(i);
            int dmg = e.checkHit(target.getHurtbox());
            if (dmg > 0) {
                target.takeDamage(dmg, attacker.getCenterX(), e.isKnockdown());
                attacker.addEnergy(e.getEnergy());
                attacker.addHitCount();
            }
        }
    }

    private void startDark(Fighter f) {
        if (darkTimer > 0f) return;
        float t = f.getUltimateDarkTime();
        if (t > 0f) {
            darkTimer = t;
            darkCaster = f;
        }
    }

    /** Luyện tập: mất máu / dùng energy xong, yên TRAINING_REFILL_DELAY giây thì tự đầy lại. */
    private void trainingRefill(Fighter f, int i, float delta) {
        float maxEnergy = CharacterStats.ENERGY_PER_BAR * CharacterStats.MAX_ENERGY_BARS;

        // Máu
        if (f.getHp() < f.getMaxHp()) {
            if (f.getHp() != lastHp[i]) hpTimer[i] = 0f;   // vừa đổi -> đếm lại từ đầu
            hpTimer[i] += delta;
            if (hpTimer[i] >= TRAINING_REFILL_DELAY) f.restoreHp();
        } else {
            hpTimer[i] = 0f;
        }
        lastHp[i] = f.getHp();

        // Energy
        if (f.getEnergy() < maxEnergy) {
            if (f.getEnergy() != lastEnergy[i]) energyTimer[i] = 0f;
            energyTimer[i] += delta;
            if (energyTimer[i] >= TRAINING_REFILL_DELAY) f.addEnergy(maxEnergy);   // addEnergy tự kẹp max
        } else {
            energyTimer[i] = 0f;
        }
        lastEnergy[i] = f.getEnergy();
    }

    /** Hai nhân vật không được chồng lên nhau. Nhảy qua đầu hoặc lướt thì qua được. */
    private void separateFighters() {
        if (fighter2 == null) return;
        if (fighter1.isPhasing() || fighter2.isPhasing()) return;

        // Khác độ cao (nhảy qua đầu) thì không chặn
        if (Math.abs(fighter1.getFeetY() - fighter2.getFeetY()) >= fighter1.getBodyHeight()) return;

        float dx = fighter2.getCenterX() - fighter1.getCenterX();
        float overlap = (fighter1.getBodyWidth() + fighter2.getBodyWidth()) / 2f - Math.abs(dx);
        if (overlap <= 0f) return;

        float dir = dx >= 0f ? 1f : -1f;   // hướng từ P1 sang P2
        fighter1.pushX(-dir * overlap / 2f);
        fighter2.pushX(dir * overlap / 2f);
    }

    @Override
    public void render(float delta) {
        // --- Luồng round ---
        if (fighter2 != null) {
            Fighter winner = rounds.isWinnerP1() ? fighter1 : fighter2;
            rounds.setWinnerStatus(winner.isReadyForWinPose(), winner.isWinPoseDone());
        }

        rounds.update(delta);
        if (rounds.isMatchOver()) {
            game.setScreen(new MainScreen(game));
            dispose();
            return;
        }
        if (rounds.consumeNextRound()) startNextRound();

        if (rounds.consumeWinPose() && fighter2 != null) {
            (rounds.isWinnerP1() ? fighter1 : fighter2).playWinPose();
        }

        // Chưa PLAYING thì khóa phím của cả 2 (bot cũng bị khóa vì Fighter bỏ qua input)
        boolean locked = !rounds.isPlaying();
        fighter1.setInputLocked(locked);
        if (fighter2 != null) fighter2.setInputLocked(locked);

        // --- Hiệu ứng tối khi ultimate ---
        boolean ultimateDark = darkTimer > 0f;
        if (ultimateDark) {
            darkTimer -= delta;
            if (darkCaster != null && !darkCaster.isUsingUltimate() && !darkCaster.isTransforming()) {
                darkTimer = 0f;
            }
            ultimateDark = darkTimer > 0f;
        }
        fighter1.setUltimateDark(ultimateDark);
        if (fighter2 != null) fighter2.setUltimateDark(ultimateDark);

        // --- Bot quyết định TRƯỚC khi fighter update ---
        if (bot != null) bot.update(delta);

        // --- Cập nhật ---
        boolean transform1 = fighter1.isTransforming();
        boolean transform2 = fighter2 != null && fighter2.isTransforming();
        boolean dark = ultimateDark || transform1 || transform2;
        if (!dark || darkCaster == fighter1 || transform1) fighter1.update(delta, worldW);
        if (fighter2 != null && (!dark || darkCaster == fighter2 || transform2)) {
            fighter2.update(delta, worldW);
        }
        separateFighters();   // Fighter update xong mới biết vị trí cuối -> tách nhau ra
        syncCharacterForms();

        // Phát hiện ai vừa bắt đầu ultimate
        boolean u1 = fighter1.isUsingUltimate();
        if (u1 && !ult1Was) startDark(fighter1);
        ult1Was = u1;
        if (fighter2 != null) {
            boolean u2 = fighter2.isUsingUltimate();
            if (u2 && !ult2Was) startDark(fighter2);
            ult2Was = u2;
        }

        if (fighter1.isTransforming()) darkCaster = fighter1;
        else if (fighter2 != null && fighter2.isTransforming()) darkCaster = fighter2;
        dark = darkTimer > 0f || fighter1.isTransforming()
            || (fighter2 != null && fighter2.isTransforming());
        dimAlpha = dark ? Math.min(dimAlpha + DARK_FADE_SPEED * delta, DARK_MAX_ALPHA)
            : Math.max(dimAlpha - DARK_FADE_SPEED * delta, 0f);

        boolean training = mode == CharacterSelectScreen.Mode.TRAINING;

        // Va chạm effect, hồi đầy (luyện tập), K.O, rồi đẩy máu + energy lên HUD
        if (fighter2 != null) {
            if (!dark && rounds.isPlaying()) {
                applyEffectHits(fighter1, fighter2);
                applyEffectHits(fighter2, fighter1);
            }

            if (training) {
                trainingRefill(fighter1, 0, delta);
                trainingRefill(fighter2, 1, delta);
            }

            // Luyện tập thì không có K.O
            if (rounds.isPlaying() && !training) {
                if (fighter1.getHp() <= 0) {
                    fighter1.knockOut();
                    rounds.onKnockout(true);
                } else if (fighter2.getHp() <= 0) {
                    fighter2.knockOut();
                    rounds.onKnockout(false);
                }
            }
            hud.setP2Hp(fighter2.getHp());
            hud.setP2Energy(fighter2.getEnergy());
        }
        hud.setP1Hp(fighter1.getHp());
        hud.setP1Energy(fighter1.getEnergy());
        hud.setP1Wins(rounds.getP1Wins());
        hud.setP2Wins(rounds.getP2Wins());
        hud.update(delta);
        hitCounter.update(fighter1.getHitsDealt(), fighter2 != null ? fighter2.getHitsDealt() : 0, delta);

        // Camera
        if (rounds.isZoomPhase() && fighter2 != null) {
            fightCam.setFocus(rounds.isWinnerP1() ? fighter1 : fighter2);
        } else if (fighter1.isTransforming()) {
            fightCam.setFocus(fighter1);
        } else if (fighter2 != null && fighter2.isTransforming()) {
            fightCam.setFocus(fighter2);
        } else {
            fightCam.clearFocus();
        }
        fightCam.update(delta, fighter1, fighter2);

        // --- Vẽ ---
        OrthographicCamera camera = fightCam.getCamera();

        if (MapData.DEBUG_PLATFORMS && Gdx.input.justTouched()) {
            Vector3 v = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
            float[] px = map.worldToImagePixel(v.x, v.y);
            System.out.println("Pixel ảnh: x = " + Math.round(px[0]) + ", y(từ dưới lên) = " + Math.round(px[1]));
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        map.renderBg(batch);
        map.renderMap(batch);
        batch.end();

        shapeRenderer.begin(ShapeType.Filled);
        map.renderPlaceholder(shapeRenderer);
        shapeRenderer.end();

        if (dimAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, dimAlpha);
            shapeRenderer.rect(0f, 0f, worldW, worldH);
            shapeRenderer.end();
        }

        batch.begin();
        fighter1.draw(batch);
        if (fighter2 != null) fighter2.draw(batch);
        fighter1.drawMarker(batch);
        if (fighter2 != null) fighter2.drawMarker(batch);
        fighter1.drawEffects(batch);
        if (fighter2 != null) fighter2.drawEffects(batch);
        map.renderFront(batch);
        batch.end();

        if (rounds.isHudVisible()) {
            hud.render(batch);
            hitCounter.render(batch);
        }
        rounds.render(batch);
    }

    @Override
    public void resize(int width, int height) {
        fightCam.resize(width, height);
        hud.resize(width, height);
        rounds.resize(width, height);
        hitCounter.resize(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        animManager.dispose();
        fighter1.dispose();
        if (fighter2 != null) fighter2.dispose();
        map.dispose();
        hud.dispose();
        rounds.dispose();
        hitCounter.dispose();
    }
}
