package com.HUY.JumpUltimateStars.character.goku.base;

import com.HUY.JumpUltimateStars.character.stats.Ultimate;
import com.HUY.JumpUltimateStars.effect.Effect;
import com.HUY.JumpUltimateStars.effect.Kamehameha;

/**
 * Ultimate Goku: chạy tới DARK_HOLD_FRAME thì đứng yên tới khi hết tối, rồi chạy tới SPAWN_FRAME
 * thì gọi Kamehameha, đứng yên ở HOLD_FRAME đợi kame chạy xong, rồi chạy nốt các frame còn lại.
 * Dame do effect Kamehameha gây, không phải chiêu này.
 * Ảnh: characters/goku_base/I/I_0.png ... (đếm từ 0)
 */
public class GokuUltimate extends Ultimate {

    // ===== Chỉnh số ở đây (frame đếm từ 0) =====
    private static final float FRAME_DURATION = 0.07f;
    private static final int SPAWN_FRAME = 5;    // tới frame này thì gọi kame
    private static final int HOLD_FRAME = 6;     // đứng yên ở frame này đợi kame (>= SPAWN_FRAME)

    private static final float DARK_TIME = 1.0f;     // giây map tối
    private static final int DARK_HOLD_FRAME = 3;    // đứng yên ở frame này tới khi hết tối

    private int phase;          // 0 = chạy tới HOLD_FRAME, 1 = đợi kame, 2 = chạy nốt
    private float time;
    private Effect kame;

    public GokuUltimate() {
        super("I", FRAME_DURATION);
        darkTime = DARK_TIME;
    }

    @Override
    public boolean update(Context c, float delta) {
        time += delta;
        int steps = (int) (time / FRAME_DURATION);

        // Đang tối: dừng ở DARK_HOLD_FRAME, hết tối thì chạy tiếp
        if (phase == 0 && c.dark && steps >= DARK_HOLD_FRAME) {
            time = DARK_HOLD_FRAME * FRAME_DURATION;
            c.frame = DARK_HOLD_FRAME;
            return false;
        }

        if (phase == 0) {
            c.frame = Math.min(steps, HOLD_FRAME);
            if (kame == null && c.frame >= SPAWN_FRAME) {
                kame = new Kamehameha(c.x, c.feetY, c.dir);
                c.effects.add(kame);
            }
            if (steps >= HOLD_FRAME) phase = 1;
        } else if (phase == 1) {
            c.frame = HOLD_FRAME;
            if (!kame.isActive()) { phase = 2; time = 0f; }
        } else {
            int f = HOLD_FRAME + 1 + steps;
            c.frame = Math.min(f, c.frameCount - 1);
            return f >= c.frameCount;
        }
        return false;
    }

    @Override
    public void cancel() {
        if (kame != null) kame.cancel();
    }
}
