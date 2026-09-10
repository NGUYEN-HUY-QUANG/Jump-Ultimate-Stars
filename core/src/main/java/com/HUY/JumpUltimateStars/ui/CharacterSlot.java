package com.HUY.JumpUltimateStars.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.HUY.JumpUltimateStars.character.CharacterData;

/**
 * Đại diện cho 1 ô (slot) chứa nhân vật trong màn hình chọn character.
 * Class này chịu trách nhiệm:
 *  - Lưu vị trí, kích thước của ô trên màn hình.
 *  - Quản lý trạng thái hiển thị (bình thường / đang chọn / đang xác nhận / đã xác nhận).
 *  - Vẽ viền, hiệu ứng glow (phát sáng) và avatar nhân vật tương ứng.
 */
public class CharacterSlot {

    /**
     * Các trạng thái có thể có của 1 slot:
     *  - NORMAL: chưa được tương tác, viền màu đen, không có glow.
     *  - SELECTED: đang được người chơi rê tới/chọn, viền trắng.
     *  - CONFIRMING: đang trong quá trình xác nhận chọn nhân vật,
     *                viền sẽ nhấp nháy đổi màu (hiệu ứng flash) trong FLASH_DURATION giây.
     *  - CONFIRMED: đã xác nhận xong, viền chuyển hẳn sang màu đỏ.
     */
    public enum State { NORMAL, SELECTED, CONFIRMING, CONFIRMED }

    // Vị trí (góc dưới trái) và kích thước (ô vuông) của slot, tính theo đơn vị world/pixel.
    private float x, y, size;

    // Trạng thái hiện tại của slot, mặc định là NORMAL.
    private State state = State.NORMAL;

    // Dữ liệu nhân vật (avatar, thông tin...) mà slot này đại diện.
    private final CharacterData data;

    // Bộ đếm thời gian dùng cho hiệu ứng flash màu khi ở trạng thái CONFIRMING.
    private float flashTimer = 0f;

    // Thời gian (giây) để hiệu ứng CONFIRMING chạy xong rồi tự chuyển sang CONFIRMED.
    private static final float FLASH_DURATION = 1f;

    // Độ dày của viền ô, chỉnh giá trị này để viền mỏng/dày hơn.
    private static final float BORDER_THICKNESS = 0.5f; // <-- chỉnh mỏng/dày ở đây

    // ----- cấu hình hiệu ứng glow (phát sáng quanh viền) -----
    // Số lớp glow được vẽ chồng lên nhau; càng nhiều lớp thì glow càng mượt nhưng càng tốn hiệu năng.
    private static final int GLOW_LAYERS = 5;
    // Khoảng cách xa nhất (px) mà glow tỏa ra so với viền gốc (ở lớp ngoài cùng).
    private static final float GLOW_MAX_SPREAD = 5f;
    // Độ sáng (alpha) tối đa của lớp glow trong cùng, tức lớp gần viền nhất.
    private static final float GLOW_MAX_ALPHA = 1f;

    public CharacterSlot(CharacterData data) {
        this.data = data;
    }

    /** Thiết lập vị trí và kích thước hiển thị của slot trên màn hình. */
    public void setPosition(float x, float y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    /**
     * Đổi trạng thái của slot.
     * Nếu chuyển sang CONFIRMING thì reset lại flashTimer để hiệu ứng nhấp nháy
     * bắt đầu lại từ đầu.
     */
    public void setState(State newState) {
        this.state = newState;
        if (newState == State.CONFIRMING) flashTimer = 0f;
    }

    /**
     * Cập nhật logic theo thời gian (gọi mỗi frame).
     * Khi đang ở trạng thái CONFIRMING, tăng flashTimer; khi timer vượt quá
     * FLASH_DURATION thì tự động chuyển trạng thái sang CONFIRMED.
     */
    public void update(float delta) {
        if (state == State.CONFIRMING) {
            flashTimer += delta;
            if (flashTimer >= FLASH_DURATION) {
                state = State.CONFIRMED;
            }
        }
    }

    /**
     * Trả về màu hiện tại của viền/glow dựa theo trạng thái.
     * Riêng CONFIRMING sẽ tính màu theo hue xoay vòng (dựa vào flashTimer)
     * để tạo hiệu ứng nhấp nháy đổi màu liên tục (cầu vồng chạy nhanh x4 vòng
     * trong suốt FLASH_DURATION).
     */
    private Color getCurrentColor() {
        switch (state) {
            case SELECTED:
                return Color.WHITE;
            case CONFIRMING:
                // hue chạy từ 0 -> 360*4 độ trong FLASH_DURATION giây, rồi mod 360
                // để tạo hiệu ứng xoay màu cầu vồng nhanh.
                float hue = (flashTimer / FLASH_DURATION) * 360f * 4f;
                return new Color().fromHsv(hue % 360f, 1f, 1f);
            case CONFIRMED:
                return Color.RED;
            default:
                // NORMAL: màu đen, không nổi bật.
                return Color.BLACK;
        }
    }

    /**
     * Vẽ hiệu ứng phát sáng (glow) quanh viền ô, chỉ áp dụng khi slot đang
     * được chú ý (không glow ở trạng thái NORMAL màu đen).
     *
     * Cách hoạt động: vẽ nhiều lớp viền (GLOW_LAYERS lớp) chồng lên nhau,
     * lớp càng xa viền gốc thì càng mờ và càng dày, lớp càng gần viền gốc
     * thì càng sáng và càng mỏng, tạo cảm giác ánh sáng lan tỏa dần ra ngoài.
     * Sử dụng additive blending (cộng dồn màu sáng) để các lớp chồng lên
     * nhau tạo hiệu ứng rực sáng hơn là bị che khuất.
     *
     * Lưu ý: phải gọi TRƯỚC renderBorder, và gọi giữa
     * shapeRenderer.begin(ShapeType.Filled) ... end()
     */
    public void renderGlow(ShapeRenderer sr) {
        if (state == State.NORMAL) return;

        Color base = getCurrentColor();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        // Blend additive: màu mới được CỘNG THÊM vào màu đã có trên buffer,
        // giúp các lớp glow chồng lên nhau trông sáng rực hơn thay vì bị trộn mờ đi.
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Vẽ từ lớp ngoài cùng (xa nhất, i = GLOW_LAYERS) vào lớp trong cùng (i = 1)
        for (int i = GLOW_LAYERS; i >= 1; i--) {
            float t = (float) i / GLOW_LAYERS;           // t: 1 (lớp ngoài/xa) -> gần 0 (lớp sát viền)
            float spread = GLOW_MAX_SPREAD * t;           // lớp càng xa thì spread càng lớn
            // Alpha: lớp gần viền (t nhỏ) thì alpha cao hơn (sáng hơn),
            // cộng thêm 1 lượng nền (15% GLOW_MAX_ALPHA) để không lớp nào bị alpha = 0 hoàn toàn.
            float alpha = GLOW_MAX_ALPHA * (1f - t) + GLOW_MAX_ALPHA * 0.15f;

            sr.setColor(base.r, base.g, base.b, alpha);

            // Toạ độ và kích thước của "khung viền ảo" cho lớp glow này,
            // mở rộng ra ngoài viền gốc một khoảng bằng spread.
            float gx = x - spread;
            float gy = y - spread;
            float gSize = size + spread * 2f;
            float gThickness = BORDER_THICKNESS + spread;

            // Vẽ 4 cạnh (dưới, trên, trái, phải) tạo thành khung viền rỗng ở giữa.
            sr.rect(gx, gy, gSize, gThickness);                              // dưới
            sr.rect(gx, gy + gSize - gThickness, gSize, gThickness);         // trên
            sr.rect(gx, gy, gThickness, gSize);                              // trái
            sr.rect(gx + gSize - gThickness, gy, gThickness, gSize);         // phải
        }

        // Trả blend mode về chế độ thông thường (alpha blending) để không ảnh hưởng
        // tới các phần vẽ khác sau đó.
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Vẽ viền chính (không glow) của slot theo màu ứng với trạng thái hiện tại.
     * Gọi giữa shapeRenderer.begin(ShapeType.Filled) ... end()
     */
    public void renderBorder(ShapeRenderer sr) {
        sr.setColor(getCurrentColor());
        sr.rect(x, y, size, BORDER_THICKNESS);                               // cạnh dưới
        sr.rect(x, y + size - BORDER_THICKNESS, size, BORDER_THICKNESS);     // cạnh trên
        sr.rect(x, y, BORDER_THICKNESS, size);                               // cạnh trái
        sr.rect(x + size - BORDER_THICKNESS, y, BORDER_THICKNESS, size);     // cạnh phải
    }

    /**
     * Vẽ avatar của nhân vật bên trong ô, có chừa 1 khoảng inset để không
     * bị đè lên viền.
     * Gọi giữa batch.begin() ... end()
     */
    public void renderAvatar(SpriteBatch batch) {
        if (data.avatar != null) {
            float inset = BORDER_THICKNESS + 4f;
            batch.draw(data.avatar, x + inset, y + inset, size - inset * 2, size - inset * 2);
        }
    }

    /** Kiểm tra 1 điểm (px, py) có nằm trong vùng của slot hay không (dùng để xử lý click/hover). */
    public boolean contains(float px, float py) {
        return px >= x && px <= x + size && py >= y && py <= y + size;
    }

    public State getState() { return state; }
    public CharacterData getData() { return data; }
}
