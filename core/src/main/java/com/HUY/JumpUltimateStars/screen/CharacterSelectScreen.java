package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.ui.CharacterSlot;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

public class CharacterSelectScreen implements Screen {

    private final MainGame game;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture backgroundTexture;

    private ShapeRenderer shapeRenderer;
    private List<CharacterSlot> slots = new ArrayList<>();

    // tỉ lệ chia màn hình: trái (preview P1) - giữa (danh sách) - phải (preview P2)
    private static final float LEFT_RATIO  = 0.25f;
    private static final float MID_RATIO   = 0.5f;
    private static final float RIGHT_RATIO = 0.25f;

    private static final float SLOT_SIZE = 60f;
    private static final float SLOT_GAP  = 15f;

    // P1 đang chọn ô nào
    private int p1Index = 0;

    public CharacterSelectScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {

        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        backgroundTexture = new Texture("background/character_select_screen.png");

        shapeRenderer = new ShapeRenderer();

        for (CharacterData data : CharacterRegistry.ALL) {
            slots.add(new CharacterSlot(data));
        }
        layoutSlots();

        // tự động chọn sẵn nhân vật đầu tiên khi vào màn hình
        p1Index = 0;
        slots.get(p1Index).setState(CharacterSlot.State.SELECTED);
    }

    private void layoutSlots() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float midX = screenW * LEFT_RATIO;
        float midW = screenW * MID_RATIO;

        int count = slots.size();
        float totalW = count * SLOT_SIZE + (count - 1) * SLOT_GAP;
        float startX = midX + (midW - totalW) / 2f;
        float y = screenH / 2f - SLOT_SIZE / 2f;

        for (int i = 0; i < count; i++) {
            float x = startX + i * (SLOT_SIZE + SLOT_GAP);
            slots.get(i).setPosition(x, y, SLOT_SIZE);
        }
    }

    private void handleInput() {
        CharacterSlot current = slots.get(p1Index);

        // đã xác nhận rồi thì khóa, không cho di chuyển/xác nhận lại nữa
        if (current.getState() == CharacterSlot.State.CONFIRMING
            || current.getState() == CharacterSlot.State.CONFIRMED) {
            return;
        }

        int newIndex = p1Index;

        if (Gdx.input.isKeyJustPressed(Keys.A)) {
            newIndex = p1Index - 1;
        } else if (Gdx.input.isKeyJustPressed(Keys.D)) {
            newIndex = p1Index + 1;
        }

        // giới hạn trong khoảng danh sách, không cho lố ra ngoài
        if (newIndex < 0) newIndex = 0;
        if (newIndex >= slots.size()) newIndex = slots.size() - 1;

        if (newIndex != p1Index) {
            current.setState(CharacterSlot.State.NORMAL);
            p1Index = newIndex;
            slots.get(p1Index).setState(CharacterSlot.State.SELECTED);
        }

        if (Gdx.input.isKeyJustPressed(Keys.J)) {
            slots.get(p1Index).setState(CharacterSlot.State.CONFIRMING);
        }
    }

    @Override
    public void render(float delta) {

        handleInput();

        for (CharacterSlot s : slots) s.update(delta);

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        shapeRenderer.begin(ShapeType.Filled);
        for (CharacterSlot s : slots) s.renderGlow(shapeRenderer);
        for (CharacterSlot s : slots) s.renderBorder(shapeRenderer);
        shapeRenderer.end();

        batch.begin();
        for (CharacterSlot s : slots) s.renderAvatar(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        layoutSlots();
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        batch.dispose();
        shapeRenderer.dispose();
    }
}
