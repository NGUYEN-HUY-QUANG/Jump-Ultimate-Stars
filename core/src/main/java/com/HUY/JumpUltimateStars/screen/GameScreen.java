package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
import com.HUY.JumpUltimateStars.character.Fighter;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.hethong.FightCamera;
import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.hethong.PlayerController;
import com.HUY.JumpUltimateStars.map.MapData;
import com.HUY.JumpUltimateStars.map.MapInfo;
import com.HUY.JumpUltimateStars.ui.HUD;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Màn hình trận đấu. Chỉ điều phối: tạo map + 2 Fighter + camera + HUD, mỗi frame update rồi vẽ.
 * Logic nhân vật nằm ở Fighter, logic từng effect nằm trong class effect đó,
 * logic zoom / theo dõi nằm ở FightCamera, phần hiển thị thanh máu / thời gian / avatar nằm ở HUD.
 *
 * THẾ GIỚI: kích thước = ảnh map (worldW x worldH). Fighter, đất, platform, effect đều
 * tính theo pixel thế giới. Cửa sổ chỉ là khung nhìn do FightCamera điều khiển, nên đổi
 * kích thước cửa sổ KHÔNG làm nhân vật hay đất bị lệch.
 *
 * Marker P1 / P2 trên đầu nhân vật do Fighter vẽ (toạ độ thế giới, đi theo nhân vật và zoom theo camera).
 *
 * HUD vẽ theo toạ độ MÀN HÌNH (camera riêng), luôn nằm trên cùng, không bị zoom theo FightCamera.
 *
 * Tài nguyên (batch, shapeRenderer, animManager, map, hud, effect + marker của Fighter) tạo trong show()
 * và PHẢI dispose() trong dispose(). Game.setScreen() chỉ gọi hide(), không tự dispose.
 *
 * TODO: va chạm. Khi Fighter có hurtbox và máu, thêm vào render():
 *   for (Effect e : fighter1.getEffects()) {
 *       int dmg = e.checkHit(fighter2.getHurtbox());
 *       if (dmg > 0) fighter2.takeDamage(dmg);
 *   }
 *   (và ngược lại fighter2 -> fighter1)
 *   rồi cập nhật HUD: hud.setP1Hp(fighter1.getHp()); hud.setP2Hp(fighter2.getHp());
 */
public class GameScreen implements Screen {

    // Đường dẫn ảnh marker trên đầu nhân vật (chỉnh cho đúng thư mục asset)
    private static final String P1_MARKER_PATH = "HUD/1P.png";
    private static final String P2_MARKER_PATH = "HUD/2P.png";

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

    private Fighter fighter1;
    private Fighter fighter2;       // null nếu SINGLE

    // Kích thước thế giới (= ảnh map), lấy sau map.load()
    private float worldW, worldH;

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
        }

        fightCam = new FightCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), worldW, worldH);
        fightCam.snapTo(fighter1, fighter2);

        // HUD: avatar lấy từ CharacterData (cùng ảnh màn chọn nhân vật), P2 null khi SINGLE
        hud = new HUD(p1Id, p2Id,
            findData(p1Id).avatar,
            p2Id != null ? findData(p2Id).avatar : null);
        hud.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /** Tìm dữ liệu nhân vật theo id trong CharacterRegistry. */
    private static CharacterData findData(String id) {
        for (CharacterData d : CharacterRegistry.ALL) {
            if (d.id.equals(id)) return d;
        }
        throw new IllegalArgumentException("Không có nhân vật: " + id);
    }

    @Override
    public void render(float delta) {
        // --- Cập nhật (worldW/worldH thay cho kích thước cửa sổ) ---
        fighter1.update(delta, worldW);
        if (fighter2 != null) fighter2.update(delta, worldW);
        // TODO: check va chạm effect (xem mô tả ở đầu class)

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

        // Lớp 1: nền map (vẽ đúng kích thước thế giới)
        batch.begin();
        map.renderBackground(batch);
        batch.end();

        // Lớp 2: đường đỏ làm đất tạm / đường debug platform
        shapeRenderer.begin(ShapeType.Filled);
        map.renderPlaceholder(shapeRenderer);
        shapeRenderer.end();

        // Lớp 3: nhân vật, marker P1/P2 trên đầu, rồi effect đè lên
        batch.begin();
        fighter1.draw(batch);
        if (fighter2 != null) fighter2.draw(batch);
        fighter1.drawMarker(batch);
        if (fighter2 != null) fighter2.drawMarker(batch);
        fighter1.drawEffects(batch);
        if (fighter2 != null) fighter2.drawEffects(batch);
        batch.end();

        // Lớp 4: HUD (toạ độ màn hình, tự begin/end và tự đặt projection riêng)
        hud.render(batch);
    }

    @Override
    public void resize(int width, int height) {
        // Chỉ đổi khung nhìn; vị trí nhân vật là tọa độ thế giới nên không cần co giãn
        fightCam.resize(width, height);
        hud.resize(width, height);
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
    }
}
