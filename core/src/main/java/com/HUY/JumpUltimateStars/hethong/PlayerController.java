package com.HUY.JumpUltimateStars.hethong;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

/**
 * Đọc phím của 1 người chơi và chuyển thành "ý định" điều khiển
 * (hiện tại: hướng di chuyển, tấn công thường, nhảy, lướt, skill U, thủ).
 *
 * Tách riêng khỏi Fighter để Fighter không cần biết phím cụ thể nào được dùng.
 * Muốn đổi phím, hoặc sau này thay bằng tay cầm / AI điều khiển, chỉ cần sửa ở đây
 * (hoặc tạo bản controller khác) mà không đụng tới logic của Fighter.
 *
 * Quy ước đặt tên: ảnh đòn đánh đặt theo "ô lệnh" (J1..J4 cho đòn thường, U cho skill U),
 * không theo phím vật lý. P2 bấm phím khác vẫn dùng đúng ảnh J1..J4 và U, chỉ cần đổi
 * phím ở p1()/p2().
 *
 * Sau này thêm skill khác (I, O...) thì thêm keySkill... và hàm isSkill...Pressed() tương ứng,
 * rồi thêm tham số vào constructor và p1()/p2().
 *
 * Phân loại các hàm đọc phím:
 *  - getMoveDirection(), isDefendHeld(): đọc phím GIỮ (isKeyPressed), đúng liên tục khi còn giữ
 *  - isAttackPressed / isJumpPressed / isDashPressed / isSkillUPressed: đọc phím BẤM
 *    (isKeyJustPressed), chỉ đúng 1 frame khi vừa nhấn xuống, giữ phím không tự lặp lại
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

    /**
     * @param keyLeft   mã phím đi sang trái (vd Keys.A)
     * @param keyRight  mã phím đi sang phải (vd Keys.D)
     * @param keyAttack mã phím tấn công thường (vd Keys.J)
     * @param keyJump   mã phím nhảy (vd Keys.K)
     * @param keyDash   mã phím lướt (vd Keys.L)
     * @param keySkillU mã phím skill U (vd Keys.U)
     * @param keyDefend mã phím thủ (vd Keys.S)
     */
    public PlayerController(int keyLeft, int keyRight, int keyAttack, int keyJump,
                            int keyDash, int keySkillU, int keyDefend) {
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
        this.keyAttack = keyAttack;
        this.keyJump = keyJump;
        this.keyDash = keyDash;
        this.keySkillU = keySkillU;
        this.keyDefend = keyDefend;
    }

    // Bộ phím mặc định của từng người chơi. Muốn đổi phím thì sửa ở 2 hàm này.

    /** P1: A = trái, D = phải, J = tấn công, K = nhảy, L = lướt, U = skill U, S = thủ (giữ). */
    public static PlayerController p1() {
        return new PlayerController(Keys.A, Keys.D, Keys.J, Keys.K, Keys.L, Keys.U, Keys.S);
    }

    /** P2: mũi tên trái/phải = di chuyển, số 1 = tấn công, số 2 = nhảy, số 3 = lướt, số 4 = skill U, mũi tên xuống = thủ (giữ). */
    public static PlayerController p2() {
        return new PlayerController(Keys.LEFT, Keys.RIGHT, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4, Keys.DOWN);
    }

    /**
     * Trả về hướng di chuyển theo phím đang được GIỮ ở thời điểm gọi (gọi mỗi frame).
     *
     * @return -1 = sang trái, 0 = đứng yên, 1 = sang phải.
     *         Nếu giữ cả 2 phím cùng lúc thì hai hướng triệt tiêu nhau (kết quả 0).
     */
    public int getMoveDirection() {
        int dir = 0;
        // isKeyPressed = đang giữ phím (khác isKeyJustPressed chỉ đúng ở frame vừa bấm),
        // nên nhân vật chạy liên tục khi còn giữ phím
        if (Gdx.input.isKeyPressed(keyLeft)) dir--;
        if (Gdx.input.isKeyPressed(keyRight)) dir++;
        return dir;
    }

    /**
     * true đúng 1 frame khi vừa bấm phím tấn công (giữ phím không tính),
     * để giữ J không tự combo liên tục.
     * Chỉ nên gọi 1 lần mỗi frame (Fighter.update đọc 1 lần rồi truyền xuống các hàm con).
     */
    public boolean isAttackPressed() {
        return Gdx.input.isKeyJustPressed(keyAttack);
    }

    /**
     * true đúng 1 frame khi vừa bấm phím nhảy (giữ phím không tính),
     * để giữ K không tự nhảy liên tục và mỗi lần bấm chỉ tốn 1 lượt nhảy.
     * Chỉ nên gọi 1 lần mỗi frame.
     */
    public boolean isJumpPressed() {
        return Gdx.input.isKeyJustPressed(keyJump);
    }

    /**
     * true đúng 1 frame khi vừa bấm phím lướt (giữ phím không tính).
     * Việc chặn lướt liên tục (đang lướt, đang cooldown) do Fighter xử lý, không phải ở đây.
     * Chỉ nên gọi 1 lần mỗi frame.
     */
    public boolean isDashPressed() {
        return Gdx.input.isKeyJustPressed(keyDash);
    }

    /**
     * true đúng 1 frame khi vừa bấm phím skill U (giữ phím không tính).
     * Việc chặn skill U (đang dùng skill, đang cooldown, nhân vật không có skill) do Fighter xử lý,
     * không phải ở đây.
     * Chỉ nên gọi 1 lần mỗi frame.
     */
    public boolean isSkillUPressed() {
        return Gdx.input.isKeyJustPressed(keySkillU);
    }

    /**
     * true liên tục khi còn GIỮ phím thủ (khác các phím bấm ở trên).
     * Fighter dùng để vào trạng thái thủ khi bấm xuống và thoát khi thả phím.
     */
    public boolean isDefendHeld() {
        return Gdx.input.isKeyPressed(keyDefend);
    }
}
