package com.HUY.JumpUltimateStars.character.stats;

import com.badlogic.gdx.math.MathUtils;

/**
 * Skill U trên không kiểu LAO XUỐNG: bay thẳng từ chỗ đang đứng xuống đất theo góc cho trước,
 * hướng theo mặt nhân vật. Hitbox bật suốt lúc lao (ngay từ frame 0), trúng 1 lần là đủ dame,
 * trúng rồi vẫn tiếp tục lao tới khi chạm đất mới kết thúc.
 *   - Goku / Naruto: angleDeg = 60 (chéo xuống)
 *   - Đánh thẳng xuống: angleDeg = 90
 */
public class DiveAirSkill extends AirSkill {

    private final int damage;
    private final float angleDeg;   // 0 = ngang, 60 = chéo xuống, 90 = thẳng xuống
    private final float speed;      // px/s theo đường chéo
    private final boolean knockdown;   // trúng thì knockout hay chỉ hit

    /** Lao xuống, chỉ hit (không knockout). */
    public DiveAirSkill(String action, float frameDuration, float cooldown,
                        int damage, float angleDeg, float speed) {
        this(action, frameDuration, cooldown, damage, angleDeg, speed, false);
    }

    /**
     * @param action        tên thư mục + tiền tố ảnh (vd "U_AIR")
     * @param frameDuration giây mỗi frame
     * @param cooldown      cooldown sau khi kết thúc (giây)
     * @param damage        sát thương
     * @param angleDeg      góc lao so với mặt đất (độ)
     * @param speed         tốc độ lao (px/s)
     * @param knockdown     true = trúng thì đối thủ bị knockout
     */
    public DiveAirSkill(String action, float frameDuration, float cooldown,
                        int damage, float angleDeg, float speed, boolean knockdown) {
        super(action, frameDuration, cooldown);
        this.damage = damage;
        this.angleDeg = angleDeg;
        this.speed = speed;
        this.knockdown = knockdown;
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

        c.hit(damage, knockdown);   // báo hitbox đang bật mỗi frame, Fighter lo chuyện chỉ tính dame 1 lần

        return c.airY <= 0f;   // chạm đất -> kết thúc
    }
}
