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
 *  - MapInfo: số liệu tĩnh theo PIXEL ẢNH (gốc DƯỚI-trái, y hướng lên), dùng chung được, không giữ tài nguyên.
 *  - MapData: giữ Texture, nhân số liệu với scale để ra tọa độ THẾ GIỚI (cùng gốc dưới-trái).
 *
 * Vòng đời bắt buộc: new MapData(info) -> load() -> (dùng) -> dispose().
 *
 * THẾ GIỚI: rộng = ảnh * scale, cao = ảnh * scale. Mọi hàm trả về / nhận pixel thế giới.
 * Fighter, effect và camera đều dùng hệ này.
 */
public class MapData {

    // Độ dày (pixel thế giới) của đường vẽ debug / map tạm
    private static final float GROUND_THICKNESS = 4f;

    // Bật để: vẽ đường mặt đất (đỏ) + platform (xanh lá) lên trên ảnh, và click chuột để
    // in tọa độ pixel ảnh ra console (giúp chỉnh số trong MapRegistry). Chỉnh xong thì tắt.
    public static final boolean DEBUG_PLATFORMS = false;

    private final MapInfo info;

    // Ảnh nền. null nếu map chưa có ảnh (map tạm) hoặc chưa gọi load()
    private Texture background;

    // Kích thước thế giới (pixel) = ảnh * scale. Được đặt trong load()
    private float worldW, worldH;

    public MapData(MapInfo info) { this.info = info; }

    /**
     * Load ảnh nền từ disk (nếu map có ảnh) và đặt kích thước thế giới = ảnh * scale.
     * Map tạm (không có ảnh) thì lấy kích thước cửa sổ hiện tại.
     */
    public void load() {
        if (info.backgroundPath != null) {
            background = new Texture(Gdx.files.internal(info.backgroundPath));
            background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            worldW = background.getWidth() * info.scale;
            worldH = background.getHeight() * info.scale;
        } else {
            worldW = Gdx.graphics.getWidth();
            worldH = Gdx.graphics.getHeight();
        }
    }

    /** Giải phóng Texture. BẮT BUỘC gọi trong dispose() của màn hình. */
    public void dispose() {
        if (background != null) background.dispose();
    }

    public float getWorldWidth() { return worldW; }
    public float getWorldHeight() { return worldH; }

    /** Đổi y pixel ảnh (gốc dưới-trái) sang y thế giới: chỉ cần nhân scale. */
    private float worldY(float imgY) { return imgY * info.scale; }

    /** Độ cao mặt đất chính (pixel thế giới). */
    public float getGroundY() { return worldY(info.groundY); }

    /** Tâm ngang (pixel thế giới) nơi P1 xuất hiện. */
    public float getP1SpawnX() { return info.p1SpawnX * info.scale; }

    /** Tâm ngang (pixel thế giới) nơi P2 xuất hiện. */
    public float getP2SpawnX() { return info.p2SpawnX * info.scale; }

    /** Vẽ ảnh nền đúng kích thước thế giới. Gọi giữa batch.begin() ... end(). */
    public void renderBackground(SpriteBatch batch) {
        if (background != null) batch.draw(background, 0, 0, worldW, worldH);
    }

    /**
     * Vẽ đường đỏ làm mặt đất tạm (map chưa có ảnh) và, nếu DEBUG_PLATFORMS bật,
     * vẽ thêm mặt đất chính (đỏ) + các platform (xanh lá) lên trên ảnh.
     * Gọi giữa shapeRenderer.begin(Filled) ... end().
     */
    public void renderPlaceholder(ShapeRenderer sr) {
        if (background != null && !DEBUG_PLATFORMS) return;

        sr.setColor(Color.RED);
        sr.rect(0, getGroundY() - GROUND_THICKNESS, worldW, GROUND_THICKNESS);

        if (DEBUG_PLATFORMS) {
            sr.setColor(Color.GREEN);
            for (float[] p : info.platforms) {
                sr.rect(p[0] * info.scale, worldY(p[2]) - GROUND_THICKNESS,
                    (p[1] - p[0]) * info.scale, GROUND_THICKNESS);
            }
        }
    }

    /**
     * One-way platform: chỉ đỡ khi nhân vật RƠI XUỐNG, không chặn khi nhảy lên.
     *
     * @param x         tâm ngang của nhân vật (pixel thế giới)
     * @param prevFeetY độ cao chân ở frame trước
     * @param feetY     độ cao chân ở frame này (sau khi di chuyển)
     * @return độ cao mặt platform nếu chân vừa đi xuyên từ trên xuống dưới nó, ngược lại -1
     */
    public float findLandingY(float x, float prevFeetY, float feetY) {
        float best = -1f;
        for (float[] p : info.platforms) {
            float py = worldY(p[2]);
            boolean inside = x >= p[0] * info.scale && x <= p[1] * info.scale;
            boolean crossed = prevFeetY >= py && feetY <= py;   // trước ở trên/ngang, giờ ở dưới/ngang
            if (inside && crossed && py > best) best = py;      // trúng nhiều cái thì lấy cái cao nhất
        }
        return best;
    }

    /**
     * Có platform nào ở độ cao y (pixel thế giới) mà x nằm trong khoảng của nó không.
     * Dùng để biết nhân vật còn đứng trên platform hay đã đi ra khỏi mép.
     */
    public boolean hasPlatformAt(float x, float y) {
        for (float[] p : info.platforms) {
            if (Math.abs(worldY(p[2]) - y) < 0.5f
                && x >= p[0] * info.scale && x <= p[1] * info.scale) return true;
        }
        return false;
    }

    /** Đổi điểm thế giới sang pixel ảnh (gốc dưới-trái) để in debug. Trả về {x, y}. */
    public float[] worldToImagePixel(float wx, float wy) {
        return new float[] { wx / info.scale, wy / info.scale };
    }
}
