package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.map.MapData;
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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Màn hình chọn map. Giống MainScreen nhưng cuộn NGANG (A/D, J để chọn),
 * mỗi mục là 1 map thay vì chữ. Chọn xong thì vào GameScreen.
 *
 * Ảnh map ở đây không có file ghép sẵn: mỗi map được vẽ chồng 3 lớp bg -> map -> front
 * cùng 1 khung (ITEM_W x ITEM_H).
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

    // mapTextures[i] = {bg, map, front} của map thứ i (bg và front có thể null)
    private Texture[][] mapTextures;

    // Giống MainScreen: target = vị trí logic vô hạn, scroll = vị trí đang hiển thị (lerp)
    private float scrollPosition = 0f;
    private float targetPosition = 0f;

    private float centerX, centerY;
    private final Rectangle mapBounds = new Rectangle();
    private final Rectangle scissors = new Rectangle();

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

        mapTextures = new Texture[MapRegistry.ALL.size()][];
        for (int i = 0; i < mapTextures.length; i++) {
            MapInfo m = MapRegistry.ALL.get(i);
            mapTextures[i] = new Texture[] {
                loadOptional(m.bgPath), loadOptional(m.mapPath), loadOptional(m.frontPath)
            };
        }

        centerX = Gdx.graphics.getWidth() / 2f;
        centerY = Gdx.graphics.getHeight() / 2f;
    }

    /** Load texture nếu file tồn tại, không có thì trả null. */
    private static Texture loadOptional(String path) {
        if (!Gdx.files.internal(path).exists()) return null;
        return new Texture(Gdx.files.internal(path));
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

        // Lớp 1: nền + các map (mỗi map vẽ chồng bg -> map -> front)
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        int size = mapTextures.length;
        for (int i = 0; i < size; i++) {
            float d = shortestCircularDistance(i, scrollPosition, size);
            if (Math.abs(d) > 1.5f) continue;

            float scale = 1f - 0.25f * Math.min(Math.abs(d), 1f);           // càng xa càng nhỏ
            float alpha = MathUtils.clamp(1f - Math.abs(d) * 0.6f, 0f, 1f);  // càng xa càng mờ
            float w = ITEM_W * scale, h = ITEM_H * scale;

            // Hệ số thu nhỏ chung cho cả 3 lớp: vừa khít ảnh map chính vào khung, giữ nguyên tỉ lệ
            Texture mainTex = mapTextures[i][1];
            if (mainTex == null) continue;
            mapBounds.set(centerX + d * SPACING - w / 2f, centerY - h / 2f, w, h);
            ScissorStack.calculateScissors(camera, batch.getTransformMatrix(), mapBounds, scissors);
            batch.flush();
            if (!ScissorStack.pushScissors(scissors)) continue;

            float fit = Math.min(w / mainTex.getWidth(), h / mainTex.getHeight());
            float x = centerX + d * SPACING - mainTex.getWidth() * fit / 2f;   // góc dưới trái của map
            float y = centerY - mainTex.getHeight() * fit / 2f;

            // Kích thước map chính đang hiển thị; bg và front tính theo nó, giống hệt trong trận
            float mapW = mainTex.getWidth() * fit, mapH = mainTex.getHeight() * fit;

            batch.setColor(1f, 1f, 1f, alpha);
            Texture bgTex = mapTextures[i][0], frontTex = mapTextures[i][2];
            if (bgTex != null) {
                float s = MapData.layerScale(bgTex, mapW, mapH, true);
                batch.draw(bgTex, x, y, bgTex.getWidth() * s, bgTex.getHeight() * s);
            }
            batch.draw(mainTex, x, y, mapW, mapH);
            if (frontTex != null) {
                float s = MapData.layerScale(frontTex, mapW, mapH, false);
                batch.draw(frontTex, x, y, frontTex.getWidth() * s, frontTex.getHeight() * s);
            }
            batch.flush();
            ScissorStack.popScissors();
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
        for (Texture[] layers : mapTextures)
            for (Texture t : layers) if (t != null) t.dispose();
    }
}
