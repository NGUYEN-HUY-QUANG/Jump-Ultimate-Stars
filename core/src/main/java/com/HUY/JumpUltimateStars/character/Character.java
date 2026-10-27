package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Đại diện cho 1 nhân vật đang được HIỂN THỊ ĐỘNG (có animation), khác với
 * CharacterData (chỉ là data tĩnh: id + avatar cho UI slot).
 * Dùng cho preview P1/P2 ở màn chọn nhân vật và cho nhân vật trong trận đấu (qua Fighter).
 *
 * Character chỉ lo 2 việc: chọn animation đang chạy và vẽ frame hiện tại.
 * Nó KHÔNG biết gì về input, di chuyển, đòn đánh hay dame (những thứ đó nằm ở Fighter).
 *
 * Mỗi Character KHÔNG tự load/giữ Texture riêng: mọi animation đều lấy
 * (và dùng chung) từ CharacterAnimationManager, nên nhiều Character cùng
 * charId có thể chia sẻ chung dữ liệu Texture mà không tốn thêm bộ nhớ.
 *
 * Có 2 chế độ vẽ:
 *  - Chế độ preview (màn chọn nhân vật): không đặt refWidth, vẽ theo setPosition(x, y).
 *  - Chế độ trận đấu (Fighter): đặt refWidth + setCenter(), vẽ theo ô neo cố định
 *    để frame có effect dài ra không làm thân nhân vật bị lệch.
 */
public class Character {

    // Manager dùng chung (inject từ ngoài vào) để lấy Animation đã cache.
    // Character KHÔNG sở hữu và KHÔNG được tự ý dispose() cái này.
    private final CharacterAnimationManager animManager;

    // Id nhân vật, dùng để tra cứu animation qua animManager (vd "naruto_base")
    private final String charId;

    // Animation hiện đang được chạy/hiển thị (vd idle, run, J1)
    private Animation<TextureRegion> currentAnim;

    // Tên action hiện tại, dùng để so sánh, tránh reset animation
    // không cần thiết khi playAnimation() được gọi lại với cùng action
    private String currentAction;

    // Thời gian đã trôi qua kể từ khi action hiện tại bắt đầu chạy (giây),
    // dùng để tính frame nào đang được hiển thị (Animation#getKeyFrame)
    private float stateTime = 0f;

    // Vị trí góc dưới-trái dùng ở CHẾ ĐỘ PREVIEW (đặt bằng setPosition).
    // y còn được dùng chung ở chế độ trận đấu (đặt bằng setCenter).
    private float x, y;

    // Hệ số scale, dùng để phóng to/thu nhỏ nhân vật khi vẽ
    private float scale = 1f;

    // Có lật ngang hình khi vẽ hay không (nhân vật quay trái/phải)
    private boolean flipX;

    // --- Dùng cho CHẾ ĐỘ TRẬN ĐẤU ---

    // Tâm neo theo chiều ngang của nhân vật (thay cho x ở chế độ trận đấu)
    private float centerX;

    // Bề rộng ô neo cố định (đã nhân scale), thường lấy từ frame idle.
    // = 0 nghĩa là chưa đặt -> draw() dùng chế độ preview.
    private float refWidth = 0f;

    public Character(CharacterAnimationManager animManager, String charId) {
        this.animManager = animManager;
        this.charId = charId;
    }

    /**
     * Đổi sang action khác, vd "idle", "run".
     * Không làm gì nếu đang chạy đúng action đó rồi, tránh việc animation
     * bị giật/reset về frame đầu mỗi lần hàm này được gọi (vd gọi mỗi frame trong update).
     *
     * Muốn phát lại từ đầu kể cả khi trùng tên (vd đánh lại cùng 1 đòn) thì dùng playFromStart().
     *
     * @param action        tên action mới
     * @param frameDuration thời gian mỗi frame (chỉ có tác dụng nếu action này
     *                      chưa từng được load, vì animManager cache theo (charId, action),
     *                      lần load đầu tiên quyết định frameDuration cho các lần sau)
     */
    public void playAnimation(String action, float frameDuration) {
        // Đang chạy đúng action này rồi thì bỏ qua, không reset stateTime
        if (action.equals(currentAction)) return;

        // Lấy animation từ cache (hoặc load mới nếu chưa có) thông qua manager
        currentAnim = animManager.get(charId, action, frameDuration);
        currentAction = action;

        // Reset thời gian để animation mới bắt đầu từ frame đầu tiên
        stateTime = 0f;
    }

    /**
     * Phát animation từ frame đầu, kể cả khi đang chạy đúng action đó.
     * Dùng cho đòn đánh: nếu dùng playAnimation() thì đánh lại cùng 1 đòn
     * sẽ bị bỏ qua và animation kẹt ở frame cuối.
     */
    public void playFromStart(String action, float frameDuration) {
        currentAnim = animManager.get(charId, action, frameDuration);
        currentAction = action;
        stateTime = 0f;
    }

    /** Cập nhật thời gian chạy animation, gọi mỗi frame trong vòng lặp game (vd render()). */
    public void update(float delta) {
        stateTime += delta;
    }

    // ==== Neo vị trí cho chế độ trận đấu ====

    /**
     * Đặt bề rộng ô neo (đã nhân scale). Thường truyền vào bề rộng frame idle.
     * Sau khi đặt (> 0), draw() chuyển sang chế độ trận đấu.
     */
    public void setRefWidth(float w) {
        this.refWidth = w;
    }

    /** Bề rộng ô neo hiện tại. Fighter dùng để clamp nhân vật trong màn hình. */
    public float getRefWidth() {
        return refWidth;
    }

    /**
     * Đặt vị trí theo tâm neo (chế độ trận đấu).
     *
     * @param centerX tâm ngang của ô neo
     * @param y       độ cao chân nhân vật (mặt đất)
     */
    public void setCenter(float centerX, float y) {
        this.centerX = centerX;
        this.y = y;
    }

    /**
     * Vẽ frame hiện tại của animation lên màn hình.
     * Không làm gì nếu chưa có animation nào được set (currentAnim == null).
     *
     * Chế độ trận đấu (refWidth > 0): mọi frame đều vẽ từ MÉP TRÁI của ô neo,
     * nên frame dài ra (có effect) chỉ tràn về phía trước, thân nhân vật đứng yên.
     * Chế độ preview (refWidth == 0): vẽ từ (x, y) như bình thường.
     */
    public void draw(SpriteBatch batch) {
        if (currentAnim == null) return;

        // Lấy frame tương ứng với thời điểm hiện tại (dựa vào stateTime)
        TextureRegion frame = currentAnim.getKeyFrame(stateTime);

        // Kích thước thực tế khi vẽ = kích thước gốc của frame * scale
        float w = frame.getRegionWidth() * scale;
        float h = frame.getRegionHeight() * scale;

        if (refWidth > 0f) {
            // Chế độ trận đấu: vẽ từ mép trái của ô neo cố định
            float left = centerX - refWidth / 2f;
            // Lật ngang bằng width âm, không đụng vào TextureRegion gốc đang được cache dùng chung
            if (flipX) batch.draw(frame, left + refWidth, y, -w, h);
            else       batch.draw(frame, left, y, w, h);
        } else {
            // Chế độ preview (màn chọn nhân vật): vẽ theo setPosition(x, y)
            if (flipX) batch.draw(frame, x + w, y, -w, h);
            else       batch.draw(frame, x, y, w, h);
        }
    }

    /**
     * Bề rộng của frame đang hiển thị (đã nhân scale).
     * Lưu ý: giá trị đổi theo frame (frame có effect sẽ rộng hơn), nên KHÔNG dùng để
     * căn vị trí trong trận đấu, chỉ dùng để lấy bề rộng neo ban đầu từ frame idle.
     */
    public float getCurrentFrameWidth() {
        if (currentAnim == null) return 0f;
        return currentAnim.getKeyFrame(stateTime).getRegionWidth() * scale;
    }

    /** Chiều cao của frame đang hiển thị (đã nhân scale). */
    public float getCurrentFrameHeight() {
        if (currentAnim == null) return 0f;
        return currentAnim.getKeyFrame(stateTime).getRegionHeight() * scale;
    }

    // ==== Setter / Getter đơn giản ====

    /** Đặt vị trí góc dưới-trái (dùng cho chế độ preview ở màn chọn nhân vật). */
    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    /** Đặt hệ số scale để phóng to/thu nhỏ nhân vật khi vẽ. */
    public void setScale(float scale) { this.scale = scale; }

    /** Bật/tắt lật ngang hình (đổi hướng quay của nhân vật). */
    public void setFlipX(boolean flip) { this.flipX = flip; }

    /** Lấy id của nhân vật này. */
    public String getCharId() { return charId; }
}
