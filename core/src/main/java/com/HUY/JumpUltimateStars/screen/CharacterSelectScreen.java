package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.Character;
import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
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

/**
 * Màn hình chọn nhân vật (character select).
 * Bố cục màn hình chia làm 3 cột: trái (preview P1) - giữa (danh sách nhân vật) - phải (preview P2).
 */
public class CharacterSelectScreen implements Screen {

    private final MainGame game;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture backgroundTexture;

    private ShapeRenderer shapeRenderer;

    private List<CharacterSlot> slots = new ArrayList<>();

    private static final float LEFT_RATIO  = 0.25f;
    private static final float MID_RATIO   = 0.5f;
    private static final float RIGHT_RATIO = 0.25f;

    private static final float SLOT_SIZE = 60f;
    private static final float SLOT_GAP  = 15f;

    private int p1Index = 0;

    // ===== Preview nhân vật (animation đứng yên) cho P1/P2 =====
    private CharacterAnimationManager animManager;
    private Character p1Preview;
    private Character p2Preview;

    private static final float IDLE_FRAME_DURATION = 0.12f;

    // Chỉnh 2 giá trị này để đổi kích thước và độ cao chân nhân vật preview.
    private static final float PREVIEW_SCALE = 4f;      // <-- chỉnh to/nhỏ nhân vật ở đây
    private static final float PREVIEW_BASELINE_Y = 200f; // <-- chỉnh độ cao chân nhân vật ở đây

    // Lệch thêm theo trục X riêng cho từng cột nếu cần (để dịch gần/xa danh sách giữa hơn).
    private static final float P1_OFFSET_X = 60f;
    private static final float P2_OFFSET_X = -60f;

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

        p1Index = 0;
        slots.get(p1Index).setState(CharacterSlot.State.SELECTED);

        animManager = new CharacterAnimationManager();
        refreshPreviews();
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

        if (p1Preview != null) positionPreview(p1Preview, true, P1_OFFSET_X);
        if (p2Preview != null) positionPreview(p2Preview, false, P2_OFFSET_X);
    }

    /**
     * Tạo (hoặc thay) Character preview cho P1 và P2 theo nhân vật đang được chọn,
     * rồi cho chạy animation "idle". Gọi khi mới show() và mỗi khi lựa chọn P1 đổi.
     */
    private void refreshPreviews() {
        String charId = slots.get(p1Index).getData().id;

        p1Preview = new Character(animManager, charId);
        p1Preview.playAnimation("idle", IDLE_FRAME_DURATION);
        p1Preview.setFlipX(false);
        p1Preview.setScale(PREVIEW_SCALE);
        positionPreview(p1Preview, true, P1_OFFSET_X);

        // TODO: chưa có input riêng cho P2, tạm cho P2 đi theo lựa chọn của P1.
        // Khi thêm p2Index + input riêng, đổi dòng dưới thành: slots.get(p2Index).getData().id
        p2Preview = new Character(animManager, charId);
        p2Preview.playAnimation("idle", IDLE_FRAME_DURATION);
        p2Preview.setFlipX(true);
        p2Preview.setScale(PREVIEW_SCALE);
        positionPreview(p2Preview, false, P2_OFFSET_X);
    }

    /**
     * Canh giữa Character preview trong cột trái (P1) hoặc cột phải (P2),
     * dựa theo width THẬT của frame hiện tại (đã nhân scale), không hardcode.
     */
    private void positionPreview(Character c, boolean isLeftColumn, float offsetX) {
        float screenW = Gdx.graphics.getWidth();
        float centerX = isLeftColumn
            ? screenW * LEFT_RATIO / 2f
            : screenW * (LEFT_RATIO + MID_RATIO) + screenW * RIGHT_RATIO / 2f;

        float w = c.getCurrentFrameWidth();
        c.setPosition(centerX - w / 2f + offsetX, PREVIEW_BASELINE_Y);
    }

    private void handleInput() {
        CharacterSlot current = slots.get(p1Index);

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

        if (newIndex < 0) newIndex = 0;
        if (newIndex >= slots.size()) newIndex = slots.size() - 1;

        if (newIndex != p1Index) {
            current.setState(CharacterSlot.State.NORMAL);
            p1Index = newIndex;
            slots.get(p1Index).setState(CharacterSlot.State.SELECTED);

            refreshPreviews();
        }

        if (Gdx.input.isKeyJustPressed(Keys.J)) {
            slots.get(p1Index).setState(CharacterSlot.State.CONFIRMING);
        }
    }

    @Override
    public void render(float delta) {

        handleInput();

        for (CharacterSlot s : slots) s.update(delta);
        p1Preview.update(delta);
        p2Preview.update(delta);

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
        p1Preview.draw(batch);
        p2Preview.draw(batch);
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
        animManager.dispose();
    }
}
