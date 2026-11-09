package com.HUY.JumpUltimateStars.hethong;

import com.HUY.JumpUltimateStars.character.Fighter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

/**
 * Camera trận đấu: nhìn vào "thế giới" (kích thước = ảnh map), tự zoom + di chuyển
 * theo vị trí 2 nhân vật.
 *  - Gần nhau  -> khung nhìn hẹp (zoom vào, nhân vật to).
 *  - Xa nhau   -> khung nhìn rộng (zoom ra).
 *  - Khung nhìn không bao giờ lớn hơn map và không bao giờ trượt ra ngoài map.
 *
 * Cách dùng: new FightCamera(...) -> snapTo(...) lúc vào trận -> mỗi frame update(...)
 * rồi dùng getCamera().combined làm projection cho batch / shapeRenderer.
 */
public class FightCamera {

    // ===== Chỉnh cảm giác camera ở đây (đơn vị: pixel của THẾ GIỚI = pixel ảnh map) =====

    /** Khung nhìn rộng tối thiểu (zoom vào tối đa). Nhỏ hơn = nhân vật to hơn khi đứng sát nhau. */
    private static final float MIN_VIEW_W = 800f;

    /** Lề ngang chừa thêm ở 2 bên nhân vật ngoài cùng. */
    private static final float PADDING_X = 250f;

    /** Lề dọc chừa thêm trên / dưới. */
    private static final float PADDING_Y = 150f;

    /** Camera nhắm vào điểm cao hơn chân nhân vật bấy nhiêu (xấp xỉ giữa thân). */
    private static final float BODY_OFFSET_Y = 80f;

    /** Độ mượt: càng lớn camera đuổi theo càng nhanh. */
    private static final float SMOOTH = 6f;

    private final OrthographicCamera cam = new OrthographicCamera();
    private final float worldW, worldH;

    // Đích mà camera đang đuổi theo
    private float targetX, targetY, targetViewW;

    public FightCamera(float screenW, float screenH, float worldW, float worldH) {
        this.worldW = worldW;
        this.worldH = worldH;
        cam.setToOrtho(false, screenW, screenH);
    }

    public OrthographicCamera getCamera() { return cam; }

    /** Khi đổi kích thước cửa sổ: giữ nguyên vị trí, chỉ đổi khung. */
    public void resize(float screenW, float screenH) {
        cam.viewportWidth = screenW;
        cam.viewportHeight = screenH;
    }

    /** Đặt camera vào đúng đích ngay lập tức (không lerp). Gọi 1 lần lúc vào trận. */
    public void snapTo(Fighter a, Fighter b) {
        computeTarget(a, b);
        cam.zoom = targetViewW / cam.viewportWidth;
        cam.position.set(targetX, targetY, 0f);
        clampToWorld();
        cam.update();
    }

    /** Gọi mỗi frame SAU khi các Fighter đã update. b có thể null (chế độ 1 người). */
    public void update(float delta, Fighter a, Fighter b) {
        computeTarget(a, b);

        float k = Math.min(1f, SMOOTH * delta);
        float viewW = cam.zoom * cam.viewportWidth;
        viewW = Math.min(MathUtils.lerp(viewW, targetViewW, k), maxViewW());
        cam.zoom = viewW / cam.viewportWidth;

        cam.position.x = MathUtils.lerp(cam.position.x, targetX, k);
        cam.position.y = MathUtils.lerp(cam.position.y, targetY, k);

        clampToWorld();
        cam.update();
    }

    /** Khung nhìn rộng tối đa = vừa khít map (theo cả chiều ngang lẫn chiều dọc). */
    private float maxViewW() {
        float aspect = cam.viewportWidth / cam.viewportHeight;
        return Math.min(worldW, worldH * aspect);
    }

    private void computeTarget(Fighter a, Fighter b) {
        float ax = a.getCenterX();
        float ay = a.getFeetY() + BODY_OFFSET_Y;
        float minX = ax, maxX = ax, minY = ay, maxY = ay;

        if (b != null) {
            float bx = b.getCenterX();
            float by = b.getFeetY() + BODY_OFFSET_Y;
            minX = Math.min(ax, bx);
            maxX = Math.max(ax, bx);
            minY = Math.min(ay, by);
            maxY = Math.max(ay, by);
        }

        float aspect = cam.viewportWidth / cam.viewportHeight;
        float maxW = maxViewW();
        float minW = Math.min(MIN_VIEW_W, maxW);

        // Cần chứa đủ cả chiều ngang lẫn chiều dọc giữa 2 nhân vật
        float needW = Math.max((maxX - minX) + 2f * PADDING_X,
            ((maxY - minY) + 2f * PADDING_Y) * aspect);

        targetViewW = MathUtils.clamp(needW, minW, maxW);
        targetX = (minX + maxX) / 2f;
        targetY = (minY + maxY) / 2f;
    }

    /** Giữ khung nhìn nằm trong map (không lòi nền đen). */
    private void clampToWorld() {
        float halfW = cam.zoom * cam.viewportWidth / 2f;
        float halfH = cam.zoom * cam.viewportHeight / 2f;
        cam.position.x = MathUtils.clamp(cam.position.x, halfW, worldW - halfW);
        cam.position.y = MathUtils.clamp(cam.position.y, halfH, worldH - halfH);
    }
}
