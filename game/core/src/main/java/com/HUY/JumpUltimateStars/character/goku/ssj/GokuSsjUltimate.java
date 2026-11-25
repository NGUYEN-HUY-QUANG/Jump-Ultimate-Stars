package com.HUY.JumpUltimateStars.character.goku.ssj;

import com.HUY.JumpUltimateStars.character.stats.Ultimate;
import com.HUY.JumpUltimateStars.effect.Effect;
import com.HUY.JumpUltimateStars.effect.Kamehameha;

public class GokuSsjUltimate extends Ultimate {

    private static final float FRAME_DURATION = 0.07f;
    private static final int SPAWN_FRAME = 7;
    private static final int HOLD_FRAME = 9;

    private static final float DARK_TIME = 1.0f;
    private static final int DARK_HOLD_FRAME = 3;

    // Chỉnh riêng Kamehameha thường của SSJ tại đây.
    private static final float KAME_OFFSET_X = 40f;
    private static final float KAME_OFFSET_Y = 60f;
    private static final float KAME_SCALE = 2.f;
    private static final int KAME_DAMAGE = 12;

    private int phase;
    private float time;
    private Effect kame;

    public GokuSsjUltimate() {
        super("I", FRAME_DURATION);
        darkTime = DARK_TIME;
    }

    @Override
    public boolean update(Context c, float delta) {
        time += delta;
        int steps = (int) (time / FRAME_DURATION);

        if (phase == 0 && c.dark && steps >= DARK_HOLD_FRAME) {
            time = DARK_HOLD_FRAME * FRAME_DURATION;
            c.frame = DARK_HOLD_FRAME;
            return false;
        }

        if (phase == 0) {
            c.frame = Math.min(steps, HOLD_FRAME);
            if (kame == null && c.frame >= SPAWN_FRAME) {
                kame = new Kamehameha(c.x, c.feetY, c.dir,
                    KAME_OFFSET_X, KAME_OFFSET_Y, KAME_SCALE, KAME_DAMAGE);
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
