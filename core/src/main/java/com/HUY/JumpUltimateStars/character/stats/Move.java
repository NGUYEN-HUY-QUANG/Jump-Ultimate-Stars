package com.HUY.JumpUltimateStars.character.stats;

/**
 * Dữ liệu của 1 đòn đánh (đòn thường J1..J4, sau này có thể dùng cho cả skill).
 *
 * Đây chỉ là dữ liệu tĩnh, KHÔNG chứa logic. Logic chạy đòn (đếm frame, nối combo,
 * gây dame) nằm ở Fighter. Mỗi nhân vật khai báo mảng Move[] trong class Stats của mình
 * (vd NarutoBaseStats), thứ tự trong mảng chính là thứ tự combo: phần tử đầu là đòn 1,
 * phần tử cuối là đòn kết (sau đó bị khóa input trong khoảng recovery).
 *
 * Các field đều final nên đối tượng này dùng chung an toàn giữa nhiều Fighter cùng nhân vật.
 * Muốn chỉnh dame / tốc độ / frame gây dame thì sửa số khi tạo Move trong class Stats.
 */
public class Move {

    /**
     * Tên action, đồng thời là tên thư mục ảnh và tiền tố file ảnh.
     * Ví dụ "J1" -> characters/{charId}/J1/J1_0.png, J1_1.png, ...
     * Phân biệt hoa thường. Frame phải đánh số liên tục từ 0.
     */
    public final String action;

    /**
     * Thời gian hiển thị mỗi frame (giây).
     * Số càng lớn thì đòn càng chậm. Thời gian cả đòn = số frame * frameDuration.
     * Lưu ý: animation được cache theo (charId, action), nên giá trị của lần load đầu tiên
     * sẽ được dùng cho các lần sau.
     */
    public final float frameDuration;

    /** Sát thương của đòn này khi trúng đối thủ. */
    public final int damage;

    /**
     * Thứ tự frame gây dame, đếm từ 0 (không phải đếm theo thời gian).
     * Ví dụ hitFrame = 2 nghĩa là khi animation chạy tới ảnh J1_2.png thì tính dame.
     * Mỗi đòn chỉ gây dame 1 lần tại frame này.
     */
    public final int hitFrame;

    /**
     * Thời gian nghỉ sau khi animation đòn kết thúc (giây).
     * - Với đòn giữa chuỗi: là cửa sổ để bấm tiếp nối đòn kế, hết giờ thì combo reset về đòn đầu.
     * - Với đòn cuối chuỗi: là khoảng khóa không cho đánh, nhân vật vẫn đi lại được.
     */
    public final float recovery;

    /**
     * @param action        tên thư mục + tiền tố ảnh của đòn (vd "J1")
     * @param frameDuration giây mỗi frame
     * @param damage        sát thương
     * @param hitFrame      frame gây dame (đếm từ 0)
     * @param recovery      thời gian nghỉ sau đòn (giây)
     */
    public Move(String action, float frameDuration, int damage, int hitFrame, float recovery) {
        this.action = action;
        this.frameDuration = frameDuration;
        this.damage = damage;
        this.hitFrame = hitFrame;
        this.recovery = recovery;
    }
}
