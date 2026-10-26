package com.HUY.JumpUltimateStars.hethong;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

/**
 * Đọc phím của 1 người chơi và chuyển thành "ý định" điều khiển (hiện tại: hướng di chuyển).
 *
 * Tách riêng khỏi Fighter để Fighter không cần biết phím cụ thể nào được dùng.
 * Muốn đổi phím, hoặc sau này thay bằng tay cầm / AI điều khiển, chỉ cần sửa ở đây
 * (hoặc tạo bản controller khác) mà không đụng tới logic của Fighter.
 *
 * Sau này thêm nhảy, đánh... thì thêm keyJump, keyPunch... và các hàm tương ứng vào class này.
 */
public class PlayerController {

    // Mã phím (Input.Keys) dùng để đi sang trái / sang phải
    private final int keyLeft;
    private final int keyRight;

    /**
     * @param keyLeft  mã phím đi sang trái (vd Keys.A)
     * @param keyRight mã phím đi sang phải (vd Keys.D)
     */
    public PlayerController(int keyLeft, int keyRight) {
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
    }

    // Bộ phím mặc định của từng người chơi. Muốn đổi phím thì sửa ở 2 hàm này.

    /** P1: A = trái, D = phải. */
    public static PlayerController p1() { return new PlayerController(Keys.A, Keys.D); }

    /** P2: phím mũi tên trái/phải. */
    public static PlayerController p2() { return new PlayerController(Keys.LEFT, Keys.RIGHT); }

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
}
