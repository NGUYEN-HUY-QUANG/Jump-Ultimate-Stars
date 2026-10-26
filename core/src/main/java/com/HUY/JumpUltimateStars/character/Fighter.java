package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.CharacterStatsRegistry;
import com.HUY.JumpUltimateStars.hethong.PlayerController;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Nhân vật đang chiến đấu trong GameScreen.
 *
 * Ghép 3 thứ lại với nhau:
 *  - Character:        lo phần animation + vẽ sprite (không biết gì về input hay di chuyển)
 *  - PlayerController: đọc phím của người chơi này (trái/phải)
 *  - CharacterStats:   chỉ số cố định của nhân vật (tốc độ, scale, máu tối đa)
 *
 * Fighter giữ vị trí (centerX, groundY) và hướng mặt, rồi mỗi frame tự quyết định
 * chạy animation nào (idle/run) dựa trên input.
 *
 * Lưu ý: stats chỉ chứa dữ liệu CỐ ĐỊNH (dùng chung giữa các Fighter cùng nhân vật).
 * Dữ liệu thay đổi trong trận (máu hiện tại, cooldown...) phải để ở Fighter,
 * không ghi vào stats.
 */
public class Fighter {

    // Thời gian mỗi frame (giây) của từng animation, số nhỏ = animation chạy nhanh hơn.
    // Lưu ý: CharacterAnimationManager cache theo (charId, action), nên giá trị
    // của lần load đầu tiên sẽ được dùng cho mọi lần sau.
    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float RUN_FRAME_DURATION = 0.08f;

    // Đối tượng lo animation và vẽ sprite
    private final Character character;

    // Nguồn input (phím) của người chơi điều khiển Fighter này
    private final PlayerController controller;

    // Chỉ số cố định của nhân vật (tốc độ chạy, scale, máu tối đa)
    private final CharacterStats stats;

    // Vị trí của nhân vật: centerX là tâm theo chiều ngang,
    // groundY là mặt đất (chân nhân vật đứng ở độ cao này)
    private float centerX;
    private float groundY;

    // Hướng mặt hiện tại. Sprite gốc quay sang phải, nên facingLeft = true thì flip ngang
    private boolean facingLeft;

    /**
     * @param animManager   manager animation dùng chung (Fighter không sở hữu, không dispose)
     * @param charId        id nhân vật, dùng để tra stats và tìm thư mục ảnh (vd "naruto_base")
     * @param controller    bộ đọc phím của người chơi này
     * @param startCenterX  tâm ngang lúc xuất hiện (pixel)
     * @param facingLeft    hướng mặt ban đầu (true = quay sang trái)
     */
    public Fighter(CharacterAnimationManager animManager, String charId,
                   PlayerController controller, float startCenterX, boolean facingLeft) {
        this.controller = controller;
        // Tra stats theo id; nếu chưa đăng ký sẽ dùng DefaultStats
        this.stats = CharacterStatsRegistry.get(charId);
        this.centerX = startCenterX;
        this.facingLeft = facingLeft;

        // Khởi tạo phần hiển thị: bắt đầu bằng idle, áp scale theo stats của nhân vật
        character = new Character(animManager, charId);
        character.setScale(stats.scale);
        character.playAnimation("idle", IDLE_FRAME_DURATION);
        character.setFlipX(facingLeft);
    }

    /**
     * Cập nhật logic mỗi frame: đọc input, di chuyển, chọn animation, giữ trong màn hình.
     *
     * @param delta   thời gian trôi qua từ frame trước (giây)
     * @param screenW chiều rộng màn hình hiện tại, dùng để chặn nhân vật đi ra ngoài
     */
    public void update(float delta, float screenW) {
        // -1 = trái, 0 = đứng yên, 1 = phải
        int dir = controller.getMoveDirection();

        if (dir != 0) {
            // Đang bấm di chuyển: quay mặt theo hướng đi, cộng quãng đường = tốc độ * thời gian
            facingLeft = dir < 0;
            centerX += dir * stats.moveSpeed * delta;
            character.playAnimation("run", RUN_FRAME_DURATION);
        } else {
            // Không bấm gì: về idle (playAnimation bỏ qua nếu đang idle rồi, nên không bị reset)
            character.playAnimation("idle", IDLE_FRAME_DURATION);
        }

        // Áp hướng mặt rồi cập nhật thời gian animation
        character.setFlipX(facingLeft);
        character.update(delta);

        // Không cho đi ra ngoài màn hình: giới hạn tâm sao cho mép sprite nằm trong [0, screenW]
        float halfW = character.getCurrentFrameWidth() / 2f;
        centerX = MathUtils.clamp(centerX, halfW, screenW - halfW);

        // Character vẽ theo góc dưới-trái, nên đổi từ tâm sang mép trái (centerX - halfW)
        character.setPosition(centerX - halfW, groundY);
    }

    /** Vẽ nhân vật. Gọi giữa batch.begin() ... batch.end(). */
    public void draw(SpriteBatch batch) { character.draw(batch); }

    /** Đặt độ cao mặt đất (lấy từ MapData), gọi lúc khởi tạo và mỗi khi đổi kích thước cửa sổ. */
    public void setGroundY(float groundY) { this.groundY = groundY; }

    /**
     * Giữ vị trí tương đối khi đổi kích thước cửa sổ:
     * nhân vật đang ở 30% chiều rộng thì sau khi resize vẫn ở 30%.
     */
    public void onResize(float oldW, float newW) {
        if (oldW > 0) centerX *= newW / oldW;
    }
}
