package com.HUY.JumpUltimateStars.character.stats;

import com.HUY.JumpUltimateStars.effect.ProjectileSpec;

/**
 * Dữ liệu tĩnh của 1 skill (hiện dùng cho phím U). KHÔNG chứa logic: việc chạy skill
 * (đếm frame, gây dame, sinh effect, cooldown) nằm ở Fighter. Các field đều final nên
 * 1 Skill dùng chung an toàn giữa nhiều Fighter cùng nhân vật.
 *
 * Ảnh của nhân vật khi tung skill: characters/{charId}/{action}/{action}_{i}.png (vd "U").
 * Effect (nếu có) nằm ở thư mục riêng, khai báo trong ProjectileSpec.
 *
 * Có 2 kiểu, dùng constructor tương ứng:
 *  - Cận chiến nhiều hit (vd Goku): có nhiều hitFrames, nhân vật tự gây dame.
 *  - Bắn effect (vd Naruto ném phi tiêu): tới spawnFrame thì sinh effect, dame nằm ở effect,
 *    còn animation nhân vật không gây dame.
 *
 * Mọi chỉ số frame đều đếm từ 0, giống tên file ảnh (spawnFrame = 2 nghĩa là ảnh U_2.png),
 * và không được vượt quá số frame thật của animation, nếu không frame đó không bao giờ tới.
 */
public class Skill {

    /** Tên action, đồng thời là tên thư mục ảnh và tiền tố file ảnh (vd "U"). Phân biệt hoa thường. */
    public final String action;

    /**
     * Thời gian hiển thị mỗi frame của animation nhân vật (giây). Số càng lớn skill càng chậm.
     * Thời gian cả skill = số frame * frameDuration. Lưu ý: animation được cache theo
     * (charId, action), nên giá trị của lần load đầu tiên được dùng cho các lần sau.
     */
    public final float frameDuration;

    /** Thời gian nghỉ sau khi skill kết thúc, chưa dùng lại được (giây). Đếm từ lúc skill xong. */
    public final float cooldown;

    // ===================== CẬN CHIẾN =====================

    /** Cận chiến: sát thương MỖI hit. Bằng 0 với skill bắn effect (dame nằm ở ProjectileSpec). */
    public final int damage;

    /**
     * Cận chiến: các frame gây dame, xếp tăng dần, đếm từ 0.
     * Ví dụ {2, 4, 6, 8} = gây dame 4 lần, ở ảnh U_2, U_4, U_6, U_8. Mảng rỗng với skill bắn effect.
     */
    public final int[] hitFrames;

    // ===================== BẮN EFFECT =====================

    /** Loại effect/đạn sẽ sinh ra. null nếu skill không bắn gì (skill cận chiến thuần). */
    public final ProjectileSpec projectile;

    /** Bắn effect: frame (đếm từ 0) mà effect được sinh ra. Chỉ có tác dụng khi projectile != null. */
    public final int spawnFrame;

    /**
     * Skill cận chiến nhiều hit (vd Goku).
     *
     * @param action        tên action / thư mục ảnh (vd "U")
     * @param frameDuration giây mỗi frame
     * @param damage        sát thương mỗi hit
     * @param hitFrames     các frame gây dame, tăng dần, đếm từ 0
     * @param cooldown      thời gian nghỉ sau skill (giây)
     */
    public Skill(String action, float frameDuration, int damage, int[] hitFrames, float cooldown) {
        this.action = action; this.frameDuration = frameDuration; this.cooldown = cooldown;
        this.damage = damage; this.hitFrames = hitFrames;
        this.projectile = null; this.spawnFrame = 0;
    }

    /**
     * Skill bắn effect (vd Naruto ném phi tiêu). Nhân vật không gây dame, effect mới gây.
     *
     * @param action        tên action / thư mục ảnh (vd "U")
     * @param frameDuration giây mỗi frame
     * @param cooldown      thời gian nghỉ sau skill (giây)
     * @param projectile    dữ liệu effect sẽ sinh ra
     * @param spawnFrame    frame (đếm từ 0) mà effect được sinh ra
     */
    public Skill(String action, float frameDuration, float cooldown,
                 ProjectileSpec projectile, int spawnFrame) {
        this.action = action; this.frameDuration = frameDuration; this.cooldown = cooldown;
        this.damage = 0; this.hitFrames = new int[0];
        this.projectile = projectile; this.spawnFrame = spawnFrame;
    }
}
