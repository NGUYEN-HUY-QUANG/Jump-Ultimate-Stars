package com.HUY.JumpUltimateStars.effect;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationFactory;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Quản lý mọi effect đang tồn tại trong trận (phi tiêu, chùm năng lượng...) và cache
 * animation của chúng.
 *
 * Việc của class này:
 *  - Sinh effect khi Fighter yêu cầu (spawnVolley).
 *  - Mỗi frame: cập nhật vị trí / thời gian sống và dọn effect đã hết hạn.
 *  - Vẽ các effect còn sống.
 *  - Cung cấp danh sách effect + đồng hồ để GameScreen check va chạm.
 *
 * Class này KHÔNG biết gì về máu, đối thủ hay luật trúng đòn: luật trúng đòn nằm trong
 * Volley (đếm hit, khoảng nghỉ) và Projectile.tryHit().
 *
 * Vòng đời: GameScreen sở hữu. Tạo trong show(), gọi update()/draw() mỗi frame,
 * và PHẢI dispose() khi thoát (Texture là tài nguyên native, không tự được GC dọn).
 * Fighter chỉ dùng chứ không sở hữu, không được dispose.
 *
 * Asset effect theo quy ước: effects/{id}/{id}_0.png, {id}_1.png, ...
 */
public class EffectManager {

    // Factory dùng để load frame ảnh từ disk khi cache chưa có
    private final CharacterAnimationFactory factory = new CharacterAnimationFactory();

    // Cache animation theo id của effect (ProjectileSpec.id), để nhiều viên đạn / nhiều lần bắn
    // dùng chung 1 animation, không load lại Texture mỗi lần bắn
    private final ObjectMap<String, Animation<TextureRegion>> cache = new ObjectMap<>();

    // Danh sách effect đang tồn tại (đang bay hoặc đang chạy animation)
    private final Array<Projectile> projectiles = new Array<>();

    // Đồng hồ của effect (giây), dùng để tính khoảng nghỉ hitInterval
    private float time = 0f;

    /**
     * Lấy animation của effect từ cache, chưa có thì load rồi lưu lại.
     * Lưu ý: cache theo spec.id nên frameDuration và playMode của lần load đầu tiên
     * được dùng cho các lần sau.
     */
    private Animation<TextureRegion> getAnim(ProjectileSpec spec) {
        Animation<TextureRegion> a = cache.get(spec.id);
        if (a == null) {
            // endWithAnimation chạy 1 lần rồi tắt, đạn bay thì lặp cho tới hết tầm
            a = factory.loadFrames("effects/" + spec.id, spec.id, spec.frameDuration,
                spec.endWithAnimation ? Animation.PlayMode.NORMAL : Animation.PlayMode.LOOP);
            cache.put(spec.id, a);
        }
        return a;
    }

    /**
     * Bắn 1 loạt (spec.count viên xếp cột dọc, viên giữa ở độ cao midY).
     * Cả loạt dùng chung 1 Volley nên được tính như 1 skill: số lần trúng và khoảng nghỉ
     * giữa các lần trúng tính chung, không tính riêng từng viên.
     *
     * @param spec  dữ liệu loại effect
     * @param owner Fighter đã bắn (để bỏ qua khi check va chạm, không tự trúng mình)
     * @param x     vị trí ngang sinh ra (tâm viên đạn, hoặc mép gần nhân vật nếu endWithAnimation)
     * @param midY  độ cao của viên giữa cột (tính từ mặt đất lên)
     * @param dir   hướng bay: -1 trái, 1 phải
     */
    public void spawnVolley(ProjectileSpec spec, Object owner, float x, float midY, int dir) {
        Volley volley = new Volley(owner, spec);
        Animation<TextureRegion> anim = getAnim(spec);
        for (int i = 0; i < spec.count; i++) {
            // Lệch so với viên giữa: count = 3 -> -1, 0, +1 (nhân spacingY)
            float y = midY + (i - (spec.count - 1) / 2f) * spec.spacingY;
            projectiles.add(new Projectile(spec, anim, volley, x, y, dir));
        }
    }

    /** Cập nhật mỗi frame: tăng đồng hồ, cho từng effect chạy, rồi dọn effect đã hết hạn. */
    public void update(float delta) {
        time += delta;
        // Duyệt ngược để removeIndex không làm lệch các phần tử chưa duyệt
        for (int i = projectiles.size - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(delta);
            if (!p.isActive()) projectiles.removeIndex(i);
        }
    }

    /** Vẽ các effect còn sống. Gọi giữa batch.begin() ... batch.end(). */
    public void draw(SpriteBatch batch) {
        for (Projectile p : projectiles) p.draw(batch);
    }

    /** Đồng hồ hiện tại của effect (giây), truyền vào Projectile.tryHit() khi check va chạm. */
    public float getTime() { return time; }

    /** Để GameScreen duyệt effect và check va chạm. */
    public Array<Projectile> getProjectiles() { return projectiles; }

    /**
     * Giải phóng toàn bộ Texture của các animation đã cache (GPU, không tự được GC dọn)
     * và xóa effect đang tồn tại. BẮT BUỘC gọi trong dispose() của GameScreen.
     */
    public void dispose() {
        for (Animation<TextureRegion> a : cache.values()) {
            for (TextureRegion r : a.getKeyFrames()) r.getTexture().dispose();
        }
        cache.clear();
        projectiles.clear();
    }
}
