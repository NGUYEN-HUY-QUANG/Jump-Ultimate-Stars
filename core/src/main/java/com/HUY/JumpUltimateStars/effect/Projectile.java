package com.HUY.JumpUltimateStars.effect;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * 1 viên đạn/effect đang tồn tại trong trận. Cả loạt chung 1 Volley, nên luật trúng đòn
 * (số lần trúng tối đa, khoảng nghỉ giữa 2 lần trúng) tính chung cho cả loạt chứ không
 * tính riêng từng viên.
 *
 * Có 2 kiểu, chọn bằng ProjectileSpec.endWithAnimation:
 *  - Kiểu bay (mặc định): bay thẳng theo hướng dir, bay đủ range thì biến mất.
 *    x, y là TÂM viên đạn. Animation lặp (LOOP) trong lúc bay.
 *  - Kiểu endWithAnimation (vd chùm Kamehameha): đứng yên tại chỗ sinh ra, x là MÉP GẦN
 *    nhân vật, frame mọc dài ra phía trước, hết animation thì biến mất.
 *    y là tâm theo chiều dọc. Hitbox theo kích thước frame hiện tại nên chùm dài ra
 *    thì vùng trúng cũng dài ra.
 *
 * Class này KHÔNG tự check va chạm hay trừ máu: GameScreen duyệt danh sách effect,
 * so getHitbox() với hurtbox đối thủ, rồi gọi tryHit() để biết lần này có được tính dame không.
 */
public class Projectile {

    // Dữ liệu tĩnh của loại effect này (tốc độ, tầm, dame, scale...), dùng chung giữa nhiều viên
    private final ProjectileSpec spec;

    // Animation dùng chung từ cache của EffectManager. Không sở hữu, không dispose ở đây
    private final Animation<TextureRegion> anim;

    // Loạt đạn mà viên này thuộc về: giữ chung bộ đếm hit và chủ đạn
    private final Volley volley;

    // Hướng bay / hướng mọc dài: -1 trái, 1 phải
    private final int dir;

    // Vị trí (xem mô tả 2 kiểu ở trên: kiểu bay là tâm, kiểu endWithAnimation là mép gần)
    private float x, y;

    // Quãng đường đã bay (px), so với spec.range để biết khi nào biến mất (chỉ kiểu bay)
    private float traveled;

    // Thời gian đã trôi qua kể từ khi sinh ra (giây), dùng để chọn frame animation
    private float stateTime;

    // false khi đã hết tầm / hết animation. Xem thêm isActive() (còn xét trạng thái của Volley)
    private boolean active = true;

    // Dùng lại 1 đối tượng cho mọi lần gọi getHitbox() để không tạo rác mỗi frame
    private final Rectangle hitbox = new Rectangle();

    /** Package-private: chỉ EffectManager được tạo Projectile. */
    Projectile(ProjectileSpec spec, Animation<TextureRegion> anim,
               Volley volley, float x, float y, int dir) {
        this.spec = spec; this.anim = anim; this.volley = volley;
        this.x = x; this.y = y; this.dir = dir;
    }

    /** Cập nhật mỗi frame: tăng thời gian, rồi di chuyển (kiểu bay) hoặc chờ hết animation. */
    public void update(float delta) {
        if (!isActive()) return;
        stateTime += delta;

        if (spec.endWithAnimation) {
            if (anim.isAnimationFinished(stateTime)) active = false;  // hết animation thì tắt
            return;                                                   // đứng yên, không bay
        }

        float step = spec.speed * delta;
        x += dir * step;
        traveled += step;
        if (traveled >= spec.range) active = false;                   // hết tầm thì biến mất
    }

    /** Vẽ frame hiện tại. Gọi giữa batch.begin() ... batch.end(). */
    public void draw(SpriteBatch batch) {
        if (!isActive()) return;
        TextureRegion f = anim.getKeyFrame(stateTime);
        // Kích thước vẽ = kích thước ảnh gốc * scale
        float w = f.getRegionWidth() * spec.scale;
        float h = f.getRegionHeight() * spec.scale;
        // Lật bằng width âm, không đụng TextureRegion đang cache dùng chung
        if (spec.endWithAnimation) {
            // x là mép gần nhân vật: vẽ từ x và mọc ra phía trước (width âm = vẽ sang trái)
            batch.draw(f, x, y - h / 2f, dir < 0 ? -w : w, h);
        } else if (dir < 0) {
            // x là tâm: lùi nửa bề rộng để căn giữa (bay sang trái, vẽ lật)
            batch.draw(f, x + w / 2f, y - h / 2f, -w, h);
        } else {
            batch.draw(f, x - w / 2f, y - h / 2f, w, h);
        }
    }

    /**
     * Hitbox theo kích thước frame hiện tại (đã nhân scale), cùng vùng với hình đang vẽ.
     * Trả về 1 Rectangle dùng lại, nên đừng giữ tham chiếu qua nhiều frame.
     */
    public Rectangle getHitbox() {
        TextureRegion f = anim.getKeyFrame(stateTime);
        float w = f.getRegionWidth() * spec.scale;
        float h = f.getRegionHeight() * spec.scale;
        if (spec.endWithAnimation) {
            // Mép gần ở x, mọc ra phía trước: quay trái thì góc dưới-trái lùi một bề rộng
            return hitbox.set(dir < 0 ? x - w : x, y - h / 2f, w, h);
        }
        return hitbox.set(x - w / 2f, y - h / 2f, w, h);
    }

    /**
     * Gọi khi hitbox chạm đối thủ. Trả về true nếu LẦN NÀY được tính dame
     * (còn lượt, đã hết khoảng nghỉ, không phải chủ đạn). Hết lượt thì cả loạt tự biến mất.
     *
     * @param target đối thủ đang bị chạm (dùng làm khóa để tính khoảng nghỉ riêng từng đối thủ)
     * @param now    đồng hồ hiện tại, lấy từ EffectManager.getTime()
     */
    public boolean tryHit(Object target, float now) {
        if (!isActive() || !volley.canHit(target, now)) return false;
        volley.registerHit(target, now);
        return true;
    }

    /** Còn tồn tại không: chưa hết tầm / hết animation VÀ cả loạt chưa hết lượt trúng. */
    public boolean isActive() { return active && !volley.isDone(); }

    /** Dame mỗi lần trúng (lấy từ spec, dùng chung cho cả loạt). */
    public int getDamage() { return spec.damage; }

    /** Fighter đã bắn, để GameScreen bỏ qua khi check va chạm (không tự trúng mình). */
    public Object getOwner() { return volley.owner; }
}
