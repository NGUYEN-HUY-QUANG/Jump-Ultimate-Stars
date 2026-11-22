package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
import com.HUY.JumpUltimateStars.character.Fighter;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.effect.Effect;
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
 * mỗi frame update rồi vẽ.
 * Logic nhân vật nằm ở Fighter, logic từng effect nằm trong class effect đó,
 * logic zoom / theo dõi nằm ở FightCamera, phần hiển thị thanh máu / energy / thời gian / avatar nằm ở HUD,
 * luồng round (ROUND, FIGHT, K.O, WINNER, đếm thắng) nằm ở RoundManager.
 *
 * THẾ GIỚI: kích thước = ảnh map (worldW x worldH). Fighter, đất, platform, effect đều
 * tính theo pixel thế giới. Cửa sổ chỉ là khung nhìn do FightCamera điều khiển.
 *
 * THỨ TỰ VẼ: bg -> map -> (đường debug) -> lớp tối -> nhân vật -> marker -> effect -> front -> HUD -> hiệu ứng round.
 *
 * HUD và hiệu ứng round vẽ theo toạ độ MÀN HÌNH (camera riêng), không bị zoom theo FightCamera.
 * HUD tắt khi zoom vào người thắng (ZOOM / WINNER).
 *
 * VÒNG ROUND: mỗi round tạo lại 2 Fighter (máu đầy, về chỗ xuất phát) nhưng GIỮ energy.
 * Chưa PLAYING (intro / sau K.O) thì khóa phím cả 2. Hết máu -> Fighter.knockOut() + RoundManager.onKnockout().
 * Thắng đủ số round thì về MainScreen.
 *
 * VA CHẠM: đòn cận chiến / skill / ultimate do Fighter tự xét hitbox với đối thủ (setOpponent).
 * Effect (phi tiêu, kame...) thì GameScreen xét mỗi frame bằng applyEffectHits().
 * Máu và energy lấy từ Fighter đẩy lên HUD mỗi frame.
 *
 * HIỆU ỨNG TỐI KHI ULTIMATE: Fighter vừa bước vào ULTIMATE mà chiêu có darkTime > 0 thì map tối
 * trong darkTime giây. Trong lúc tối chỉ người tung chiêu được update, đối thủ đứng im hoàn toàn,
 * va chạm effect tạm tắt. Lớp tối phủ lên bg + map, nhân vật và front vẽ sau nên vẫn sáng.
 *
 * Tài nguyên (batch, shapeRenderer, animManager, map, hud, rounds, effect + marker của Fighter) tạo trong show()
 * và PHẢI dispose() trong dispose(). Game.setScreen() chỉ gọi hide(), không tự dispose.
 */
public class GameScreen implements Screen {

    // Đường dẫn ảnh marker trên đầu nhân vật (chỉnh cho đúng thư mục asset)
    private static final String P1_MARKER_PATH = "HUD/1P.png";
    private static final String P2_MARKER_PATH = "HUD/2P.png";

    // Hiệu ứng tối khi ultimate
    private static final float DARK_MAX_ALPHA = 0.8f;   // độ tối của map
    private static final float DARK_FADE_SPEED = 4f;    // tốc độ tối dần / sáng dần (alpha mỗi giây)

    private final MainGame game;
    private final CharacterSelectScreen.Mode mode;
    private final String p1Id;
    private final String p2Id;      // null khi SINGLE
    private final MapData map;

    private FightCamera fightCam;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private CharacterAnimationManager animManager;
    private HUD hud;
    private RoundManager rounds;
    private HitCounter hitCounter;

    private Fighter fighter1;
    private Fighter fighter2;       // null nếu SINGLE

    // Kích thước thế giới (= ảnh map), lấy sau map.load()
    private float worldW, worldH;

    // Trạng thái hiệu ứng tối
    private float darkTimer;        // thời gian tối còn lại
    private Fighter darkCaster;     // người đang tung ultimate
    private float dimAlpha;         // độ tối đang hiển thị (fade)
    private boolean ult1Was, ult2Was;   // frame trước đó có đang ultimate không (để bắt lúc vừa bắt đầu)

    public GameScreen(MainGame game, CharacterSelectScreen.Mode mode,
                      String p1Id, String p2Id, MapInfo mapInfo) {
        this.game = game;
        this.mode = mode;
        this.p1Id = p1Id;
        this.p2Id = p2Id;
        this.map = new MapData(mapInfo);   // chưa load Texture, việc đó làm trong show()
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

        // HUD: avatar lấy từ CharacterData (cùng ảnh màn chọn nhân vật), P2 null khi SINGLE
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

        // P1 bên trái quay phải, P2 bên phải quay trái
        fighter1 = new Fighter(animManager, p1Id, PlayerController.p1(), map.getP1SpawnX(), false);
        fighter1.setMap(map);
        fighter1.setGroundY(map.getGroundY());
        fighter1.setMarker(P1_MARKER_PATH);
        if (p2Id != null) {
            fighter2 = new Fighter(animManager, p2Id, PlayerController.p2(), map.getP2SpawnX(), true);
            fighter2.setMap(map);
            fighter2.setGroundY(map.getGroundY());
            fighter2.setMarker(P2_MARKER_PATH);

            // Gắn đối thủ cho nhau để đòn cận chiến biết đánh ai
            fighter1.setOpponent(fighter2);
            fighter2.setOpponent(fighter1);
        }

        // Reset hiệu ứng tối
        darkTimer = 0f;
        dimAlpha = 0f;
        darkCaster = null;
        ult1Was = false;
        ult2Was = false;
        if (hitCounter != null) hitCounter.reset();   // Fighter mới nên tổng hit về 0
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

    /** Tìm dữ liệu nhân vật theo id trong CharacterRegistry. */
    private static CharacterData findData(String id) {
        for (CharacterData d : CharacterRegistry.ALL) {
            if (d.id.equals(id)) return d;
        }
        throw new IllegalArgumentException("Không có nhân vật: " + id);
    }

    /** Effect của attacker chạm hurtbox của target thì trừ máu target (có thể knockdown) và hồi energy cho attacker. */
    private static void applyEffectHits(Fighter attacker, Fighter target) {
        Array<Effect> effects = attacker.getEffects();
        for (int i = 0; i < effects.size; i++) {
            if (target.isInvulnerable()) continue;   // đang lướt / knockout / đứng dậy: effect bay xuyên qua

            Effect e = effects.get(i);
            int dmg = e.checkHit(target.getHurtbox());
            if (dmg > 0) {
                target.takeDamage(dmg, attacker.getCenterX(), e.isKnockdown());
                attacker.addEnergy(e.getEnergy());
                attacker.addHitCount();   // Fighter đếm số hit để HitCounter hiển thị
            }
        }
    }

    /** Fighter vừa bắt đầu ultimate thì bật tối nếu chiêu có darkTime (đang có người tối rồi thì bỏ qua). */
    private void startDark(Fighter f) {
        if (darkTimer > 0f) return;
        float t = f.getUltimateDarkTime();
        if (t > 0f) {
            darkTimer = t;
            darkCaster = f;
        }
    }

    @Override
    public void render(float delta) {
        // --- Luồng round ---
        // Báo RoundManager: người thắng đã rảnh chưa, pose winner đã xong chưa
        if (fighter2 != null) {
            Fighter winner = rounds.isWinnerP1() ? fighter1 : fighter2;
            rounds.setWinnerStatus(winner.isReadyForWinPose(), winner.isWinPoseDone());
        }

        rounds.update(delta);
        if (rounds.isMatchOver()) {
            game.setScreen(new MainScreen(game));   // sửa theo constructor MainScreen của bạn
            dispose();
            return;
        }
        if (rounds.consumeNextRound()) startNextRound();

        // Tới lúc người thắng làm pose winner
        if (rounds.consumeWinPose() && fighter2 != null) {
            (rounds.isWinnerP1() ? fighter1 : fighter2).playWinPose();
        }

        // Chưa PLAYING (intro / sau K.O) thì khóa phím của cả 2
        boolean locked = !rounds.isPlaying();
        fighter1.setInputLocked(locked);
        if (fighter2 != null) fighter2.setInputLocked(locked);

        // --- Hiệu ứng tối khi ultimate ---
        boolean dark = darkTimer > 0f;
        if (dark) {
            darkTimer -= delta;
            // Người tung chiêu bị ngắt giữa chừng thì hết tối luôn
            if (darkCaster != null && !darkCaster.isUsingUltimate()) darkTimer = 0f;
            dark = darkTimer > 0f;
        }
        fighter1.setUltimateDark(dark);
        if (fighter2 != null) fighter2.setUltimateDark(dark);
        dimAlpha = dark ? Math.min(dimAlpha + DARK_FADE_SPEED * delta, DARK_MAX_ALPHA)
            : Math.max(dimAlpha - DARK_FADE_SPEED * delta, 0f);

        // --- Cập nhật: đang tối thì chỉ người tung chiêu chạy, người kia đứng im ---
        if (!dark || darkCaster == fighter1) fighter1.update(delta, worldW);
        if (fighter2 != null && (!dark || darkCaster == fighter2)) fighter2.update(delta, worldW);

        // Phát hiện ai vừa bắt đầu ultimate
        boolean u1 = fighter1.isUsingUltimate();
        if (u1 && !ult1Was) startDark(fighter1);
        ult1Was = u1;
        if (fighter2 != null) {
            boolean u2 = fighter2.isUsingUltimate();
            if (u2 && !ult2Was) startDark(fighter2);
            ult2Was = u2;
        }

        // Va chạm effect (tạm tắt khi đang tối / chưa PLAYING), rồi đẩy máu + energy lên HUD
        // (đòn cận chiến đã được Fighter tự xử lý trong update)
        if (fighter2 != null) {
            if (!dark && rounds.isPlaying()) {
                applyEffectHits(fighter1, fighter2);
                applyEffectHits(fighter2, fighter1);
            }
            // Có người hết máu -> knockdown luôn + báo RoundManager (cộng thắng, bật K.O)
            if (rounds.isPlaying()) {
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

        // Camera: zoom vào người thắng lúc ZOOM / WINNER
        if (rounds.isZoomPhase() && fighter2 != null) {
            fightCam.setFocus(rounds.isWinnerP1() ? fighter1 : fighter2);
        } else {
            fightCam.clearFocus();
        }
        fightCam.update(delta, fighter1, fighter2);   // SAU khi fighter đã update

        // --- Vẽ ---
        OrthographicCamera camera = fightCam.getCamera();

        // Debug: click chuột -> in tọa độ pixel ảnh (gốc dưới-trái) để chép vào MapRegistry
        if (MapData.DEBUG_PLATFORMS && Gdx.input.justTouched()) {
            Vector3 v = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
            float[] px = map.worldToImagePixel(v.x, v.y);
            System.out.println("Pixel ảnh: x = " + Math.round(px[0]) + ", y(từ dưới lên) = " + Math.round(px[1]));
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Lớp 1: nền sau (bg) rồi map chính, đều vẽ đúng kích thước thế giới
        batch.begin();
        map.renderBg(batch);
        map.renderMap(batch);
        batch.end();

        // Lớp 2: đường đỏ làm đất tạm / đường debug platform
        shapeRenderer.begin(ShapeType.Filled);
        map.renderPlaceholder(shapeRenderer);
        shapeRenderer.end();

        // Lớp 2.5: phủ tối lên bg + map (nhân vật và front vẽ sau nên vẫn sáng)
        if (dimAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, dimAlpha);
            shapeRenderer.rect(0f, 0f, worldW, worldH);
            shapeRenderer.end();
        }

        // Lớp 3: nhân vật, marker P1/P2 trên đầu, effect đè lên, cuối cùng là front đè lên tất cả
        batch.begin();
        fighter1.draw(batch);
        if (fighter2 != null) fighter2.draw(batch);
        fighter1.drawMarker(batch);
        if (fighter2 != null) fighter2.drawMarker(batch);
        fighter1.drawEffects(batch);
        if (fighter2 != null) fighter2.drawEffects(batch);
        map.renderFront(batch);
        batch.end();

        // Lớp 4: HUD (tắt khi zoom vào người thắng) rồi hiệu ứng ROUND / FIGHT / K.O / WINNER
        if (rounds.isHudVisible()) {
            hud.render(batch);
            hitCounter.render(batch);
        }
        rounds.render(batch);
    }

    @Override
    public void resize(int width, int height) {
        // Chỉ đổi khung nhìn; vị trí nhân vật là tọa độ thế giới nên không cần co giãn
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
