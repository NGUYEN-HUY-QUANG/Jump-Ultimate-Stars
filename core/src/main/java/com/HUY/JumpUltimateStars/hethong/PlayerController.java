package com.HUY.JumpUltimateStars.hethong;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

/**
 * Đọc phím của 1 người chơi và chuyển thành "ý định" điều khiển
 * (hiện tại: hướng di chuyển, tấn công thường, nhảy, lướt, skill U, thủ, ultimate).
 *
 * Tách riêng khỏi Fighter để Fighter không cần biết phím cụ thể nào được dùng.
 * Muốn đổi phím, hoặc sau này thay bằng tay cầm / AI điều khiển, chỉ cần sửa ở đây
 * (hoặc tạo bản controller khác) mà không đụng tới logic của Fighter.
 *
 * Quy ước đặt tên: ảnh đòn đánh đặt theo "ô lệnh" (J1..J4 cho đòn thường, U cho skill U, I cho ultimate),
 * không theo phím vật lý. P2 bấm phím khác vẫn dùng đúng ảnh J1..J4, U, I, chỉ cần đổi
 * phím ở p1()/p2().
 *
 * Phân loại các hàm đọc phím:
 *  - getMoveDirection(), isDefendHeld(): đọc phím GIỮ (isKeyPressed), đúng liên tục khi còn giữ
 *  - isAttackPressed / isJumpPressed / isDashPressed / isSkillUPressed / isUltimatePressed:
 *    đọc phím BẤM (isKeyJustPressed), chỉ đúng 1 frame khi vừa nhấn xuống, giữ phím không tự lặp lại
 */
public class PlayerController {

    // Mã phím (Input.Keys) dùng để đi sang trái / sang phải
    private final int keyLeft;
    private final int keyRight;

    // Mã phím tấn công thường (đòn J1..J4)
    private final int keyAttack;

    // Mã phím nhảy (bấm lần nữa khi đang trên không để nhảy kép, nếu nhân vật cho phép)
    private final int keyJump;

    // Mã phím lướt (nút né: lướt 1 đoạn theo hướng mặt, không cần giữ phím hướng)
    private final int keyDash;

    // Mã phím skill U. Nhân vật không có skill U (stats.skillU == null) thì Fighter bỏ qua phím này
    private final int keySkillU;

    // Mã phím thủ (GIỮ phím để thủ, thả ra thì thôi)
    private final int keyDefend;

    // Mã phím ultimate (tốn 1 thanh energy)
    private final int keyUltimate;

    // Mã phím biến hình
    private final int keyTransformation;

    /**
     * @param keyLeft     mã phím đi sang trái (vd Keys.A)
     * @param keyRight    mã phím đi sang phải (vd Keys.D)
     * @param keyAttack   mã phím tấn công thường (vd Keys.J)
     * @param keyJump     mã phím nhảy (vd Keys.K)
     * @param keyDash     mã phím lướt (vd Keys.L)
     * @param keySkillU   mã phím skill U (vd Keys.U)
     * @param keyDefend   mã phím thủ (vd Keys.S)
     * @param keyUltimate      mã phím ultimate (vd Keys.I)
     * @param keyTransformation mã phím biến hình
     */
    public PlayerController(int keyLeft, int keyRight, int keyAttack, int keyJump,
                            int keyDash, int keySkillU, int keyDefend, int keyUltimate,
                            int keyTransformation) {
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
        this.keyAttack = keyAttack;
        this.keyJump = keyJump;
        this.keyDash = keyDash;
        this.keySkillU = keySkillU;
        this.keyDefend = keyDefend;
        this.keyUltimate = keyUltimate;
        this.keyTransformation = keyTransformation;
    }

    // Bộ phím mặc định của từng người chơi. Muốn đổi phím thì sửa ở 2 hàm này.

    /** P1: A/D = di chuyển, J = tấn công, K = nhảy, L = lướt, U = skill, S = thủ, I = ultimate, O = biến hình. */
    public static PlayerController p1() {
        return new PlayerController(Keys.A, Keys.D, Keys.J, Keys.K, Keys.L, Keys.U, Keys.S, Keys.I, Keys.O);
    }

    /** P2: mũi tên trái/phải = di chuyển, 1 = tấn công, 2 = nhảy, 3 = lướt, 4 = skill, xuống = thủ, 5 = ultimate, 6 = biến hình. */
    public static PlayerController p2() {
        return new PlayerController(Keys.LEFT, Keys.RIGHT, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4,
            Keys.DOWN, Keys.NUM_5, Keys.NUM_6);
    }

    /**
     * Trả về hướng di chuyển theo phím đang được GIỮ ở thời điểm gọi (gọi mỗi frame).
     *
     * @return -1 = sang trái, 0 = đứng yên, 1 = sang phải.
     *         Nếu giữ cả 2 phím cùng lúc thì hai hướng triệt tiêu nhau (kết quả 0).
     */
    public int getMoveDirection() {
        int dir = 0;
        if (Gdx.input.isKeyPressed(keyLeft)) dir--;
        if (Gdx.input.isKeyPressed(keyRight)) dir++;
        return dir;
    }

    /** true đúng 1 frame khi vừa bấm phím tấn công. Chỉ nên gọi 1 lần mỗi frame. */
    public boolean isAttackPressed() {
        return Gdx.input.isKeyJustPressed(keyAttack);
    }

    /** true đúng 1 frame khi vừa bấm phím nhảy. Chỉ nên gọi 1 lần mỗi frame. */
    public boolean isJumpPressed() {
        return Gdx.input.isKeyJustPressed(keyJump);
    }

    /**
     * true đúng 1 frame khi vừa bấm phím lướt.
     * Việc chặn lướt liên tục (đang lướt, đang cooldown) do Fighter xử lý. Chỉ nên gọi 1 lần mỗi frame.
     */
    public boolean isDashPressed() {
        return Gdx.input.isKeyJustPressed(keyDash);
    }

    /**
     * true đúng 1 frame khi vừa bấm phím skill U.
     * Việc chặn skill U (đang cooldown, không có skill) do Fighter xử lý. Chỉ nên gọi 1 lần mỗi frame.
     */
    public boolean isSkillUPressed() {
        return Gdx.input.isKeyJustPressed(keySkillU);
    }

    /**
     * true đúng 1 frame khi vừa bấm phím ultimate.
     * Việc kiểm tra đủ energy / có ultimate do Fighter xử lý. Chỉ nên gọi 1 lần mỗi frame.
     */
    public boolean isUltimatePressed() {
        return Gdx.input.isKeyJustPressed(keyUltimate);
    }

    /** true đúng 1 frame khi vừa bấm phím biến hình. */
    public boolean isTransformationPressed() {
        return Gdx.input.isKeyJustPressed(keyTransformation);
    }

    /** true liên tục khi còn GIỮ phím thủ (khác các phím bấm ở trên). */
    public boolean isDefendHeld() {
        return Gdx.input.isKeyPressed(keyDefend);
    }
}
