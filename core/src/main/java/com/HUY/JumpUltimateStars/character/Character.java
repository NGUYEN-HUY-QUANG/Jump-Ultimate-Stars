package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Đại diện cho 1 nhân vật đang được HIỂN THỊ ĐỘNG (có animation), khác với
 * CharacterData (chỉ là data tĩnh: id + avatar cho UI slot).
 * Dùng cho preview P1/P2 ở màn chọn nhân vật, và sau này dùng luôn trong trận đấu.
 *
 * Mỗi Character KHÔNG tự load/giữ Texture riêng — mọi animation đều lấy
 * (và dùng chung) từ CharacterAnimationManager, nên nhiều Character cùng
 * charId có thể chia sẻ chung dữ liệu Texture mà không tốn thêm bộ nhớ.
 */
public class Character {

    // Manager dùng chung (inject từ ngoài vào) để lấy Animation đã cache,
    // Character KHÔNG sở hữu và KHÔNG được tự ý dispose() cái này
    private final CharacterAnimationManager animManager;

    // Id nhân vật, dùng để tra cứu animation qua animManager (vd: "mario")
    private final String charId;

    // Animation hiện đang được chạy/hiển thị (vd: idle, walk, punch)
    private Animation<TextureRegion> currentAnim;

    // Tên action hiện tại, dùng để so sánh tránh reload/reset animation
    // không cần thiết khi playAnimation() được gọi lại với cùng action
    private String currentAction;

    // Thời gian đã trôi qua kể từ khi action hiện tại bắt đầu chạy (giây),
    // dùng để tính frame nào đang được hiển thị (Animation#getKeyFrame)
    private float stateTime = 0f;

    // Vị trí vẽ nhân vật trên màn hình (tọa độ world/screen tùy nơi gọi)
    private float x, y;

    // Hệ số scale, dùng để phóng to/thu nhỏ nhân vật khi vẽ
    private float scale = 1f;

    // Có lật ngang hình khi vẽ hay không (vd nhân vật quay trái/phải)
    private boolean flipX;

    public Character(CharacterAnimationManager animManager, String charId) {
        this.animManager = animManager;
        this.charId = charId;
    }

    /**
     * Đổi sang action khác, VD: "idle", "walk", "punch".
     * Không làm gì nếu đang chạy đúng action đó rồi, tránh việc animation
     * bị giật/reset về frame đầu mỗi lần hàm này được gọi (vd gọi mỗi frame ở update loop).
     *
     * @param action        tên action mới
     * @param frameDuration thời gian mỗi frame (chỉ áp dụng nếu action này
     *                      chưa từng được load — vì animManager cache theo (charId, action),
     *                      lần load đầu tiên sẽ quyết định frameDuration cho các lần sau)
     */
    public void playAnimation(String action, float frameDuration) {
        // Nếu đang chạy đúng action này rồi thì bỏ qua, không reset stateTime
        if (action.equals(currentAction)) return;

        // Lấy animation từ cache (hoặc load mới nếu chưa có) thông qua manager
        currentAnim = animManager.get(charId, action, frameDuration);
        currentAction = action;

        // Reset thời gian để animation mới bắt đầu từ frame đầu tiên
        stateTime = 0f;
    }

    /** Cập nhật thời gian chạy animation, gọi mỗi frame trong vòng lặp game (vd render()). */
    public void update(float delta) {
        stateTime += delta;
    }

    /**
     * Vẽ frame hiện tại của animation lên màn hình tại vị trí (x, y) đã set.
     * Không làm gì nếu chưa có animation nào được set (currentAnim == null).
     */
    public void draw(SpriteBatch batch) {
        if (currentAnim == null) return;

        // Lấy frame tương ứng với thời điểm hiện tại (dựa vào stateTime)
        TextureRegion frame = currentAnim.getKeyFrame(stateTime);

        // Kích thước thực tế khi vẽ = kích thước gốc của frame * scale
        float w = frame.getRegionWidth() * scale;
        float h = frame.getRegionHeight() * scale;

        if (flipX) {
            // Lật ngang bằng cách vẽ với width âm, bắt đầu từ (x + w) thay vì x.
            // Cách này không đụng vào TextureRegion gốc đang được cache dùng chung,
            // nên không ảnh hưởng tới Character khác cũng dùng chung animation này.
            batch.draw(frame, x + w, y, -w, h);
        } else {
            batch.draw(frame, x, y, w, h);
        }
    }

    /** Bề rộng hiện tại của frame đang hiển thị (đã nhân scale) — dùng để canh giữa chính xác. */
    public float getCurrentFrameWidth() {
        if (currentAnim == null) return 0f;
        return currentAnim.getKeyFrame(stateTime).getRegionWidth() * scale;
    }

    /** Chiều cao hiện tại của frame đang hiển thị (đã nhân scale). */
    public float getCurrentFrameHeight() {
        if (currentAnim == null) return 0f;
        return currentAnim.getKeyFrame(stateTime).getRegionHeight() * scale;
    }

    // ==== Setter / Getter đơn giản ====

    /** Đặt vị trí vẽ nhân vật (góc dưới-trái, theo convention của SpriteBatch#draw). */
    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    /** Đặt hệ số scale để phóng to/thu nhỏ nhân vật khi vẽ. */
    public void setScale(float scale) { this.scale = scale; }

    /** Bật/tắt lật ngang hình (vd đổi hướng quay của nhân vật). */
    public void setFlipX(boolean flip) { this.flipX = flip; }

    /** Lấy id của nhân vật này. */
    public String getCharId() { return charId; }
}
