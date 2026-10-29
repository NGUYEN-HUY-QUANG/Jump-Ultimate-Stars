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
 *  - PlayerController: đọc phím của người chơi (di chuyển, tấn công, nhảy, lướt)
 *  - CharacterStats:   chỉ số cố định của nhân vật (tốc độ, nhảy, lướt, scale, máu, danh sách đòn Move[])
 *
 * Fighter giữ vị trí (centerX, groundY, airY), hướng mặt và một state machine:
 *
 *   FREE ──bấm J──> ATTACK ──hết animation──> RECOVERY ──hết giờ──> FREE
 *                     ^                           |
 *                     └────── bấm J (nối đòn) ────┘  (chỉ khi chưa phải đòn cuối)
 *
 *   FREE / RECOVERY / LAND ──bấm K──> AIR ──chạm đất──> LAND ──hết animation──> FREE
 *                                      |
 *                                      └── bấm K (nhảy kép, còn lượt) ──> AIR
 *
 *   FREE / RECOVERY / LAND / AIR ──bấm L──> DASH ──hết dashDuration──> FREE (hoặc AIR nếu lướt từ trên không)
 *
 * Logic đòn đánh (frame nào gây dame, khi nào nối combo) dựa trên stateTime của Fighter,
 * KHÔNG đọc từ animation, nên đổi sprite không ảnh hưởng gameplay.
 */
public class Fighter {

    // ===================== HẰNG SỐ =====================

    // Thời gian mỗi frame (giây) của idle/run. Số nhỏ = chạy nhanh hơn.
    // Lưu ý: animManager cache theo (charId, action), lần load đầu quyết định giá trị.
    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float RUN_FRAME_DURATION = 0.08f;

    // Thời gian mỗi frame (giây) của các animation nhảy / rơi / chạm đất
    private static final float JUMP_FRAME_DURATION = 0.08f;
    private static final float FALL_FRAME_DURATION = 0.08f;
    private static final float LAND_FRAME_DURATION = 0.06f;

    // Chỉ dùng để preload và đếm số frame của dash. Thời gian chạy thật của animation dash
    // được tính lại từ stats.dashDuration trong startDash().
    private static final float DASH_FRAME_DURATION = 0.05f;

    // ===================== STATE MACHINE =====================

    // FREE     = tự do di chuyển, sẵn sàng đánh / nhảy / lướt
    // ATTACK   = đang chạy animation của 1 đòn, không di chuyển được
    // RECOVERY = animation đòn đã xong, đang nghỉ: vẫn đi lại / nhảy / lướt được,
    //            nhưng đánh tiếp chỉ được nếu chưa phải đòn cuối
    // AIR      = đang trên không (bay lên hoặc rơi): đi ngang, nhảy kép, lướt được; chưa đánh được
    // LAND     = animation chạm đất, không khóa: đánh / nhảy / lướt / đi đều hủy được ngay
    // DASH     = đang lướt: khóa cứng, không đọc input cho đến khi hết dashDuration
    private enum State { FREE, ATTACK, RECOVERY, AIR, LAND, DASH }

    // ===================== CÁC THÀNH PHẦN =====================

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

    // ===================== VỊ TRÍ & HƯỚNG MẶT =====================

    // Vị trí: centerX là tâm ngang, groundY là mặt đất (chân nhân vật)
    private float centerX;
    private float groundY;

    // Hướng mặt. Sprite gốc quay phải, facingLeft = true thì lật ngang
    private boolean facingLeft;

    // ===================== TRẠNG THÁI ĐÁNH =====================

    private State state = State.FREE;
    private Move move;            // đòn đang chạy
    private int comboIndex;       // chỉ số đòn đang chạy trong mảng moves (đếm từ 0)
    private int frameCount;       // số frame của đòn đang chạy
    private float stateTime;      // thời gian đã trôi qua kể từ khi đòn bắt đầu (giây)
    private float recoveryTimer;  // thời gian nghỉ còn lại ở RECOVERY (giây)
    private boolean buffered;     // đã bấm J trong lúc đang đánh -> tự nối đòn kế khi hết animation
    private boolean hitDone;      // đòn hiện tại đã gây dame chưa (đảm bảo chỉ gây 1 lần)

    // ===================== TRẠNG THÁI NHẢY / CHẠM ĐẤT =====================

    private float airY;       // độ cao so với mặt đất (0 = đang đứng đất). Vị trí vẽ = groundY + airY
    private float velY;       // vận tốc dọc (px/s), dương = đang bay lên
    private int jumpsUsed;    // số lần đã nhảy từ lần chạm đất gần nhất (so với stats.maxJumps)
    private float landTimer;  // thời gian còn lại của animation chạm đất (giây)

    // ===================== TRẠNG THÁI LƯỚT =====================

    private float dashTimer;      // thời gian lướt còn lại (giây)
    private float dashSpeed;      // vận tốc lướt (px/s) = dashDistance / dashDuration
    private int dashDir;          // hướng lướt: -1 trái, 1 phải
    private float dashCooldown;   // thời gian nghỉ còn lại trước khi lướt tiếp được (giây)
    private boolean dashInAir;    // lần lướt này bắt đầu từ trên không -> lướt xong quay lại AIR

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

        // Load sẵn animation nhảy/rơi/chạm đất/lướt vì cùng lý do cache ở trên.
        // jump, land, dash chạy 1 lần (NORMAL); fall lặp (LOOP) cho tới khi chạm đất.
        // Thiếu asset (jump_*, fall_*, land_*, dash_*) của nhân vật nào thì lỗi ngay lúc vào trận.
        animManager.get(charId, "jump", JUMP_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "fall", FALL_FRAME_DURATION, Animation.PlayMode.LOOP);
        animManager.get(charId, "land", LAND_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "dash", DASH_FRAME_DURATION, Animation.PlayMode.NORMAL);

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
        // Đếm ngược thời gian nghỉ giữa 2 lần lướt (chạy ở mọi state)
        if (dashCooldown > 0f) dashCooldown -= delta;

        // Mỗi phím chỉ đọc 1 lần mỗi frame (true đúng 1 frame khi vừa bấm) rồi truyền xuống các hàm con.
        // dash còn bị chặn khi đang cooldown, nên mọi state đều tự áp dụng cooldown mà không cần kiểm tra lại.
        boolean atk = controller.isAttackPressed();
        boolean jump = controller.isJumpPressed();
        boolean dash = controller.isDashPressed() && dashCooldown <= 0f;

        switch (state) {
            case FREE:
                // Thứ tự ưu tiên khi bấm cùng frame: lướt > nhảy > đánh > đi
                if (dash) startDash();
                else if (jump) startJump();
                else if (atk && moves.length > 0) startAttack(0);   // bắt đầu chuỗi từ đòn đầu
                else updateMovement(delta);
                break;
            case ATTACK:
                updateAttack(delta, atk);
                break;
            case RECOVERY:
                updateRecovery(delta, atk, jump, dash);
                break;
            case AIR:
                updateAir(delta, jump, dash);
                break;
            case LAND:
                updateLand(delta, atk, jump, dash);
                break;
            case DASH:
                updateDash(delta);   // không truyền input: đang lướt là khóa cứng
                break;
        }

        // Áp hướng mặt rồi cập nhật thời gian animation
        character.setFlipX(facingLeft);
        character.update(delta);

        // Chặn ra ngoài màn hình theo bề rộng neo (cố định), không theo frame hiện tại.
        // Lướt chạm mép thì bị chặn ở đây nên quãng đường thực tế có thể ngắn hơn dashDistance.
        float halfW = character.getRefWidth() / 2f;
        centerX = MathUtils.clamp(centerX, halfW, screenW - halfW);

        // Vị trí vẽ = mặt đất + độ cao đang nhảy (airY = 0 khi đứng đất)
        character.setCenter(centerX, groundY + airY);
    }

    // ===================== DI CHUYỂN =====================

    /**
     * Đọc phím trái/phải, cập nhật hướng mặt và vị trí ngang. Không chọn animation.
     * Dùng chung cho đi dưới đất (updateMovement) và đi ngang trên không (updateAir).
     *
     * @return hướng đang bấm: -1 trái, 0 đứng yên, 1 phải
     */
    private int moveHorizontal(float delta) {
        int dir = controller.getMoveDirection();
        if (dir != 0) {
            facingLeft = dir < 0;
            centerX += dir * stats.moveSpeed * delta;
        }
        return dir;
    }

    /** Di chuyển trái/phải và chọn animation idle/run. Dùng ở FREE, RECOVERY và LAND. */
    private void updateMovement(float delta) {
        int dir = moveHorizontal(delta);
        character.playAnimation(dir != 0 ? "run" : "idle",
            dir != 0 ? RUN_FRAME_DURATION : IDLE_FRAME_DURATION);
    }

    // ===================== ĐÁNH THƯỜNG =====================

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
     * Đang ra đòn thì không nhảy / lướt / di chuyển được.
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
     *  - Lướt hoặc nhảy: hủy recovery ngay, reset combo về đòn đầu.
     *  - Chưa phải đòn cuối: bấm J trong lúc nghỉ thì nối đòn kế.
     *  - Đòn cuối: bấm J bị bỏ qua (khóa), để đối thủ có thời gian phản ứng.
     * Hết giờ thì reset chuỗi về đòn đầu và về FREE.
     */
    private void updateRecovery(float delta, boolean atk, boolean jump, boolean dash) {
        boolean isLast = comboIndex == moves.length - 1;
        recoveryTimer -= delta;

        if (dash) {
            comboIndex = 0;     // hủy recovery bằng lướt: tính như hết chuỗi combo
            startDash();
            return;
        }
        if (jump) {
            comboIndex = 0;     // hủy recovery bằng nhảy: tính như hết chuỗi combo
            startJump();
            return;
        }
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

    // ===================== NHẢY / RƠI / CHẠM ĐẤT =====================

    /**
     * Bắt đầu 1 lần nhảy (lần 1 từ đất, lần 2 từ trên không): tăng đếm lượt nhảy,
     * đặt vận tốc hướng lên và phát animation jump từ đầu.
     */
    private void startJump() {
        jumpsUsed++;
        velY = stats.jumpSpeed;
        state = State.AIR;
        character.playFromStart("jump", JUMP_FRAME_DURATION);
    }

    /**
     * Đang trên không (bay lên hoặc rơi): lướt được, nhảy tiếp nếu còn lượt, đi ngang được,
     * chịu trọng lực. Chưa đánh được trên không. Chạm đất thì reset lượt nhảy và sang LAND.
     */
    private void updateAir(float delta, boolean jump, boolean dash) {
        if (dash) { startDash(); return; }                       // lướt ưu tiên hơn nhảy
        if (jump && jumpsUsed < stats.maxJumps) startJump();     // nhảy kép

        moveHorizontal(delta);

        // Trọng lực: giảm vận tốc dọc rồi cộng vào độ cao
        velY -= stats.gravity * delta;
        airY += velY * delta;

        // Chạm đất
        if (airY <= 0f) {
            airY = 0f;
            velY = 0f;
            jumpsUsed = 0;
            startLand();
            return;
        }

        // Đang rơi (vận tốc dọc <= 0) thì chuyển sang animation fall.
        // playAnimation không restart nếu đang chạy sẵn fall nên gọi mỗi frame được.
        if (velY <= 0f) character.playAnimation("fall", FALL_FRAME_DURATION);
    }

    /** Chạm đất: phát animation land một lần, đặt timer bằng đúng độ dài animation. */
    private void startLand() {
        int frames = animManager.get(charId, "land", LAND_FRAME_DURATION).getKeyFrames().length;
        landTimer = frames * LAND_FRAME_DURATION;
        state = State.LAND;
        character.playFromStart("land", LAND_FRAME_DURATION);
    }

    /**
     * Đang animation chạm đất. Không khóa người chơi: lướt, nhảy, đánh hoặc di chuyển
     * đều hủy animation ngay. Đứng yên thì chạy hết animation rồi về FREE.
     */
    private void updateLand(float delta, boolean atk, boolean jump, boolean dash) {
        if (dash) { startDash(); return; }
        if (jump) { startJump(); return; }
        if (atk && moves.length > 0) { startAttack(0); return; }

        if (controller.getMoveDirection() != 0) {
            state = State.FREE;
            updateMovement(delta);   // đi ngay, chuyển sang animation run
            return;
        }

        landTimer -= delta;
        if (landTimer <= 0f) state = State.FREE;
    }

    // ===================== LƯỚT (DASH) =====================

    /**
     * Nút né: lướt theo hướng mặt hiện tại (không đọc phím hướng), dashDistance px trong
     * dashDuration giây. Animation dash được giãn để chạy vừa khít dashDuration.
     * Lướt từ trên không thì giữ nguyên độ cao, hủy đà nhảy/rơi trong lúc lướt.
     */
    private void startDash() {
        if (state == State.DASH) return;    // phòng hờ bấm chồng khi đang lướt
        dashInAir = (state == State.AIR);   // phải đọc TRƯỚC khi đổi state
        if (dashInAir) velY = 0f;           // hủy đà nhảy/rơi, lơ lửng trong lúc lướt

        dashDir = facingLeft ? -1 : 1;
        dashTimer = stats.dashDuration;
        dashSpeed = stats.dashDistance / stats.dashDuration;

        // Thời gian mỗi frame = dashDuration / số frame, để animation dài đúng bằng thời gian lướt
        int frames = animManager.get(charId, "dash", DASH_FRAME_DURATION).getKeyFrames().length;
        float frameDuration = stats.dashDuration / frames;

        state = State.DASH;
        character.playFromStart("dash", frameDuration);
    }

    /**
     * Đang lướt: khóa cứng, không đọc input, tự di chuyển theo dashDir.
     * Hết dashDuration thì bắt đầu cooldown, rồi về AIR (nếu lướt từ trên không, sẽ rơi tiếp
     * từ vận tốc 0) hoặc FREE (nếu lướt dưới đất).
     */
    private void updateDash(float delta) {
        centerX += dashDir * dashSpeed * delta;   // airY không đổi, không có trọng lực trong lúc lướt
        dashTimer -= delta;
        if (dashTimer <= 0f) {
            dashCooldown = stats.dashRecovery;    // cooldown bắt đầu đếm từ lúc lướt XONG
            state = dashInAir ? State.AIR : State.FREE;
        }
    }

    // ===================== GÂY DAME =====================

    /**
     * Được gọi đúng 1 lần khi đòn tới frame gây dame.
     * TODO: tạo hitbox, kiểm tra overlap với hurtbox đối thủ, trừ máu m.damage.
     * Nhớ tính vị trí hitbox theo groundY + airY, không chỉ groundY.
     */
    private void doHit(Move m) {
    }

    // ===================== API CHO GAMESCREEN =====================

    /** Vẽ nhân vật. Gọi giữa batch.begin() ... batch.end(). */
    public void draw(SpriteBatch batch) { character.draw(batch); }

    /** Đặt độ cao mặt đất (lấy từ MapData), gọi lúc khởi tạo và mỗi khi đổi kích thước cửa sổ. */
    public void setGroundY(float groundY) { this.groundY = groundY; }

    /** Giữ vị trí tương đối khi đổi kích thước cửa sổ (đang ở 30% chiều rộng thì vẫn ở 30%). */
    public void onResize(float oldW, float newW) { if (oldW > 0) centerX *= newW / oldW; }
}
