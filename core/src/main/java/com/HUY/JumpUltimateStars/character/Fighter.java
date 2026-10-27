package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.CharacterStatsRegistry;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.hethong.PlayerController;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Nhân vật đang chiến đấu trong GameScreen.
 *
 * Ghép các thành phần lại với nhau:
 *  - Character:        lo animation + vẽ sprite (không biết gì về input hay đòn đánh)
 *  - PlayerController: đọc phím của người chơi (di chuyển, tấn công)
 *  - CharacterStats:   chỉ số cố định của nhân vật (tốc độ, scale, máu, danh sách đòn Move[])
 *
 * Fighter giữ vị trí (centerX, groundY), hướng mặt và một state machine nhỏ:
 *
 *   FREE ──bấm J──> ATTACK ──hết animation──> RECOVERY ──hết giờ──> FREE
 *                     ^                           |
 *                     └────── bấm J (nối đòn) ────┘  (chỉ khi chưa phải đòn cuối)
 *
 * Logic đòn đánh (frame nào gây dame, khi nào nối combo) dựa trên stateTime của Fighter,
 * KHÔNG đọc từ animation, nên đổi sprite không ảnh hưởng gameplay.
 */
public class Fighter {

    // Thời gian mỗi frame (giây) của idle/run. Số nhỏ = chạy nhanh hơn.
    // Lưu ý: animManager cache theo (charId, action), lần load đầu quyết định giá trị.
    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float RUN_FRAME_DURATION = 0.08f;

    // FREE     = tự do di chuyển, sẵn sàng đánh
    // ATTACK   = đang chạy animation của 1 đòn, không di chuyển được
    // RECOVERY = animation đòn đã xong, đang nghỉ: vẫn đi lại được,
    //            nhưng đánh tiếp chỉ được nếu chưa phải đòn cuối
    private enum State { FREE, ATTACK, RECOVERY }

    // Phần hiển thị (animation + vẽ)
    private final Character character;

    // Nguồn input của người chơi điều khiển Fighter này
    private final PlayerController controller;

    // Chỉ số cố định, dùng chung giữa các Fighter cùng nhân vật (không ghi dữ liệu trong trận vào đây)
    private final CharacterStats stats;

    // Dùng để lấy animation đòn đánh và đếm số frame
    private final CharacterAnimationManager animManager;
    private final String charId;

    // Danh sách đòn thường theo thứ tự combo (phần tử cuối là đòn kết)
    private final Move[] moves;

    // Vị trí: centerX là tâm ngang, groundY là mặt đất (chân nhân vật)
    private float centerX;
    private float groundY;

    // Hướng mặt. Sprite gốc quay phải, facingLeft = true thì lật ngang
    private boolean facingLeft;

    // --- Trạng thái đánh ---
    private State state = State.FREE;
    private Move move;            // đòn đang chạy
    private int comboIndex;       // chỉ số đòn đang chạy trong mảng moves (đếm từ 0)
    private int frameCount;       // số frame của đòn đang chạy
    private float stateTime;      // thời gian đã trôi qua kể từ khi đòn bắt đầu (giây)
    private float recoveryTimer;  // thời gian nghỉ còn lại ở RECOVERY (giây)
    private boolean buffered;     // đã bấm J trong lúc đang đánh -> tự nối đòn kế khi hết animation
    private boolean hitDone;      // đòn hiện tại đã gây dame chưa (đảm bảo chỉ gây 1 lần)

    /**
     * @param animManager   manager animation dùng chung (Fighter không sở hữu, không dispose)
     * @param charId        id nhân vật, dùng để tra stats và tìm thư mục ảnh
     * @param controller    bộ đọc phím của người chơi này
     * @param startCenterX  tâm ngang lúc xuất hiện (pixel)
     * @param facingLeft    hướng mặt ban đầu
     */
    public Fighter(CharacterAnimationManager animManager, String charId,
                   PlayerController controller, float startCenterX, boolean facingLeft) {
        this.animManager = animManager;
        this.charId = charId;
        this.controller = controller;
        this.stats = CharacterStatsRegistry.get(charId);
        this.moves = stats.moves;
        this.centerX = startCenterX;
        this.facingLeft = facingLeft;

        // Load sẵn animation các đòn với NORMAL (chạy 1 lần, không lặp).
        // Manager cache theo lần load đầu nên PHẢI load kiểu này trước mọi chỗ khác,
        // nếu không đòn đánh sẽ bị lặp.
        for (Move m : moves) {
            animManager.get(charId, m.action, m.frameDuration, Animation.PlayMode.NORMAL);
        }

        character = new Character(animManager, charId);
        character.setScale(stats.scale);
        character.playAnimation("idle", IDLE_FRAME_DURATION);
        // Bề rộng neo = bề rộng frame idle. Mọi frame khác (kể cả frame có effect dài ra)
        // đều vẽ từ mép trái của ô neo này, nên thân nhân vật không bị lệch.
        character.setRefWidth(character.getCurrentFrameWidth());
        character.setFlipX(facingLeft);
    }

    /**
     * Cập nhật mỗi frame: đọc input, chạy state machine, giữ nhân vật trong màn hình.
     *
     * @param delta   thời gian trôi qua từ frame trước (giây)
     * @param screenW chiều rộng màn hình, dùng để chặn đi ra ngoài
     */
    public void update(float delta, float screenW) {
        // true đúng 1 frame khi vừa bấm. Chỉ đọc 1 lần mỗi frame rồi truyền xuống các hàm con.
        boolean atk = controller.isAttackPressed();

        switch (state) {
            case FREE:
                if (atk && moves.length > 0) startAttack(0);   // bắt đầu chuỗi từ đòn đầu
                else updateMovement(delta);
                break;
            case ATTACK:
                updateAttack(delta, atk);
                break;
            case RECOVERY:
                updateRecovery(delta, atk);
                break;
        }

        // Áp hướng mặt rồi cập nhật thời gian animation
        character.setFlipX(facingLeft);
        character.update(delta);

        // Chặn ra ngoài màn hình theo bề rộng neo (cố định), không theo frame hiện tại
        float halfW = character.getRefWidth() / 2f;
        centerX = MathUtils.clamp(centerX, halfW, screenW - halfW);
        character.setCenter(centerX, groundY);
    }

    /** Di chuyển trái/phải và chọn animation idle/run. Dùng ở FREE và RECOVERY. */
    private void updateMovement(float delta) {
        int dir = controller.getMoveDirection(); // -1 trái, 0 đứng, 1 phải
        if (dir != 0) {
            facingLeft = dir < 0;
            centerX += dir * stats.moveSpeed * delta;
            character.playAnimation("run", RUN_FRAME_DURATION);
        } else {
            character.playAnimation("idle", IDLE_FRAME_DURATION);
        }
    }

    /**
     * Bắt đầu đòn thứ index trong mảng moves: reset bộ đếm và phát animation từ frame đầu.
     * Dùng playFromStart (không phải playAnimation) để đánh lại cùng 1 đòn vẫn chạy từ đầu.
     */
    private void startAttack(int index) {
        comboIndex = index;
        move = moves[index];
        frameCount = animManager.get(charId, move.action, move.frameDuration).getKeyFrames().length;
        stateTime = 0f;
        buffered = false;
        hitDone = false;
        state = State.ATTACK;
        character.playFromStart(move.action, move.frameDuration);
    }

    /**
     * Đang chạy 1 đòn: đếm thời gian, gây dame ở frame chỉ định, và khi animation hết
     * thì nối đòn kế (nếu đã bấm J trong lúc đánh) hoặc chuyển sang RECOVERY.
     */
    private void updateAttack(float delta, boolean atk) {
        if (atk) buffered = true;   // spam J lúc đang đánh thì nhớ lại để nối đòn
        stateTime += delta;

        // Frame đang hiển thị theo thời gian (chặn ở frame cuối)
        int frame = Math.min((int) (stateTime / move.frameDuration), frameCount - 1);

        // Tới frame gây dame thì gọi doHit đúng 1 lần nhờ cờ hitDone
        if (!hitDone && frame >= move.hitFrame) {
            hitDone = true;
            doHit(move);
        }

        // Hết animation của đòn
        if (stateTime >= frameCount * move.frameDuration) {
            boolean isLast = comboIndex == moves.length - 1;
            if (buffered && !isLast) {
                startAttack(comboIndex + 1);          // nối thẳng sang đòn kế
            } else {
                state = State.RECOVERY;               // nghỉ: cửa sổ nối đòn hoặc khóa input
                recoveryTimer = move.recovery;
            }
        }
    }

    /**
     * Nghỉ sau đòn. Vẫn đi lại được.
     *  - Chưa phải đòn cuối: bấm J trong lúc nghỉ thì nối đòn kế.
     *  - Đòn cuối: bấm J bị bỏ qua (khóa), để đối thủ có thời gian phản ứng.
     * Hết giờ thì reset chuỗi về đòn đầu và về FREE.
     */
    private void updateRecovery(float delta, boolean atk) {
        boolean isLast = comboIndex == moves.length - 1;
        recoveryTimer -= delta;

        if (atk && !isLast) {
            startAttack(comboIndex + 1);   // nối đòn trong cửa sổ recovery
            return;                        // đã chuyển sang ATTACK, không di chuyển nữa
        }

        updateMovement(delta);             // vẫn đi lại / về idle trong lúc nghỉ

        if (recoveryTimer <= 0f) {
            comboIndex = 0;                // hết hạn: reset chuỗi
            state = State.FREE;
        }
    }

    /**
     * Được gọi đúng 1 lần khi đòn tới frame gây dame.
     * TODO: tạo hitbox, kiểm tra overlap với hurtbox đối thủ, trừ máu m.damage.
     */
    private void doHit(Move m) {
    }

    /** Vẽ nhân vật. Gọi giữa batch.begin() ... batch.end(). */
    public void draw(SpriteBatch batch) { character.draw(batch); }

    /** Đặt độ cao mặt đất (lấy từ MapData), gọi lúc khởi tạo và mỗi khi đổi kích thước cửa sổ. */
    public void setGroundY(float groundY) { this.groundY = groundY; }

    /** Giữ vị trí tương đối khi đổi kích thước cửa sổ (đang ở 30% chiều rộng thì vẫn ở 30%). */
    public void onResize(float oldW, float newW) { if (oldW > 0) centerX *= newW / oldW; }
}
