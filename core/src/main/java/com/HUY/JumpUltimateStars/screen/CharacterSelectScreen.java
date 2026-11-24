package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.hethong.PlayerSelector;
import com.HUY.JumpUltimateStars.ui.CharacterSlot;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Man hinh chon nhan vat (character select).
 * Bo cuc: trai (preview P1) - giua (danh sach nhan vat) - phai (preview P2).
 *
 * 3 che do:
 *  - PVP:      2 nguoi that, moi nguoi 1 bo phim, chon cung luc.
 *  - PVE:      1 nguoi choi (P1). P1 chon xong thi P2 (bot) hien ra de P1 chon tiep
 *              (P2 dung chung phim cua P1).
 *  - TRAINING: giong PVP (2 nguoi that), khac nhau o trong tran.
 *
 * Chon xong thi sang MapSelectScreen, roi moi vao GameScreen.
 */
public class CharacterSelectScreen implements Screen {

    /** Che do: PVP (2 nguoi), PVE (1 nguoi + bot), TRAINING (2 nguoi, hp/energy luon day). */
    public enum Mode { PVP, PVE, TRAINING }

    private final MainGame game;
    private final Mode mode;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture backgroundTexture;
    private ShapeRenderer shapeRenderer;

    // Font rieng, nho, chi de ve chu "P1"/"P2" phia tren slot
    private BitmapFont indicatorFont;
    private GlyphLayout layout;

    private final List<CharacterSlot> slots = new ArrayList<>();

    private float[] slotX;
    private float[] slotY;

    private static final float LEFT_RATIO  = 0.25f;
    private static final float MID_RATIO   = 0.5f;
    private static final float RIGHT_RATIO = 0.25f;

    private static final float SLOT_SIZE = 60f;
    private static final float SLOT_GAP  = 15f;

    private CharacterAnimationManager animManager;

    // Nguoi choi dang active. PVE: luc dau chi co P1, P2 duoc add sau khi P1 confirm
    private final List<PlayerSelector> players = new ArrayList<>();
    private PlayerSelector p1;
    private PlayerSelector p2;

    private static final float PREVIEW_BASELINE_Y = 200f;
    private static final float P1_OFFSET_X = 60f;
    private static final float P2_OFFSET_X = -60f;

    private static final Color P1_COLOR = new Color(0.2f, 0.55f, 1f, 1f); // xanh duong
    private static final Color P2_COLOR = new Color(1f, 0.25f, 0.25f, 1f); // do

    private static final float INDICATOR_GAP = 6f;
    private static final float TRIANGLE_SIZE = 10f;
    private static final float LABEL_SAME_SLOT_OFFSET = 22f;

    public CharacterSelectScreen(MainGame game, Mode mode) {
        this.game = game;
        this.mode = mode;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        backgroundTexture = new Texture("background/character_select_screen.png");
        shapeRenderer = new ShapeRenderer();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font_chinh.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = "P12";
        parameter.size = 20;
        parameter.color = Color.WHITE;
        parameter.borderWidth = 2;
        parameter.borderColor = Color.BLACK;
        indicatorFont = generator.generateFont(parameter);
        generator.dispose();
        layout = new GlyphLayout();

        for (CharacterData data : CharacterRegistry.ALL) {
            slots.add(new CharacterSlot(data));
        }
        slotX = new float[slots.size()];
        slotY = new float[slots.size()];
        layoutSlots();

        animManager = new CharacterAnimationManager();

        p1 = new PlayerSelector(PlayerSelector.Id.P1, P1_COLOR,
            Keys.A, Keys.D, Keys.J, false, P1_OFFSET_X, animManager);
        players.add(p1);

        if (mode == Mode.PVE) {
            // P2 la bot nhung do P1 chon nen dung chung phim cua P1.
            // CHUA add vao players, chi add sau khi P1 confirm xong (xem checkAllConfirmed)
            p2 = new PlayerSelector(PlayerSelector.Id.P2, P2_COLOR,
                Keys.A, Keys.D, Keys.J, true, P2_OFFSET_X, animManager);
        } else {
            // PVP va TRAINING: 2 nguoi that, moi nguoi 1 bo phim
            p2 = new PlayerSelector(PlayerSelector.Id.P2, P2_COLOR,
                Keys.LEFT, Keys.RIGHT, Keys.NUM_1, true, P2_OFFSET_X, animManager);
            players.add(p2);
        }

        for (PlayerSelector p : players) {
            p.refreshPreview(slots);
        }
        repositionPreviews();
        updateSlotHighlights();
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
            slotX[i] = x;
            slotY[i] = y;
        }

        repositionPreviews();
    }

    private void repositionPreviews() {
        float screenW = Gdx.graphics.getWidth();
        float p1CenterX = screenW * LEFT_RATIO / 2f;
        float p2CenterX = screenW * (LEFT_RATIO + MID_RATIO) + screenW * RIGHT_RATIO / 2f;
        if (p1 != null) p1.positionPreview(p1CenterX, PREVIEW_BASELINE_Y);
        if (p2 != null) p2.positionPreview(p2CenterX, PREVIEW_BASELINE_Y);
    }

    /* ========================= INPUT ========================= */

    private void handleInput() {
        for (PlayerSelector p : players) {
            if (p.handleMove(slots)) {
                p.refreshPreview(slots);
                repositionPreviews();
            }
            if (p.isConfirmPressed() && !p.isLocked()) {
                p.startConfirm();
                CharacterSlot slot = slots.get(p.getIndex());
                if (slot.getState() != CharacterSlot.State.CONFIRMING
                    && slot.getState() != CharacterSlot.State.CONFIRMED) {
                    slot.setState(CharacterSlot.State.CONFIRMING);
                }
            }
        }
        updateSlotHighlights();
    }

    private void updateSlotHighlights() {
        for (CharacterSlot s : slots) {
            if (s.getState() == CharacterSlot.State.SELECTED) {
                s.setState(CharacterSlot.State.NORMAL);
            }
        }
        for (PlayerSelector p : players) {
            CharacterSlot slot = slots.get(p.getIndex());
            if (slot.getState() == CharacterSlot.State.NORMAL) {
                slot.setState(CharacterSlot.State.SELECTED);
            }
        }
    }

    /**
     * PVE: P1 confirm xong thi cho P2 (bot) hien ra de P1 chon tiep, chua chuyen man hinh.
     * Con lai: tat ca nguoi choi active da CONFIRMED thi sang MapSelectScreen.
     */
    private boolean checkAllConfirmed() {
        if (mode == Mode.PVE && !players.contains(p2)) {
            if (p1.isConfirmed()) {
                players.add(p2);
                p2.refreshPreview(slots);
                repositionPreviews();
                updateSlotHighlights();
            }
            return false;   // chua xong, con phai cho P2
        }

        for (PlayerSelector p : players) {
            if (!p.isConfirmed()) return false;
        }
        String p1Id = slots.get(p1.getIndex()).getData().id;
        String p2Id = slots.get(p2.getIndex()).getData().id;

        game.setScreen(new MapSelectScreen(game, mode, p1Id, p2Id));
        dispose();
        return true;
    }

    /* ========================= RENDER ========================= */

    @Override
    public void render(float delta) {
        handleInput();

        for (CharacterSlot s : slots) s.update(delta);
        for (PlayerSelector p : players) p.update(delta);

        if (checkAllConfirmed()) return;

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
        renderIndicatorTriangles();
        shapeRenderer.end();

        batch.begin();
        for (CharacterSlot s : slots) s.renderAvatar(batch);
        for (PlayerSelector p : players) p.draw(batch);
        renderIndicatorLabels();
        batch.end();
    }

    private void renderIndicatorTriangles() {
        for (PlayerSelector p : players) {
            int i = p.getIndex();
            float cx = slotX[i] + SLOT_SIZE / 2f + getIndicatorXOffset(p);
            float topY = slotY[i] + SLOT_SIZE;

            shapeRenderer.setColor(p.getColor());
            shapeRenderer.triangle(
                cx - TRIANGLE_SIZE / 2f, topY + INDICATOR_GAP + TRIANGLE_SIZE,
                cx + TRIANGLE_SIZE / 2f, topY + INDICATOR_GAP + TRIANGLE_SIZE,
                cx,                      topY + INDICATOR_GAP
            );
        }
    }

    private void renderIndicatorLabels() {
        for (PlayerSelector p : players) {
            int i = p.getIndex();
            float cx = slotX[i] + SLOT_SIZE / 2f + getIndicatorXOffset(p);
            float topY = slotY[i] + SLOT_SIZE;

            indicatorFont.setColor(p.getColor());
            layout.setText(indicatorFont, p.getLabel());
            indicatorFont.draw(batch, layout,
                cx - layout.width / 2f,
                topY + INDICATOR_GAP + TRIANGLE_SIZE + layout.height + 4f);
        }
    }

    // P1 va P2 cung tro 1 slot -> tach sang 2 ben. P2 chua hien (PVE) thi khong can tach.
    private float getIndicatorXOffset(PlayerSelector p) {
        if (!players.contains(p2) || p1.getIndex() != p2.getIndex()) return 0f;
        return p == p1 ? -LABEL_SAME_SLOT_OFFSET : LABEL_SAME_SLOT_OFFSET;
    }

    /* ========================= VONG DOI SCREEN ========================= */

    @Override
    public void resize(int width, int height) {
        layoutSlots();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        indicatorFont.dispose();
        animManager.dispose();
    }
}
