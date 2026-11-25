package com.HUY.JumpUltimateStars.hethong;

import com.HUY.JumpUltimateStars.character.Character;
import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.ui.CharacterSlot;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

/**
 * Dai dien cho 1 nguoi choi (P1 hoac P2) trong CharacterSelectScreen.
 *
 * Gom rieng cho tung nguoi choi:
 *  - index dang tro toi trong danh sach slot
 *  - preview nhan vat (Character) o cot trai/phai
 *  - phim dieu khien rieng (trai/phai + xac nhan)
 *  - mau + huong preview (P2 lat mat de nhin vao P1)
 *
 * Tach class nay de CharacterSelectScreen dung CHUNG 1 logic cho ca
 * P1 va P2 thay vi copy 2 lan. Muon them nguoi choi thu 3/4 sau nay
 * chi can tao them 1 PlayerSelector, khong dong gi den logic cu.
 *
 * Dat trong package hethong (cung MainGame) vi day la 1 thanh phan
 * "he thong" dung chung cho nhieu man hinh chon nhan vat sau nay
 * (PVP, PVE, Luyen tap, Vuot ai...), khong rieng gi 1 Screen cu the.
 */
public class PlayerSelector {

    public enum Id { P1, P2 }

    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float PREVIEW_SCALE = 4f;

    private final Id id;
    private final Color color;
    private final int keyLeft;
    private final int keyRight;
    private final int keyConfirm;
    private final boolean flipPreview;
    private final float offsetX;
    private final CharacterAnimationManager animManager;

    // Vi tri (index) dang tro toi trong danh sach slot nhan vat
    private int index = 0;

    // Preview nhan vat dang duoc chon, hien o cot trai (P1) hoac phai (P2)
    private Character preview;

    private static final float CONFIRM_DURATION = 1f; // khớp FLASH_DURATION của CharacterSlot

    private boolean locked = false;     // đã bấm confirm (đang nhấp nháy hoặc xong)
    private boolean confirmed = false;  // đã confirm xong hẳn
    private float confirmTimer = 0f;

    public PlayerSelector(Id id, Color color, int keyLeft, int keyRight, int keyConfirm,
                          boolean flipPreview, float offsetX,
                          CharacterAnimationManager animManager) {
        this.id = id;
        this.color = color;
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
        this.keyConfirm = keyConfirm;
        this.flipPreview = flipPreview;
        this.offsetX = offsetX;
        this.animManager = animManager;
    }

    /**
     * Xu ly di chuyen trai/phai cua rieng nguoi choi nay.
     * Tra ve true neu index vua doi (goi noi nay xong thi CharacterSelectScreen
     * se goi refreshPreview() + repositionPreviews()).
     *
     * KHONG tu xu ly phim xac nhan o day, vi viec set state CONFIRMING
     * anh huong truc tiep den CharacterSlot dung chung giua nhieu nguoi choi,
     * nen de CharacterSelectScreen dieu phoi.
     */
    public boolean handleMove(List<CharacterSlot> slots) {
        if (locked) return false; // đã confirm thì không đổi nhân vật nữa

        int newIndex = index;
        if (Gdx.input.isKeyJustPressed(keyLeft)) newIndex--;
        if (Gdx.input.isKeyJustPressed(keyRight)) newIndex++;
        newIndex = MathUtils.clamp(newIndex, 0, slots.size() - 1);

        if (newIndex == index) return false;
        index = newIndex;
        return true;
    }

    public boolean isConfirmPressed() {
        return Gdx.input.isKeyJustPressed(keyConfirm);
    }

    /** Bắt đầu confirm cho riêng người chơi này. */
    public void startConfirm() {
        if (locked) return;
        locked = true;
        confirmTimer = 0f;
    }

    public boolean isLocked() { return locked; }
    public boolean isConfirmed() { return confirmed; }

    /** Tao lai Character preview theo nhan vat dang duoc tro toi (index hien tai). */
    public void refreshPreview(List<CharacterSlot> slots) {
        CharacterData data = slots.get(index).getData();
        preview = new Character(animManager, data.id);
        preview.playAnimation("idle", IDLE_FRAME_DURATION);
        preview.setFlipX(flipPreview);
        preview.setScale(PREVIEW_SCALE);
    }

    /** Canh giua preview theo tam cot (centerX) + offset rieng cua nguoi choi nay. */
    public void positionPreview(float centerX, float baselineY) {
        if (preview == null) return;
        float w = preview.getCurrentFrameWidth();
        preview.setPosition(centerX - w / 2f + offsetX, baselineY);
    }

    public void update(float delta) {
        if (preview != null) preview.update(delta);
        if (locked && !confirmed) {
            confirmTimer += delta;
            if (confirmTimer >= CONFIRM_DURATION) confirmed = true;
        }
    }

    public void draw(SpriteBatch batch) {
        if (preview != null) preview.draw(batch);
    }

    public int getIndex() { return index; }
    public Id getId() { return id; }
    public Color getColor() { return color; }
    public String getLabel() { return id.name(); } // "P1" / "P2"
}
