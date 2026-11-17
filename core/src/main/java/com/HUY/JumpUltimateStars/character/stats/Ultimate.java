package com.HUY.JumpUltimateStars.character.stats;

import com.HUY.JumpUltimateStars.effect.Effect;
import com.badlogic.gdx.utils.Array;

/**
 * Khung chung cho chiêu ultimate (phím I / số 5). Mỗi lần dùng tạo 1 instance MỚI (qua Factory)
 * nên class con thoải mái giữ trạng thái (đang ở đoạn nào, đã chạy bao lâu...).
 * Fighter đưa dữ liệu vào Context, chiêu ghi kết quả (frame, dame) ra Context.
 */
public abstract class Ultimate {

    public interface Factory { Ultimate create(); }

    public static class Context {
        // ---- Fighter đưa vào ----
        public float x;               // tâm ngang nhân vật (chiêu có thể sửa để di chuyển)
        public float feetY;
        public int dir;               // -1 trái, 1 phải
        public boolean touching;      // sprite hiện tại đang chạm đối thủ (đối thủ chưa bị miễn dame)
        public int frameCount;        // tổng số ảnh của chiêu
        public Array<Effect> effects; // để chiêu gọi effect ra
        // ---- Chiêu ghi ra ----
        public int frame;             // frame cần hiển thị
        public int damage;            // > 0: gây dame lần này
        public boolean knockdown;
    }

    public final String action;
    public final float frameDuration;

    protected Ultimate(String action, float frameDuration) {
        this.action = action;
        this.frameDuration = frameDuration;
    }

    /** Gọi mỗi frame. Trả true khi chiêu xong. */
    public abstract boolean update(Context c, float delta);

    /** Nhân vật bị đánh trúng giữa chừng. Chiêu nào có effect thì hủy ở đây. */
    public void cancel() {}
}
