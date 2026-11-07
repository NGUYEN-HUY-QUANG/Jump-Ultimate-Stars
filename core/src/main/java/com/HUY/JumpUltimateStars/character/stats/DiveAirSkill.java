package com.HUY.JumpUltimateStars.character.stats;

import com.badlogic.gdx.math.MathUtils;

/**
 * Skill U trên không kiểu LAO XUỐNG: bay thẳng từ chỗ đang đứng xuống đất theo góc cho trước,
 * hướng theo mặt nhân vật. Chạm đất thì gây dame cận chiến và kết thúc.
 *   - Goku / Naruto: angleDeg = 60 (chéo xuống)
 *   - Đánh thẳng xuống: angleDeg = 90
 */
public class DiveAirSkill extends AirSkill {

    private final int damage;
    private final float angleDeg;   // 0 = ngang, 60 = chéo xuống, 90 = thẳng xuống
    private final float speed;      // px/s theo đường chéo

    /**
     * @param action        tên thư mục + tiền tố ảnh (vd "U_AIR")
     * @param frameDuration giây mỗi frame
     * @param cooldown      cooldown sau khi kết thúc (giây)
     * @param damage        sát thương khi chạm đất
     * @param angleDeg      góc lao so với mặt đất (độ)
     * @param speed         tốc độ lao (px/s)
     */
    public DiveAirSkill(String action, float frameDuration, float cooldown,
                        int damage, float angleDeg, float speed) {
        super(action, frameDuration, cooldown);
        this.damage = damage;
        this.angleDeg = angleDeg;
        this.speed = speed;
    }

    @Override
    public void start(AirSkillContext c) {
        c.velX = c.dir * MathUtils.cosDeg(angleDeg) * speed;
        c.velY = -MathUtils.sinDeg(angleDeg) * speed;   // âm = đi xuống
    }

    @Override
    public boolean update(AirSkillContext c, float delta) {
        c.x += c.velX * delta;
        c.airY += c.velY * delta;

        if (c.airY <= 0f) {
            c.hit(damage);
            return true;   // chạm đất -> kết thúc
        }
        return false;
    }
}
