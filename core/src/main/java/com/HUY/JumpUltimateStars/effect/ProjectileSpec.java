package com.HUY.JumpUltimateStars.effect;

/**
 * Dữ liệu tĩnh của 1 loại effect/đạn (phi tiêu, chùm năng lượng...). KHÔNG chứa logic:
 * việc bay, hết hạn nằm ở Projectile, luật trúng đòn (số lần trúng, khoảng nghỉ) nằm ở Volley.
 * Các field đều final nên 1 spec dùng chung an toàn giữa nhiều viên đạn và nhiều Fighter.
 *
 * Ảnh theo quy ước: effects/{id}/{id}_0.png, {id}_1.png, ...
 *
 * Có 2 kiểu effect, chọn bằng endWithAnimation:
 *  - Đạn bay (mặc định): bay thẳng theo hướng mặt, bay đủ range thì biến mất.
 *    Dùng speed, range. Animation lặp trong lúc bay.
 *  - Đứng yên theo animation (vd chùm Kamehameha): không bay, neo ở mép gần nhân vật,
 *    mọc dài ra phía trước, hết animation thì biến mất. speed và range bị bỏ qua.
 *
 * Kiểu trúng đòn chọn bằng maxHits và hitInterval:
 *  - Phi tiêu (trúng là mất):     maxHits = 1, hitInterval = 0
 *  - Xuyên người, trúng tối đa 3: maxHits = 3, hitInterval = 0.3f
 *  - Gây dame liên tục tới hết:   maxHits = 0, hitInterval = 0.1f (vd chùm Kamehameha)
 */
public class ProjectileSpec {

    /** Tên thư mục + tiền tố file ảnh, vd "small_shuriken". Cũng là khóa cache trong EffectManager. */
    public final String id;

    /**
     * Thời gian hiển thị mỗi frame của animation effect (giây).
     * Lưu ý: cache theo id nên giá trị của lần load đầu tiên được dùng cho các lần sau.
     */
    public final float frameDuration;

    /** Tốc độ bay (px/s). Bỏ qua khi endWithAnimation = true. */
    public final float speed;

    /** Quãng bay tối đa (px), bay hết thì biến mất. Bỏ qua khi endWithAnimation = true. */
    public final float range;

    /** Dame MỖI LẦN TRÚNG của cả loạt (các viên trong loạt không cộng dồn). */
    public final int damage;

    /**
     * Lệch ngang so với tâm nhân vật, theo hướng mặt (px).
     * Đạn bay: tới tâm viên đạn. endWithAnimation: tới mép gần của chùm.
     */
    public final float offsetX;

    /** Độ cao của viên GIỮA cột so với chân nhân vật (px). */
    public final float offsetY;

    /** Hệ số phóng to effect khi vẽ (nhân vào kích thước ảnh gốc). Hitbox cũng đổi theo. */
    public final float scale;

    /** Số viên bắn cùng lúc, xếp thành cột dọc. Cả loạt chung 1 Volley (tính như 1 skill). */
    public final int count;

    /**
     * Khoảng cách dọc giữa 2 viên liền nhau (px). Nên xấp xỉ chiều cao ảnh * scale:
     * nhỏ hơn thì các viên đè lên nhau, lớn hơn nhiều thì tách xa.
     */
    public final float spacingY;

    // ===================== LUẬT TRÚNG ĐÒN =====================

    /**
     * Số lần gây dame tối đa của cả loạt. Đủ số lần thì cả loạt biến mất.
     * 1 = trúng là biến mất, 0 = không giới hạn (tới hết tầm / hết animation).
     */
    public final int maxHits;

    /**
     * Khoảng nghỉ giữa 2 lần dame lên CÙNG 1 đối thủ (giây): "tính hit theo thời gian".
     * Bỏ qua nếu maxHits = 1.
     */
    public final float hitInterval;

    /**
     * true: đứng yên, neo mép gần nhân vật, hết animation thì biến mất (animation chạy 1 lần).
     * false: đạn bay thẳng, animation lặp.
     */
    public final boolean endWithAnimation;

    /**
     * Constructor đầy đủ.
     *
     * @param id               tên thư mục + tiền tố ảnh
     * @param frameDuration    giây mỗi frame
     * @param speed            tốc độ bay (px/s)
     * @param range            tầm bay tối đa (px)
     * @param damage           dame mỗi lần trúng
     * @param offsetX          lệch ngang so với tâm nhân vật (px)
     * @param offsetY          độ cao viên giữa so với chân (px)
     * @param scale            hệ số phóng to
     * @param count            số viên mỗi loạt
     * @param spacingY         khoảng cách dọc giữa 2 viên (px)
     * @param maxHits          số lần trúng tối đa (0 = không giới hạn)
     * @param hitInterval      khoảng nghỉ giữa 2 lần trúng cùng 1 đối thủ (giây)
     * @param endWithAnimation true = đứng yên tới hết animation
     */
    public ProjectileSpec(String id, float frameDuration, float speed, float range,
                          int damage, float offsetX, float offsetY, float scale,
                          int count, float spacingY,
                          int maxHits, float hitInterval, boolean endWithAnimation) {
        this.id = id; this.frameDuration = frameDuration; this.speed = speed;
        this.range = range; this.damage = damage;
        this.offsetX = offsetX; this.offsetY = offsetY; this.scale = scale;
        this.count = count; this.spacingY = spacingY;
        this.maxHits = maxHits; this.hitInterval = hitInterval;
        this.endWithAnimation = endWithAnimation;
    }

    /**
     * Bản rút gọn: đạn bay thẳng, trúng 1 lần là biến mất (phi tiêu hiện tại).
     * Tương đương maxHits = 1, hitInterval = 0, endWithAnimation = false.
     */
    public ProjectileSpec(String id, float frameDuration, float speed, float range,
                          int damage, float offsetX, float offsetY, float scale,
                          int count, float spacingY) {
        this(id, frameDuration, speed, range, damage, offsetX, offsetY, scale,
            count, spacingY, 1, 0f, false);
    }
}
