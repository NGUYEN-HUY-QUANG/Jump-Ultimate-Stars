package com.HUY.JumpUltimateStars.character.stats;

/**
 * Dữ liệu của 1 đòn đánh (đòn thường J1..J4, đòn trên không).
 *
 * Đây chỉ là dữ liệu tĩnh, KHÔNG chứa logic. Logic chạy đòn (đếm frame, nối combo,
 * gây dame) nằm ở Fighter. Mỗi nhân vật khai báo mảng Move[] trong class Stats của mình
 * (vd NarutoBaseStats), thứ tự trong mảng chính là thứ tự combo.
 */
public class Move {

    /** Tên action, đồng thời là tên thư mục ảnh và tiền tố file ảnh (vd "J1"). */
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

    /** true = trúng thì đối thủ bị KNOCKOUT; false = chỉ bị HIT (stun). */
    public final boolean knockdown;

    /** Energy hồi khi đòn này trúng. Mặc định 0. */
    public float energy;

    /** Đòn chỉ gây hit (không knockout). */
    public Move(String action, float frameDuration, int damage, int hitFrame, float recovery) {
        this(action, frameDuration, damage, hitFrame, recovery, false);
    }

    public Move(String action, float frameDuration, int damage, int hitFrame, float recovery,
                boolean knockdown) {
        this.action = action;
        this.frameDuration = frameDuration;
        this.damage = damage;
        this.hitFrame = hitFrame;
        this.recovery = recovery;
        this.knockdown = knockdown;
    }

    /** Đặt energy hồi khi trúng, dùng nối ngay sau constructor: new Move(...).energy(4f). */
    public Move energy(float e) {
        this.energy = e;
        return this;
    }
}
