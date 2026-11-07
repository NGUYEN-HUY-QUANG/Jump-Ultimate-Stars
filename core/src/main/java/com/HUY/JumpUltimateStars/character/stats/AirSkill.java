package com.HUY.JumpUltimateStars.character.stats;

/**
 * Skill U khi bấm trên không. Mỗi LOẠI skill là 1 class con kế thừa class này
 * (vd DiveAirSkill = lao chéo xuống, sau này TeleportAirSkill, ShootAirSkill...).
 * Nhân vật nào dùng loại nào thì khai báo trong class Stats của mình:
 *     airSkillU = new DiveAirSkill("U_AIR", 0.06f, 0.3f, 10, 60f, 1500f);
 *
 * Class này là dữ liệu + cách di chuyển, KHÔNG giữ trạng thái của lần dùng (vì 1 instance
 * dùng chung cho nhiều Fighter). Mọi thứ thay đổi trong lúc dùng (vị trí, vận tốc, đã bắn chưa...)
 * nằm ở AirSkillContext do Fighter truyền vào.
 *
 * Fighter lo phần chung: chạy animation, chỉ cho dùng 1 lần mỗi lượt nhảy, cooldown,
 * xong thì chuyển sang LAND (nếu đang ở đất) hoặc AIR (nếu còn ở trên không).
 * Trong lúc skill chạy, Fighter KHÔNG áp trọng lực: skill tự quyết định bay, đứng yên hay dịch chuyển.
 *
 * Animation lấy từ characters/{charId}/{action}/{action}_{i}.png (vd "U_AIR"), chạy kiểu NORMAL.
 */
public abstract class AirSkill {

    /** Tên action = tên thư mục ảnh = tiền tố file ảnh (vd "U_AIR"). */
    public final String action;

    /** Giây mỗi frame. */
    public final float frameDuration;

    /** Thời gian chờ dùng lại skill U (giây), đếm từ lúc skill kết thúc. Dùng chung với skill U dưới đất. */
    public final float cooldown;

    protected AirSkill(String action, float frameDuration, float cooldown) {
        this.action = action;
        this.frameDuration = frameDuration;
        this.cooldown = cooldown;
    }

    /** Gọi 1 lần lúc bắt đầu skill. Dùng để đặt vận tốc ban đầu... (mặc định không làm gì). */
    public void start(AirSkillContext c) {
    }

    /**
     * Gọi mỗi frame trong lúc skill chạy. Sửa c.x / c.airY để di chuyển nhân vật,
     * c.spawn(effect) để bắn effect, c.hit(dame) để gây dame cận chiến.
     *
     * @return true khi skill kết thúc. Lúc đó nếu c.airY <= 0 thì nhân vật chạm đất (sang LAND),
     *         còn không thì quay lại trạng thái trên không và rơi tiếp.
     */
    public abstract boolean update(AirSkillContext c, float delta);
}
