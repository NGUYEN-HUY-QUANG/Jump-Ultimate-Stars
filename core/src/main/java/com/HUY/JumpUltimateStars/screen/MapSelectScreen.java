package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.map.MapInfo;
import com.HUY.JumpUltimateStars.map.MapRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Màn hình chọn map. Giống MainScreen nhưng cuộn NGANG (A/D, J để chọn),
 * mỗi mục là 1 ảnh map thay vì chữ. Chọn xong thì vào GameScreen.
 */
public class MapSelectScreen implements Screen {

    private static final float ITEM_W = 320f;
    private static final float ITEM_H = 180f;
    private static final float SPACING = 380f;   // khoảng cách ngang giữa 2 ảnh liên tiếp
    private static final float BORDER = 4f;

    private final MainGame game;
    private final CharacterSelectScreen.Mode mode;
    private final String p1Id;
    private final String p2Id;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Texture backgroundTexture;
    private Texture[] mapTextures;

    // Giống MainScreen: target = vị trí logic vô hạn, scroll = vị trí đang hiển thị (lerp)
    private float scrollPosition = 0f;
    private float targetPosition = 0f;

    private float centerX, centerY;

    public MapSelectScreen(MainGame game, CharacterSelectScreen.Mode mode, String p1Id, String p2Id) {
        this.game = game;
        this.mode = mode;
        this.p1Id = p1Id;
        this.p2Id = p2Id;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        backgroundTexture = new Texture("background/map_select_screen.png");

        mapTextures = new Texture[MapRegistry.ALL.size()];
        for (int i = 0; i < mapTextures.length; i++) {
            String path = MapRegistry.ALL.get(i).backgroundPath;
            if (path != null) mapTextures[i] = new Texture(Gdx.files.internal(path));
        }

        centerX = Gdx.graphics.getWidth() / 2f;
        centerY = Gdx.graphics.getHeight() / 2f;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) targetPosition--;
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) targetPosition++;
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            selectMap();
            return;
        }
        scrollPosition = MathUtils.lerp(scrollPosition, targetPosition, 5f * delta);

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Lớp 1: nền + ảnh các map
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        int size = mapTextures.length;
        for (int i = 0; i < size; i++) {
            float d = shortestCircularDistance(i, scrollPosition, size);
            if (Math.abs(d) > 1.5f || mapTextures[i] == null) continue;

            float scale = 1f - 0.25f * Math.min(Math.abs(d), 1f);           // càng xa càng nhỏ
            float alpha = MathUtils.clamp(1f - Math.abs(d) * 0.6f, 0f, 1f);  // càng xa càng mờ
            float w = ITEM_W * scale, h = ITEM_H * scale;

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(mapTextures[i], centerX + d * SPACING - w / 2f, centerY - h / 2f, w, h);
        }
        batch.setColor(Color.WHITE); // trả lại màu, tránh ảnh hưởng chỗ khác
        batch.end();

        // Lớp 2: viền trắng quanh map đang chọn (đứng yên ở giữa)
        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        float bx = centerX - ITEM_W / 2f - BORDER, by = centerY - ITEM_H / 2f - BORDER;
        float bw = ITEM_W + BORDER * 2, bh = ITEM_H + BORDER * 2;
        shapeRenderer.rect(bx, by, bw, BORDER);                  // dưới
        shapeRenderer.rect(bx, by + bh - BORDER, bw, BORDER);    // trên
        shapeRenderer.rect(bx, by, BORDER, bh);                  // trái
        shapeRenderer.rect(bx + bw - BORDER, by, BORDER, bh);    // phải
        shapeRenderer.end();
    }

    private void selectMap() {
        int size = MapRegistry.ALL.size();
        int index = ((Math.round(targetPosition) % size) + size) % size;
        MapInfo info = MapRegistry.ALL.get(index);
        game.setScreen(new GameScreen(game, mode, p1Id, p2Id, info));
        dispose();
    }

    // Khoảng cách vòng tròn ngắn nhất từ index tới position (y hệt MainScreen)
    private float shortestCircularDistance(int index, float position, int size) {
        float raw = (index - position) % size;
        if (raw > size / 2f) raw -= size;
        if (raw < -size / 2f) raw += size;
        return raw;
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        backgroundTexture.dispose();
        for (Texture t : mapTextures) if (t != null) t.dispose();
    }
}
