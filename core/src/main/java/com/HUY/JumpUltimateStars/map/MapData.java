package com.HUY.JumpUltimateStars.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Bản "đang chạy" của một map: giữ Texture ảnh nền và biết cách vẽ map đó.
 *
 * Phân vai với MapInfo:
 *  - MapInfo: chỉ chứa số liệu tĩnh (đường dẫn ảnh, tỉ lệ mặt đất, vị trí spawn),
 *             dùng chung được, không giữ tài nguyên.
 *  - MapData: giữ tài nguyên native (Texture) nên mỗi lần vào trận phải tạo 1 instance mới.
 *
 * Vòng đời bắt buộc: new MapData(info) -> load() -> (dùng) -> dispose().
 * Quên dispose() sẽ bị leak Texture; gọi render trước load() thì map có ảnh sẽ không vẽ gì.
 *
 * Mọi vị trí đều tính theo TỈ LỆ màn hình (0..1) rồi nhân với kích thước thật,
 * nên map vẫn đúng khi đổi kích thước cửa sổ.
 */
public class MapData {

    // Độ dày (pixel) của đường đỏ vẽ làm mặt đất ở map tạm
    private static final float GROUND_THICKNESS = 4f;

    // Số liệu tĩnh của map này (đường dẫn ảnh, groundY, spawn...)
    private final MapInfo info;

    // Ảnh nền. null nếu map chưa có ảnh (map tạm) hoặc chưa gọi load()
    private Texture background;

    public MapData(MapInfo info) { this.info = info; }

    /**
     * Load ảnh nền từ disk (nếu map có ảnh). Gọi 1 lần trong show() của màn hình.
     * Map tạm (backgroundPath == null) thì không làm gì.
     */
    public void load() {
        if (info.backgroundPath != null) {
            background = new Texture(Gdx.files.internal(info.backgroundPath));
            // Linear filter để ảnh mượt khi bị kéo giãn theo kích thước cửa sổ
            background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    /** Giải phóng Texture. BẮT BUỘC gọi trong dispose() của màn hình, vì Texture không tự được GC dọn. */
    public void dispose() {
        if (background != null) background.dispose();
    }

    /** Độ cao mặt đất (pixel) = chiều cao màn hình * tỉ lệ (0 = đáy, 1 = đỉnh). */
    public float getGroundY(float screenH) { return screenH * info.groundYRatio; }

    /** Tâm ngang (pixel) nơi P1 xuất hiện. */
    public float getP1SpawnX(float screenW) { return screenW * info.p1SpawnXRatio; }

    /** Tâm ngang (pixel) nơi P2 xuất hiện. */
    public float getP2SpawnX(float screenW) { return screenW * info.p2SpawnXRatio; }

    /**
     * Vẽ ảnh nền, kéo giãn phủ kín màn hình (w x h). Gọi giữa batch.begin() ... end().
     * Map tạm (không có ảnh) thì bỏ qua, nền đen do ScreenUtils.clear() ở GameScreen lo.
     */
    public void renderBackground(SpriteBatch batch, float w, float h) {
        if (background != null) batch.draw(background, 0, 0, w, h);
    }

    /**
     * Vẽ đường đỏ làm mặt đất tạm. Gọi giữa shapeRenderer.begin(Filled) ... end().
     * Chỉ vẽ khi map CHƯA có ảnh; có ảnh rồi thì tự tắt, không cần sửa GameScreen.
     * Đường đỏ nằm ngay DƯỚI groundY (chân nhân vật đứng trên mép trên của đường).
     */
    public void renderPlaceholder(ShapeRenderer sr, float w, float h) {
        if (background != null) return;
        sr.setColor(Color.RED);
        sr.rect(0, getGroundY(h) - GROUND_THICKNESS, w, GROUND_THICKNESS);
    }
}
