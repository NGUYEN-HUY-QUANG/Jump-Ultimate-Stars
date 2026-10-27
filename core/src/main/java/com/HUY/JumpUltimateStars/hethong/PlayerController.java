package com.HUY.JumpUltimateStars.hethong;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

/**
 * Đọc phím của 1 người chơi và chuyển thành "ý định" điều khiển
 * (hiện tại: hướng di chuyển và bấm tấn công).
 *
 * Tách riêng khỏi Fighter để Fighter không cần biết phím cụ thể nào được dùng.
 * Muốn đổi phím, hoặc sau này thay bằng tay cầm / AI điều khiển, chỉ cần sửa ở đây
 * (hoặc tạo bản controller khác) mà không đụng tới logic của Fighter.
 *
 * Quy ước đặt tên: ảnh đòn đánh đặt theo "ô lệnh" (J1..J4 cho đòn thường), không theo
 * phím vật lý. P2 bấm phím khác vẫn dùng đúng ảnh J1..J4, chỉ cần đổi phím ở p1()/p2().
 *
 * Sau này thêm nhảy, skill... thì thêm keyJump, keySkill... và các hàm tương ứng vào class này.
 */
public class PlayerController {

    // Mã phím (Input.Keys) dùng để đi sang trái / sang phải
    private final int keyLeft;
    private final int keyRight;

    // Mã phím tấn công thường (đòn J1..J4)
    private final int keyAttack;

    /**
     * @param keyLeft   mã phím đi sang trái (vd Keys.A)
     * @param keyRight  mã phím đi sang phải (vd Keys.D)
     * @param keyAttack mã phím tấn công thường (vd Keys.J)
     */
    public PlayerController(int keyLeft, int keyRight, int keyAttack) {
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
        this.keyAttack = keyAttack;
    }

    // Bộ phím mặc định của từng người chơi. Muốn đổi phím thì sửa ở 2 hàm này.

    /** P1: A = trái, D = phải, J = tấn công. */
    public static PlayerController p1() { return new PlayerController(Keys.A, Keys.D, Keys.J); }

    /** P2: mũi tên trái/phải = di chuyển, phím số 1 (hàng trên) = tấn công. */
    public static PlayerController p2() { return new PlayerController(Keys.LEFT, Keys.RIGHT, Keys.NUM_1); }

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
}
