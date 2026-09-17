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
 * Bo cuc man hinh chia lam 3 cot: trai (preview P1) - giua (danh sach nhan vat) - phai (preview P2).
 *
 * Ho tro 2 che do:
 *  - Mode.SINGLE: chi P1 duoc chon (dung cho PVE, Luyen tap, va Vuot ai sau nay)
 *  - Mode.DUO:    ca P1 va P2 cung chon (dung cho PVP)
 *
 * Moi nguoi choi (P1/P2) duoc dai dien boi 1 PlayerSelector (xem package hethong),
 * gom index dang chon + preview + phim dieu khien rieng. Man hinh nay chi dong vai
 * tro dieu phoi: giu danh sach slot + danh sach PlayerSelector, dong bo trang thai
 * CharacterSlot theo tung nguoi choi, va ve them tam giac + chu "P1"/"P2" phia tren
 * slot dang duoc tro toi.
 */
public class CharacterSelectScreen implements Screen {

    /** Che do man hinh: chi P1 hay ca P1+P2. */
    public enum Mode { SINGLE, DUO }

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

    // Luu rieng vi tri tung slot (CharacterSlot khong expose getX()/getY())
    // de ve tam giac/label dung cho slot tuong ung.
    private float[] slotX;
    private float[] slotY;

    private static final float LEFT_RATIO  = 0.25f;
    private static final float MID_RATIO   = 0.5f;
    private static final float RIGHT_RATIO = 0.25f;

    private static final float SLOT_SIZE = 60f;
    private static final float SLOT_GAP  = 15f;

    private CharacterAnimationManager animManager;

    // Danh sach nguoi choi active: SINGLE = chi P1, DUO = P1 + P2
    private final List<PlayerSelector> players = new ArrayList<>();
    private PlayerSelector p1;
    private PlayerSelector p2; // null khi Mode.SINGLE

    private static final float PREVIEW_BASELINE_Y = 200f;
    private static final float P1_OFFSET_X = 60f;
    private static final float P2_OFFSET_X = -60f;

    // Mau P1/P2 (tam thoi, se custom lai sau)
    private static final Color P1_COLOR = new Color(0.2f, 0.55f, 1f, 1f); // xanh duong
    private static final Color P2_COLOR = new Color(1f, 0.25f, 0.25f, 1f); // do

    private static final float INDICATOR_GAP = 6f;    // khoang cach tu dinh slot toi tam giac
    private static final float TRIANGLE_SIZE = 10f;
    private static final float LABEL_SAME_SLOT_OFFSET = 22f; // P1/P2 cung slot -> tach sang 2 ben

    public CharacterSelectScreen(MainGame game, Mode mode) {
        this.game = game;
        this.mode = mode;
    }

    // Constructor cu, mac dinh SINGLE, de cho cac cho con dang goi
    // CharacterSelectScreen(game) khong bi loi bien dich.
    public CharacterSelectScreen(MainGame game) {
        this(game, Mode.SINGLE);
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

        if (mode == Mode.DUO) {
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

    /**
     * Tinh toan va gan vi tri (x, y) cho tung slot nhan vat o cot giua,
     * canh giua theo chieu ngang trong vung MID_RATIO cua man hinh.
     * Duoc goi lai moi khi resize() man hinh.
     */
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

    /**
     * Xu ly di chuyen + xac nhan cho tat ca nguoi choi active.
     * KHONG check "da confirm het chua" o day - viec do can chay SAU khi
     * CharacterSlot.update() da chay trong frame nay (xem render()),
     * vi CONFIRMING co the vua tu chuyen thanh CONFIRMED ngay trong frame do.
     */
    private void handleInput() {
        for (PlayerSelector p : players) {
            if (p.handleMove(slots)) {
                p.refreshPreview(slots);
                repositionPreviews();
            }
            if (p.isConfirmPressed()) {
                CharacterSlot slot = slots.get(p.getIndex());
                if (slot.getState() != CharacterSlot.State.CONFIRMING
                    && slot.getState() != CharacterSlot.State.CONFIRMED) {
                    slot.setState(CharacterSlot.State.CONFIRMING);
                }
            }
        }
        updateSlotHighlights();
    }

    // Dong bo trang thai SELECTED cua slot theo vi tri hien tai cua tung
    // nguoi choi, khong dong vao slot dang CONFIRMING/CONFIRMED.
    // Luu y: neu P1 va P2 cung tro vao 1 slot, slot do van duoc coi la
    // SELECTED (cho phep 2 nguoi chon trung nhan vat). Neu sau nay muon
    // cam trung nhan vat thi chan ngay trong PlayerSelector.handleMove().
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
     * Kiem tra tat ca nguoi choi active (P1, hoac P1+P2 neu DUO) da o
     * trang thai CONFIRMED hay chua. CharacterSlot tu chuyen CONFIRMING
     * -> CONFIRMED sau FLASH_DURATION giay (xem CharacterSlot.update()),
     * nen chi can doc state la du, khong can tu quan ly timer o day.
     */
    private void checkAllConfirmed() {
        for (PlayerSelector p : players) {
            if (slots.get(p.getIndex()).getState() != CharacterSlot.State.CONFIRMED) {
                return;
            }
        }
        // TODO: tat ca da confirm -> chuyen sang man hinh gameplay
        // game.setScreen(new GameScreen(game, mode, p1.getIndex(),
        //     p2 != null ? p2.getIndex() : -1));
    }

    /* ========================= RENDER ========================= */

    @Override
    public void render(float delta) {
        handleInput();

        // update slot TRUOC checkAllConfirmed(), vi CONFIRMING co the vua
        // tu chuyen thanh CONFIRMED ngay trong lan update nay.
        for (CharacterSlot s : slots) s.update(delta);
        for (PlayerSelector p : players) p.update(delta);

        checkAllConfirmed();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Lop 1: nen
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // Lop 2: glow + border cua slot + tam giac chi P1/P2
        shapeRenderer.begin(ShapeType.Filled);
        for (CharacterSlot s : slots) s.renderGlow(shapeRenderer);
        for (CharacterSlot s : slots) s.renderBorder(shapeRenderer);
        renderIndicatorTriangles();
        shapeRenderer.end();

        // Lop 3: avatar + preview P1/P2 + chu "P1"/"P2"
        batch.begin();
        for (CharacterSlot s : slots) s.renderAvatar(batch);
        for (PlayerSelector p : players) p.draw(batch);
        renderIndicatorLabels();
        batch.end();
    }

    /** Ve tam giac chi xuong dau slot ma tung nguoi choi dang tro toi. */
    private void renderIndicatorTriangles() {
        for (PlayerSelector p : players) {
            int i = p.getIndex();
            float cx = slotX[i] + SLOT_SIZE / 2f + getIndicatorXOffset(p);
            float topY = slotY[i] + SLOT_SIZE;

            shapeRenderer.setColor(p.getColor());
            // tam giac huong XUONG, dinh nam sat tren canh tren cua slot
            shapeRenderer.triangle(
                cx - TRIANGLE_SIZE / 2f, topY + INDICATOR_GAP + TRIANGLE_SIZE,
                cx + TRIANGLE_SIZE / 2f, topY + INDICATOR_GAP + TRIANGLE_SIZE,
                cx,                      topY + INDICATOR_GAP
            );
        }
    }

    /** Ve chu "P1"/"P2" phia tren tam giac, ngay tren slot dang duoc tro toi. */
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

    // P1 va P2 cung tro vao 1 slot -> tach label/tam giac sang 2 ben
    // de khong de len nhau.
    private float getIndicatorXOffset(PlayerSelector p) {
        if (p2 == null || p1.getIndex() != p2.getIndex()) return 0f;
        return p == p1 ? -LABEL_SAME_SLOT_OFFSET : LABEL_SAME_SLOT_OFFSET;
    }

    /* ========================= VONG DOI SCREEN ========================= */

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
        indicatorFont.dispose();
        animManager.dispose();
    }
}
