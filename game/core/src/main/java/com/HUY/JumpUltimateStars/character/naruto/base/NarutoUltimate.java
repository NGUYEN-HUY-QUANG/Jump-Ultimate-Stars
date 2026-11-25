package com.HUY.JumpUltimateStars.character.naruto.base;

import com.HUY.JumpUltimateStars.character.stats.Ultimate;

/**
 * Ultimate Naruto: CHARGE (đứng yên) -> RUSH (lao tới, không chạm ai thì mất chiêu)
 * -> HIT (đứng yên gây dame liên tục, đòn cuối knockdown) -> END (hết chiêu, không dame).
 * Map tối đúng trong lúc CHARGE (vận chiêu), bắt đầu RUSH thì sáng lại.
 * Ảnh: characters/naruto_base/I/I_0.png ... (đếm từ 0)
 */
public class NarutoUltimate extends Ultimate {

    // ===== Chỉnh số ở đây (frame đếm từ 0, gồm cả 2 đầu) =====
    private static final float FRAME_DURATION = 0.08f;
    private static final float HIT_FRAME_DURATION = 0.09f;
    private static final int CHARGE_FROM = 0,  CHARGE_TO = 13;   // vận chiêu
    private static final int RUSH_FROM   = 14, RUSH_TO   = 16;   // lao tới, hết frame thì giữ frame cuối
    private static final int HIT_FROM    = 17, HIT_TO    = 33;   // gây dame
    private static final int END_FROM    = 34;                   // hết chiêu, chạy tới hết ảnh

    private static final float RUSH_SPEED = 1000f;      // px/s
    private static final float RUSH_DISTANCE = 600f;    // lao tối đa bao xa mà không trúng thì mất chiêu
    private static final float TICK_INTERVAL = 0.1f;    // giây giữa 2 lần dame
    private static final int TICK_DAMAGE = 10;          // dame mỗi lần tick
    private static final int LAST_DAMAGE = 40;          // đòn cuối, knockdown

    private static final int CHARGE = 0, RUSH = 1, HIT = 2, END = 3;

    private int phase = CHARGE;
    private float time;        // giây trong đoạn hiện tại
    private float traveled;
    private float tick;

    public NarutoUltimate() {
        super("I", FRAME_DURATION);
        darkTime = (CHARGE_TO - CHARGE_FROM + 1) * FRAME_DURATION;   // tối đúng lúc vận chiêu
    }

    private void next() { phase++; time = 0f; }

    @Override
    public boolean update(Context c, float delta) {
        time += delta;
        int steps = (int) (time / (phase == HIT ? HIT_FRAME_DURATION : FRAME_DURATION));   // đã qua bao nhiêu frame trong đoạn này

        switch (phase) {
            case CHARGE:
                c.frame = Math.min(CHARGE_FROM + steps, CHARGE_TO);
                if (CHARGE_FROM + steps > CHARGE_TO) next();
                break;
            case RUSH:
                c.frame = Math.min(RUSH_FROM + steps, RUSH_TO);
                float move = RUSH_SPEED * delta;
                c.x += c.dir * move;
                traveled += move;
                if (c.touching) next();
                else if (traveled >= RUSH_DISTANCE) return true;   // hụt -> mất chiêu
                break;
            case HIT:
                c.frame = Math.min(HIT_FROM + steps, HIT_TO);
                tick -= delta;
                if (HIT_FROM + steps > HIT_TO) {
                    c.damage = LAST_DAMAGE;
                    c.knockdown = true;
                    next();
                } else if (tick <= 0f) {
                    tick = TICK_INTERVAL;
                    c.damage = TICK_DAMAGE;
                }
                break;
            default:
                int f = END_FROM + steps;
                c.frame = Math.min(f, c.frameCount - 1);
                return f >= c.frameCount;
        }
        return false;
    }
}
