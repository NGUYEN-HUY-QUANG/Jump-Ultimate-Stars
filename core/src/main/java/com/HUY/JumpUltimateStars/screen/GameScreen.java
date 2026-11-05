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
 * Màn hình trận đấu. Chỉ điều phối: tạo map + 2 Fighter, mỗi frame update rồi vẽ.
 * Logic nhân vật nằm ở Fighter, logic từng effect nằm trong class effect đó.
 *
 * Tài nguyên (batch, shapeRenderer, animManager, map, effect của Fighter) tạo trong show()
 * và PHẢI dispose() trong dispose(). Game.setScreen() chỉ gọi hide(), không tự dispose.
 *
 * TODO: va chạm. Khi Fighter có hurtbox và máu, thêm vào render():
 *   for (Effect e : fighter1.getEffects()) {
 *       int dmg = e.checkHit(fighter2.getHurtbox());
 *       if (dmg > 0) fighter2.takeDamage(dmg);
 *   }
 *   (và ngược lại fighter2 -> fighter1)
 */
public class GameScreen implements Screen {

    private final MainGame game;
    private final CharacterSelectScreen.Mode mode;
    private final String p1Id;
    private final String p2Id;      // null khi SINGLE
    private final MapData map;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private CharacterAnimationManager animManager;

    private Fighter fighter1;
    private Fighter fighter2;       // null nếu SINGLE

    // Chiều rộng cửa sổ lần resize trước, để co giãn vị trí nhân vật theo tỉ lệ
    private float lastW;

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
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        lastW = w;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        animManager = new CharacterAnimationManager();
        map.load();

        // P1 bên trái quay phải, P2 bên phải quay trái
        fighter1 = new Fighter(animManager, p1Id, PlayerController.p1(), map.getP1SpawnX(w), false);
        if (p2Id != null) {
            fighter2 = new Fighter(animManager, p2Id, PlayerController.p2(), map.getP2SpawnX(w), true);
        }
        updateGround(h);
    }

    private void updateGround(float h) {
        float groundY = map.getGroundY(h);
        fighter1.setGroundY(groundY);
        if (fighter2 != null) fighter2.setGroundY(groundY);
    }

    @Override
    public void render(float delta) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // --- Cập nhật ---
        fighter1.update(delta, w);
        if (fighter2 != null) fighter2.update(delta, w);
        // TODO: check va chạm effect (xem mô tả ở đầu class)

        // --- Vẽ ---
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Lớp 1: nền map
        batch.begin();
        map.renderBackground(batch, w, h);
        batch.end();

        // Lớp 2: đường đỏ làm đất tạm
        shapeRenderer.begin(ShapeType.Filled);
        map.renderPlaceholder(shapeRenderer, w, h);
        shapeRenderer.end();

        // Lớp 3: nhân vật, rồi effect đè lên
        batch.begin();
        fighter1.draw(batch);
        if (fighter2 != null) fighter2.draw(batch);
        fighter1.drawEffects(batch);
        if (fighter2 != null) fighter2.drawEffects(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        // onResize dùng lastW (cũ) TRƯỚC khi gán lastW mới
        fighter1.onResize(lastW, width);
        if (fighter2 != null) fighter2.onResize(lastW, width);
        lastW = width;
        updateGround(height);
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
    }
}
