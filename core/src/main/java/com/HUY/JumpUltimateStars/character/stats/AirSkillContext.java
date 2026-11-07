package com.HUY.JumpUltimateStars.character.stats;

import com.HUY.JumpUltimateStars.effect.Effect;
import com.badlogic.gdx.utils.Array;

/**
 * Trạng thái của 1 lần dùng skill U trên không. Fighter tạo 1 cái và dùng lại cho mọi lần dùng,
 * mỗi lần bắt đầu thì gọi reset(). AirSkill chỉ đọc / sửa các field ở đây, không tự giữ trạng thái.
 */
public class AirSkillContext {

    // ----- Vị trí & vận tốc: skill được sửa -----

    /** Tâm ngang của nhân vật. Sửa để di chuyển / dịch chuyển. */
    public float x;

    /** Độ cao so với mặt đất (<= 0 nghĩa là đã chạm đất). Sửa để bay lên / xuống. */
    public float airY;

    /** Vận tốc để skill tự dùng (Fighter không đụng tới). */
    public float velX, velY;

    // ----- Chỉ đọc: Fighter cập nhật mỗi frame -----

    /** Cao độ mặt đất trên màn hình. */
    public float groundY;

    /** Hướng mặt: -1 = trái, 1 = phải. */
    public int dir;

    /** Thời gian skill đã chạy (giây). */
    public float time;

    /** Frame animation hiện tại (đếm từ 0). */
    public int frame;

    /** true khi animation đã chạy hết. */
    public boolean animDone;

    // ----- Ghi chú của skill -----

    /** Cờ tự do cho skill, dùng để làm việc đúng 1 lần (vd "đã bắn", "đã dịch chuyển"). Reset về false lúc bắt đầu. */
    public boolean flag;

    /** Sát thương cận chiến skill báo ở frame này (Fighter đọc rồi xử lý). Đặt bằng hit(). */
    public int hitDamage;

    private final Array<Effect> effects;   // danh sách effect của Fighter

    public AirSkillContext(Array<Effect> effects) {
        this.effects = effects;
    }

    /** Fighter gọi lúc bắt đầu skill. */
    public void reset(float x, float airY, float groundY, int dir) {
        this.x = x;
        this.airY = airY;
        this.groundY = groundY;
        this.dir = dir;
        velX = 0f;
        velY = 0f;
        time = 0f;
        frame = 0;
        animDone = false;
        flag = false;
        hitDamage = 0;
    }

    /** Độ cao thật trên màn hình (dùng để đặt vị trí effect). */
    public float worldY() {
        return groundY + airY;
    }

    /** Sinh 1 effect (phi tiêu, sóng khí...). Effect tự bay, tự vẽ, tự gây dame. */
    public void spawn(Effect e) {
        effects.add(e);
    }

    /**
     * Báo "hitbox cận chiến đang bật ở frame này". Có thể gọi MỖI FRAME: Fighter tự kiểm tra va chạm
     * và chỉ tính dame 1 lần mỗi lần dùng skill (trúng rồi thì các lần gọi sau bị bỏ qua).
     */
    public void hit(int damage) {
        hitDamage = damage;
    }
}
