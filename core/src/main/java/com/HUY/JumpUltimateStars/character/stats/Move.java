package com.HUY.JumpUltimateStars.character.stats;

/**
 * Dữ liệu của 1 đòn đánh (đòn thường J1..J4, đòn trên không).
 *
 * Đây chỉ là dữ liệu tĩnh, KHÔNG chứa logic. Logic chạy đòn (đếm frame, nối combo,
 * gây dame) nằm ở Fighter. Mỗi nhân vật khai báo mảng Move[] trong class Stats của mình
 * (vd NarutoBaseStats), thứ tự trong mảng chính là thứ tự combo: phần tử đầu là đòn 1,
 * phần tử cuối là đòn kết (sau đó bị khóa input trong khoảng recovery).
 *
 * Các field đều final nên đối tượng này dùng chung an toàn giữa nhiều Fighter cùng nhân vật.
 */
public class Move {

    /**
     * Tên action, đồng thời là tên thư mục ảnh và tiền tố file ảnh.
     * Ví dụ "J1" -> characters/{charId}/J1/J1_0.png, J1_1.png, ...
     */
    public final String action;

    /** Thời gian hiển thị mỗi frame (giây). Thời gian cả đòn = số frame * frameDuration. */
    public final float frameDuration;

    /** Sát thương của đòn này khi trúng đối thủ. */
    public final int damage;

    /** Thứ tự frame gây dame, đếm từ 0. Mỗi đòn chỉ gây dame 1 lần tại frame này. */
    public final int hitFrame;

    /**
     * Thời gian nghỉ sau khi animation đòn kết thúc (giây).
     * - Đòn giữa chuỗi: cửa sổ để bấm nối đòn kế.
     * - Đòn cuối chuỗi: khoảng khóa không cho đánh.
     */
    public final float recovery;

    /** true = trúng thì đối thủ bị KNOCKOUT (ngã, nằm, đứng dậy); false = chỉ bị HIT (stun). */
    public final boolean knockdown;

    /** Đòn chỉ gây hit (không knockout). */
    public Move(String action, float frameDuration, int damage, int hitFrame, float recovery) {
        this(action, frameDuration, damage, hitFrame, recovery, false);
    }

    /**
     * @param action        tên thư mục + tiền tố ảnh của đòn (vd "J1")
     * @param frameDuration giây mỗi frame
     * @param damage        sát thương
     * @param hitFrame      frame gây dame (đếm từ 0)
     * @param recovery      thời gian nghỉ sau đòn (giây)
     * @param knockdown     true = đòn này knockout đối thủ
     */
    public Move(String action, float frameDuration, int damage, int hitFrame, float recovery,
                boolean knockdown) {
        this.action = action;
        this.frameDuration = frameDuration;
        this.damage = damage;
        this.hitFrame = hitFrame;
        this.recovery = recovery;
        this.knockdown = knockdown;
    }
}
