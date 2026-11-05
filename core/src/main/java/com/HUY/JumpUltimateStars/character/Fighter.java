package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.CharacterStatsRegistry;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;
import com.HUY.JumpUltimateStars.effect.Effect;
import com.HUY.JumpUltimateStars.hethong.PlayerController;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Nhân vật đang chiến đấu trong GameScreen.
 *
 * Ghép các thành phần:
 *  - Character:        animation + vẽ sprite
 *  - PlayerController: đọc phím
 *  - CharacterStats:   chỉ số cố định (tốc độ, nhảy, lướt, đòn đánh, skill)
 *  - Effect:           Fighter tự giữ danh sách effect do mình sinh ra (phi tiêu...).
 *                      Effect tự bay, tự vẽ, tự tính dame; Fighter chỉ gọi ra đúng frame
 *                      và cập nhật/vẽ chúng.
 *
 * State machine:
 *
 *   FREE ──bấm J──> ATTACK ──hết animation──> RECOVERY ──hết giờ──> FREE
 *                     ^                           |
 *                     └────── bấm J (nối đòn) ────┘  (chỉ khi chưa phải đòn cuối)
 *
 *   FREE / RECOVERY / LAND / ATTACK(sau frame gây dame + vài frame) ──bấm U──> SKILL ──hết──> FREE
 *   FREE / RECOVERY / LAND ──bấm K──> AIR ──chạm đất──> LAND ──hết──> FREE
 *   FREE / RECOVERY / LAND / AIR ──bấm L──> DASH ──hết──> FREE (hoặc AIR nếu lướt trên không)
 */
public class Fighter {

    // ===================== HẰNG SỐ =====================

    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float RUN_FRAME_DURATION = 0.08f;
    private static final float JUMP_FRAME_DURATION = 0.08f;
    private static final float FALL_FRAME_DURATION = 0.08f;
    private static final float LAND_FRAME_DURATION = 0.06f;
    private static final float DASH_FRAME_DURATION = 0.05f;   // chỉ để preload, thời gian thật tính ở startDash()

    // Skill ngắt được đòn thường sau frame gây dame bao nhiêu frame (0 = ngắt ngay)
    private static final int SKILL_CANCEL_DELAY_FRAMES = 3;

    // FREE: tự do | ATTACK: đang ra đòn | RECOVERY: nghỉ sau đòn | AIR: trên không
    // LAND: chạm đất (không khóa) | DASH: lướt (khóa cứng) | SKILL: tung skill (khóa cứng)
    private enum State { FREE, ATTACK, RECOVERY, AIR, LAND, DASH, SKILL }

    // ===================== THÀNH PHẦN =====================

    private final Character character;
    private final PlayerController controller;
    private final CharacterStats stats;
    private final CharacterAnimationManager animManager;   // dùng chung, GameScreen sở hữu
    private final String charId;
    private final Move[] moves;

    // Effect do Fighter này sinh ra. Fighter sở hữu, tự dispose khi xong
    private final Array<Effect> effects = new Array<>();

    // ===================== VỊ TRÍ & HƯỚNG MẶT =====================

    private float centerX;
    private float groundY;
    private boolean facingLeft;

    private State state = State.FREE;

    // ===================== ĐÁNH THƯỜNG =====================

    private Move move;
    private int comboIndex;
    private int frameCount;
    private float stateTime;
    private float recoveryTimer;
    private boolean buffered;       // đã bấm J lúc đang đánh -> nối đòn kế
    private boolean skillBuffered;  // đã bấm U lúc đang đánh -> ra skill khi tới frame hủy
    private boolean hitDone;

    // ===================== SKILL =====================

    private Skill skill;
    private int skillFrameCount;
    private float skillTime;
    private int skillNextHit;       // hit cận chiến kế tiếp chưa gây dame
    private boolean skillSpawned;   // đã gọi effect chưa
    private float skillCooldown;

    // ===================== NHẢY / LƯỚT =====================

    private float airY;             // độ cao so với mặt đất
    private float velY;
    private int jumpsUsed;
    private float landTimer;

    private float dashTimer;
    private float dashSpeed;
    private int dashDir;
    private float dashCooldown;
    private boolean dashInAir;

    /**
     * @param animManager  cache animation dùng chung (không dispose ở đây)
     * @param charId       id nhân vật
     * @param controller   bộ đọc phím của người chơi này
     * @param startCenterX tâm ngang lúc xuất hiện
     * @param facingLeft   hướng mặt ban đầu
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

        // Load sẵn animation đòn đánh kiểu NORMAL (chạy 1 lần). Phải làm TRƯỚC mọi chỗ khác
        // vì manager cache theo lần load đầu, nếu không đòn đánh sẽ bị lặp.
        for (Move m : moves) {
            animManager.get(charId, m.action, m.frameDuration, Animation.PlayMode.NORMAL);
        }

        character = new Character(animManager, charId);
        character.setScale(stats.scale);
        character.playAnimation("idle", IDLE_FRAME_DURATION);

        // Load sẵn nhảy / rơi / chạm đất / lướt / skill (thiếu asset sẽ lỗi ngay lúc vào trận)
        animManager.get(charId, "jump", JUMP_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "fall", FALL_FRAME_DURATION, Animation.PlayMode.LOOP);
        animManager.get(charId, "land", LAND_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "dash", DASH_FRAME_DURATION, Animation.PlayMode.NORMAL);
        if (stats.skillU != null) {
            animManager.get(charId, stats.skillU.action, stats.skillU.frameDuration, Animation.PlayMode.NORMAL);
        }

        // Ô neo = bề rộng frame idle. Mọi frame khác vẽ từ mép trái ô này nên thân không bị lệch
        character.setRefWidth(character.getCurrentFrameWidth());
        character.setFlipX(facingLeft);
    }

    // ===================== UPDATE =====================

    /**
     * Mỗi frame: đọc input, chạy state machine, cập nhật effect, giữ nhân vật trong màn hình.
     *
     * @param delta   giây từ frame trước
     * @param screenW chiều rộng màn hình
     */
    public void update(float delta, float screenW) {
        if (dashCooldown > 0f) dashCooldown -= delta;
        if (skillCooldown > 0f) skillCooldown -= delta;

        // Mỗi phím đọc 1 lần mỗi frame. dash và skillU đã gồm điều kiện cooldown
        boolean atk = controller.isAttackPressed();
        boolean jump = controller.isJumpPressed();
        boolean dash = controller.isDashPressed() && dashCooldown <= 0f;
        boolean skillU = controller.isSkillUPressed() && stats.skillU != null && skillCooldown <= 0f;

        switch (state) {
            case FREE:
                // Ưu tiên khi bấm cùng lúc: lướt > nhảy > skill > đánh > đi
                if (dash) startDash();
                else if (jump) startJump();
                else if (skillU) startSkill(stats.skillU);
                else if (atk && moves.length > 0) startAttack(0);
                else updateMovement(delta);
                break;
            case ATTACK:   updateAttack(delta, atk, skillU); break;
            case RECOVERY: updateRecovery(delta, atk, jump, dash, skillU); break;
            case AIR:      updateAir(delta, jump, dash); break;
            case LAND:     updateLand(delta, atk, jump, dash, skillU); break;
            case DASH:     updateDash(delta); break;
            case SKILL:    updateSkill(delta); break;
        }

        updateEffects(delta);

        character.setFlipX(facingLeft);
        character.update(delta);

        float halfW = character.getRefWidth() / 2f;
        centerX = MathUtils.clamp(centerX, halfW, screenW - halfW);
        character.setCenter(centerX, groundY + airY);
    }

    /** Effect luôn chạy tiếp kể cả khi skill đã xong (phi tiêu vẫn bay). Dọn cái đã hết. */
    private void updateEffects(float delta) {
        for (int i = effects.size - 1; i >= 0; i--) {
            Effect e = effects.get(i);
            e.update(delta);
            if (!e.isActive()) effects.removeIndex(i);
        }
    }

    // ===================== DI CHUYỂN =====================

    /** Đọc phím trái/phải, đổi hướng mặt và vị trí. Trả về hướng: -1, 0, 1. */
    private int moveHorizontal(float delta) {
        int dir = controller.getMoveDirection();
        if (dir != 0) {
            facingLeft = dir < 0;
            centerX += dir * stats.moveSpeed * delta;
        }
        return dir;
    }

    /** Đi dưới đất + chọn animation idle/run. */
    private void updateMovement(float delta) {
        int dir = moveHorizontal(delta);
        character.playAnimation(dir != 0 ? "run" : "idle",
            dir != 0 ? RUN_FRAME_DURATION : IDLE_FRAME_DURATION);
    }

    // ===================== ĐÁNH THƯỜNG =====================

    private void startAttack(int index) {
        comboIndex = index;
        move = moves[index];
        frameCount = animManager.get(charId, move.action, move.frameDuration).getKeyFrames().length;
        stateTime = 0f;
        buffered = false;
        skillBuffered = false;
        hitDone = false;
        state = State.ATTACK;
        character.playFromStart(move.action, move.frameDuration);
    }

    /**
     * Đang ra đòn: gây dame ở hitFrame. Bấm J thì nhớ để nối đòn, bấm U thì nhớ để ngắt sang skill
     * (ngắt được khi đã qua hitFrame + SKILL_CANCEL_DELAY_FRAMES frame).
     */
    private void updateAttack(float delta, boolean atk, boolean skillU) {
        if (atk) buffered = true;
        if (skillU) skillBuffered = true;
        stateTime += delta;

        int frame = Math.min((int) (stateTime / move.frameDuration), frameCount - 1);

        if (!hitDone && frame >= move.hitFrame) {
            hitDone = true;
            doHit(move);
        }

        int cancelFrame = Math.min(move.hitFrame + SKILL_CANCEL_DELAY_FRAMES, frameCount - 1);
        if (skillBuffered && frame >= cancelFrame) {
            comboIndex = 0;
            startSkill(stats.skillU);
            return;
        }

        if (stateTime >= frameCount * move.frameDuration) {
            boolean isLast = comboIndex == moves.length - 1;
            if (buffered && !isLast) {
                startAttack(comboIndex + 1);
            } else {
                state = State.RECOVERY;
                recoveryTimer = move.recovery;
            }
        }
    }

    /** Nghỉ sau đòn. Lướt / nhảy / skill hủy ngay; bấm J nối đòn nếu chưa phải đòn cuối. */
    private void updateRecovery(float delta, boolean atk, boolean jump, boolean dash, boolean skillU) {
        boolean isLast = comboIndex == moves.length - 1;
        recoveryTimer -= delta;

        if (dash)   { comboIndex = 0; startDash(); return; }
        if (jump)   { comboIndex = 0; startJump(); return; }
        if (skillU) { comboIndex = 0; startSkill(stats.skillU); return; }
        if (atk && !isLast) { startAttack(comboIndex + 1); return; }

        updateMovement(delta);

        if (recoveryTimer <= 0f) {
            comboIndex = 0;
            state = State.FREE;
        }
    }

    // ===================== SKILL U =====================

    private void startSkill(Skill s) {
        skill = s;
        skillFrameCount = animManager.get(charId, s.action, s.frameDuration).getKeyFrames().length;
        skillTime = 0f;
        skillNextHit = 0;
        skillSpawned = false;
        skillBuffered = false;
        state = State.SKILL;
        character.playFromStart(s.action, s.frameDuration);
    }

    /**
     * Đang tung skill (khóa cứng). Tới hitFrames thì gây dame, tới spawnFrame thì gọi effect ra.
     * Hết animation thì cooldown rồi về FREE.
     */
    private void updateSkill(float delta) {
        skillTime += delta;
        int frame = Math.min((int) (skillTime / skill.frameDuration), skillFrameCount - 1);

        // while vì lag lớn có thể vượt nhiều hitFrames cùng lúc
        while (skillNextHit < skill.hitFrames.length && frame >= skill.hitFrames[skillNextHit]) {
            doSkillHit(skill, skillNextHit);
            skillNextHit++;
        }

        // Gọi effect ra đúng 1 lần
        if (!skillSpawned && skill.effect != null && frame >= skill.spawnFrame) {
            skillSpawned = true;
            int dir = facingLeft ? -1 : 1;
            effects.add(skill.effect.create(centerX, groundY + airY, dir));
        }

        if (skillTime >= skillFrameCount * skill.frameDuration) {
            skillCooldown = skill.cooldown;
            comboIndex = 0;
            state = State.FREE;
        }
    }

    // ===================== NHẢY / RƠI / CHẠM ĐẤT =====================

    private void startJump() {
        jumpsUsed++;
        velY = stats.jumpSpeed;
        state = State.AIR;
        character.playFromStart("jump", JUMP_FRAME_DURATION);
    }

    /** Trên không: lướt, nhảy kép, đi ngang, chịu trọng lực. Chạm đất thì sang LAND. */
    private void updateAir(float delta, boolean jump, boolean dash) {
        if (dash) { startDash(); return; }
        if (jump && jumpsUsed < stats.maxJumps) startJump();

        moveHorizontal(delta);

        velY -= stats.gravity * delta;
        airY += velY * delta;

        if (airY <= 0f) {
            airY = 0f;
            velY = 0f;
            jumpsUsed = 0;
            startLand();
            return;
        }

        if (velY <= 0f) character.playAnimation("fall", FALL_FRAME_DURATION);
    }

    private void startLand() {
        int frames = animManager.get(charId, "land", LAND_FRAME_DURATION).getKeyFrames().length;
        landTimer = frames * LAND_FRAME_DURATION;
        state = State.LAND;
        character.playFromStart("land", LAND_FRAME_DURATION);
    }

    /** Chạm đất không khóa: lướt / nhảy / skill / đánh / đi đều hủy được ngay. */
    private void updateLand(float delta, boolean atk, boolean jump, boolean dash, boolean skillU) {
        if (dash) { startDash(); return; }
        if (jump) { startJump(); return; }
        if (skillU) { startSkill(stats.skillU); return; }
        if (atk && moves.length > 0) { startAttack(0); return; }

        if (controller.getMoveDirection() != 0) {
            state = State.FREE;
            updateMovement(delta);
            return;
        }

        landTimer -= delta;
        if (landTimer <= 0f) state = State.FREE;
    }

    // ===================== LƯỚT =====================

    private void startDash() {
        if (state == State.DASH) return;
        dashInAir = (state == State.AIR);   // phải đọc TRƯỚC khi đổi state
        if (dashInAir) velY = 0f;

        dashDir = facingLeft ? -1 : 1;
        dashTimer = stats.dashDuration;
        dashSpeed = stats.dashDistance / stats.dashDuration;

        // Giãn animation cho vừa khít thời gian lướt
        int frames = animManager.get(charId, "dash", DASH_FRAME_DURATION).getKeyFrames().length;
        state = State.DASH;
        character.playFromStart("dash", stats.dashDuration / frames);
    }

    private void updateDash(float delta) {
        centerX += dashDir * dashSpeed * delta;
        dashTimer -= delta;
        if (dashTimer <= 0f) {
            dashCooldown = stats.dashRecovery;
            state = dashInAir ? State.AIR : State.FREE;
        }
    }

    // ===================== GÂY DAME CẬN CHIẾN =====================

    /** Gọi 1 lần khi đòn thường tới frame gây dame. TODO: hitbox, trừ máu m.damage. */
    private void doHit(Move m) {
    }

    /** Gọi mỗi khi skill cận chiến tới 1 frame gây dame. TODO: trừ máu s.damage. */
    private void doSkillHit(Skill s, int hitIndex) {
    }

    // ===================== API CHO GAMESCREEN =====================

    /** Vẽ nhân vật. */
    public void draw(SpriteBatch batch) {
        character.draw(batch);
    }

    /** Vẽ effect của nhân vật này (gọi sau khi vẽ cả 2 nhân vật để effect nằm đè lên). */
    public void drawEffects(SpriteBatch batch) {
        for (int i = 0; i < effects.size; i++) effects.get(i).draw(batch);
    }

    /** Effect đang sống, để GameScreen check va chạm với đối thủ. */
    public Array<Effect> getEffects() {
        return effects;
    }

    public void setGroundY(float groundY) {
        this.groundY = groundY;
    }

    public void onResize(float oldW, float newW) {
        if (oldW > 0) centerX *= newW / oldW;
    }

    /** Giải phóng Texture của effect còn sống. Gọi khi thoát trận. */
    public void dispose() {
        for (Effect e : effects) e.cancel();
        effects.clear();
    }
}
