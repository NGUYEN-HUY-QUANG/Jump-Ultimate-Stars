package com.HUY.JumpUltimateStars.effect;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * 1 lần bắn skill = 1 loạt đạn (vd 3 phi tiêu xếp cột dọc, hoặc 1 chùm năng lượng).
 * Tất cả các viên trong loạt cùng trỏ vào 1 Volley, nên luật trúng đòn tính CHUNG cho cả loạt:
 *  - Tổng số lần gây dame (maxHits) đếm chung, không đếm riêng từng viên.
 *  - Khoảng nghỉ giữa 2 lần trúng (hitInterval) tính riêng cho từng đối thủ, nhưng chung cho các viên.
 *    Nhờ vậy 3 viên cùng chạm 1 đối thủ trong cùng 1 frame thì chỉ viên đầu tiên được tính dame.
 *  - Hết lượt trúng (done = true) thì cả loạt biến mất cùng lúc (xem Projectile.isActive()).
 *
 * Class này chỉ giữ dữ liệu + luật, KHÔNG biết gì về hitbox, máu hay việc vẽ.
 * Package-private: chỉ Projectile và EffectManager dùng, bên ngoài đi qua Projectile.tryHit().
 */
public class Volley {

    // Fighter đã bắn loạt này, dùng để không cho trúng chính mình (và để GameScreen biết chủ đạn)
    final Object owner;

    // Số lần gây dame tối đa của cả loạt (0 = không giới hạn). Lấy từ ProjectileSpec
    private final int maxHits;

    // Khoảng nghỉ giữa 2 lần dame lên cùng 1 đối thủ (giây). Lấy từ ProjectileSpec
    private final float hitInterval;

    // Số lần đã gây dame cho tới giờ (cộng dồn từ mọi viên trong loạt, mọi đối thủ)
    private int hits;

    // true khi đã đủ maxHits -> cả loạt biến mất
    private boolean done;

    // Thời điểm sớm nhất mà mỗi đối thủ có thể bị trúng lại (theo đồng hồ của EffectManager).
    // Chưa có trong map nghĩa là đối thủ đó chưa bị trúng lần nào
    private final ObjectMap<Object, Float> nextHitTime = new ObjectMap<>();

    /** Lấy luật trúng đòn (maxHits, hitInterval) từ spec của loại effect. */
    Volley(Object owner, ProjectileSpec spec) {
        this.owner = owner;
        this.maxHits = spec.maxHits;
        this.hitInterval = spec.hitInterval;
    }

    /** Cả loạt đã hết lượt trúng chưa (true = nên biến mất). */
    boolean isDone() { return done; }

    /**
     * Lần chạm này có được tính dame không?
     * Không nếu: loạt đã hết lượt, đối thủ là chính chủ đạn, hoặc đối thủ này chưa hết khoảng nghỉ.
     *
     * @param target đối thủ đang chạm
     * @param now    đồng hồ hiện tại (EffectManager.getTime())
     */
    boolean canHit(Object target, float now) {
        if (done || target == owner) return false;
        Float t = nextHitTime.get(target);
        return t == null || now >= t;
    }

    /**
     * Ghi nhận 1 lần trúng: tăng bộ đếm, đặt thời điểm sớm nhất đối thủ này bị trúng lại,
     * và đánh dấu cả loạt xong nếu đã đủ maxHits (maxHits = 0 thì không bao giờ xong ở đây).
     * Chỉ gọi sau khi canHit() trả về true.
     */
    void registerHit(Object target, float now) {
        hits++;
        nextHitTime.put(target, now + hitInterval);
        if (maxHits > 0 && hits >= maxHits) done = true;
    }
}
