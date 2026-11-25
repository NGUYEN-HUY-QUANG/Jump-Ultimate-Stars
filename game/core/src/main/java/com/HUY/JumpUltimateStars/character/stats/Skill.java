package com.HUY.JumpUltimateStars.character.stats;

import com.HUY.JumpUltimateStars.effect.Effect;

/**
 * Dữ liệu của 1 skill (hiện là phím U). KHÔNG chứa logic: việc chạy skill nằm ở Fighter,
 * số liệu của effect (dame, tốc độ, knockout, energy...) nằm trong class effect.
 * Ảnh nhân vật khi tung skill: characters/{charId}/{action}/{action}_{i}.png (vd "U").
 * Mọi chỉ số frame đều đếm từ 0, giống tên file ảnh.
 *
 * 2 kiểu, dùng constructor tương ứng:
 *  - Cận chiến nhiều hit (Goku): nhân vật tự gây dame ở các hitFrames.
 *  - Gọi effect (Naruto): tới spawnFrame thì sinh effect, effect mới là thứ gây dame.
 */
public class Skill {

    /** Tên thư mục ảnh + tiền tố file (vd "U"). Phân biệt hoa thường. */
    public final String action;

    /** Giây mỗi frame của animation nhân vật. */
    public final float frameDuration;

    /** Nghỉ sau khi skill xong, chưa dùng lại được (giây). */
    public final float cooldown;

    // ----- Cận chiến -----
    public final int damage;          // dame mỗi hit
    public final int[] hitFrames;     // các frame gây dame, tăng dần

    /** true = chỉ hit CUỐI trong hitFrames knockout, các hit trước chỉ là hit thường. */
    public final boolean knockdownOnLastHit;

    // ----- Lướt trước khi đánh -----
    public float rushDistance;   // 0 = không lướt
    public float rushSpeed;      // pixel/giây

    // ----- Gọi effect -----
    public final Effect.Factory effect;   // null = skill không có effect
    public final int spawnFrame;          // frame sinh effect

    /** Energy hồi MỖI HIT cận chiến trúng. Mặc định 0. (Skill gọi effect: ghi energy trong class effect.) */
    public float energy;

    /** Skill cận chiến nhiều hit, không knockout. */
    public Skill(String action, float frameDuration, int damage, int[] hitFrames, float cooldown) {
        this(action, frameDuration, damage, hitFrames, cooldown, false);
    }

    /** Skill cận chiến nhiều hit, có thể knockout ở hit cuối. */
    public Skill(String action, float frameDuration, int damage, int[] hitFrames, float cooldown,
                 boolean knockdownOnLastHit) {
        this.action = action;
        this.frameDuration = frameDuration;
        this.cooldown = cooldown;
        this.damage = damage;
        this.hitFrames = hitFrames;
        this.knockdownOnLastHit = knockdownOnLastHit;
        this.effect = null;
        this.spawnFrame = 0;
    }

    /** Skill gọi effect. */
    public Skill(String action, float frameDuration, float cooldown,
                 int spawnFrame, Effect.Factory effect) {
        this.action = action;
        this.frameDuration = frameDuration;
        this.cooldown = cooldown;
        this.damage = 0;
        this.hitFrames = new int[0];
        this.knockdownOnLastHit = false;
        this.effect = effect;
        this.spawnFrame = spawnFrame;
    }

    /** Đặt energy hồi mỗi hit trúng: new Skill(...).energy(3f). */
    public Skill energy(float e) {
        this.energy = e;
        return this;
    }

    /** Lướt tới trước tối đa distance px (tốc độ speed px/s) rồi mới tung skill. */
    public Skill rush(float distance, float speed) {
        this.rushDistance = distance;
        this.rushSpeed = speed;
        return this;
    }
}
