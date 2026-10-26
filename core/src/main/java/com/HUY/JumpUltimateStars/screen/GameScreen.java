package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.Fighter;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.hethong.PlayerController;
import com.HUY.JumpUltimateStars.map.MapData;
import com.HUY.JumpUltimateStars.map.MapInfo;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Màn hình trận đấu.
 *
 * Nhận từ CharacterSelectScreen: chế độ chơi, id nhân vật của P1/P2 và map đã chọn.
 * Màn hình này chỉ điều phối: tạo map + các Fighter, mỗi frame gọi update rồi vẽ theo lớp
 * (nền map -> đường đỏ tạm -> nhân vật). Logic di chuyển/animation nằm trong Fighter,
 * đọc phím nằm trong PlayerController, vị trí map nằm trong MapData.
 *
 * Vòng đời tài nguyên: batch, shapeRenderer, animManager, map đều được tạo trong show()
 * và PHẢI dispose() trong dispose(). Game.setScreen() chỉ gọi hide(), không tự dispose,
 * nên nơi nào rời khỏi GameScreen sau này phải gọi dispose() thủ công.
 */
public class GameScreen implements Screen {

    // Giữ tham chiếu để sau này chuyển màn hình (quay lại menu, màn kết quả...)
    private final MainGame game;

    // SINGLE = chỉ P1, DUO = P1 + P2 (hiện chưa dùng, sẽ cần khi làm PVE/đối thủ AI)
    private final CharacterSelectScreen.Mode mode;

    // Id nhân vật đã chọn (vd "naruto_base"). p2Id = null khi chế độ SINGLE
    private final String p1Id;
    private final String p2Id;

    // Dữ liệu + ảnh của map đang chơi (Texture bên trong cần load() và dispose())
    private final MapData map;

    private OrthographicCamera camera;
    private SpriteBatch batch;           // vẽ ảnh: nền map, nhân vật
    private ShapeRenderer shapeRenderer; // vẽ hình: đường đỏ làm đất tạm

    // Cache animation dùng chung cho cả 2 Fighter (dispose 1 lần ở dispose())
    private CharacterAnimationManager animManager;

    private Fighter fighter1;
    private Fighter fighter2; // null nếu SINGLE

    // Chiều rộng cửa sổ ở lần resize trước, dùng để co giãn vị trí nhân vật theo tỉ lệ
    private float lastW;

    public GameScreen(MainGame game, CharacterSelectScreen.Mode mode,
                      String p1Id, String p2Id, MapInfo mapInfo) {
        this.game = game;
        this.mode = mode;
        this.p1Id = p1Id;
        this.p2Id = p2Id;
        // Chỉ bọc MapInfo, chưa load Texture. Việc load làm trong show() (cần có GL context)
        this.map = new MapData(mapInfo);
    }

    /** Được gọi khi màn hình bắt đầu hiển thị: khởi tạo tài nguyên và spawn nhân vật. */
    @Override
    public void show() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        lastW = w;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        animManager = new CharacterAnimationManager();
        map.load();

        // P1 bên trái quay phải (không flip), P2 bên phải quay trái (flip).
        // Vị trí spawn do map quyết định (fix cứng theo từng map)
        fighter1 = new Fighter(animManager, p1Id, PlayerController.p1(),
            map.getP1SpawnX(w), false);
        if (p2Id != null) {
            fighter2 = new Fighter(animManager, p2Id, PlayerController.p2(),
                map.getP2SpawnX(w), true);
        }
        updateGround(h);
    }

    /** Báo cho các Fighter biết độ cao mặt đất hiện tại (đổi theo chiều cao cửa sổ). */
    private void updateGround(float h) {
        float groundY = map.getGroundY(h);
        fighter1.setGroundY(groundY);
        if (fighter2 != null) fighter2.setGroundY(groundY);
    }

    /** Mỗi frame: cập nhật logic trước, rồi vẽ. */
    @Override
    public void render(float delta) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // --- Cập nhật logic (input, di chuyển, animation) ---
        fighter1.update(delta, w);
        if (fighter2 != null) fighter2.update(delta, w);

        // --- Vẽ ---
        ScreenUtils.clear(0f, 0f, 0f, 1f); // nền đen
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Lớp 1: ảnh nền map (map tạm chưa có ảnh thì bỏ qua)
        batch.begin();
        map.renderBackground(batch, w, h);
        batch.end();

        // Lớp 2: đường đỏ làm đất tạm (map có ảnh rồi thì tự tắt)
        shapeRenderer.begin(ShapeType.Filled);
        map.renderPlaceholder(shapeRenderer, w, h);
        shapeRenderer.end();

        // Lớp 3: nhân vật, vẽ sau cùng để nằm trên nền
        batch.begin();
        fighter1.draw(batch);
        if (fighter2 != null) fighter2.draw(batch);
        batch.end();
    }

    /** Đổi kích thước cửa sổ: cập nhật camera, co giãn vị trí nhân vật, tính lại mặt đất. */
    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        // Phải gọi onResize với lastW (cũ) TRƯỚC khi gán lastW = width (mới)
        fighter1.onResize(lastW, width);
        if (fighter2 != null) fighter2.onResize(lastW, width);
        lastW = width;
        updateGround(height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    /** Giải phóng toàn bộ tài nguyên native (GPU/texture). Không tự được GC dọn. */
    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        animManager.dispose();
        map.dispose();
    }
}
