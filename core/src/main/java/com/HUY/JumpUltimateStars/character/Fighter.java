package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.character.stats.AirSkill;
import com.HUY.JumpUltimateStars.character.stats.AirSkillContext;
import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.CharacterStatsRegistry;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;
import com.HUY.JumpUltimateStars.character.stats.Ultimate;
import com.HUY.JumpUltimateStars.effect.Effect;
import com.HUY.JumpUltimateStars.hethong.PlayerController;
import com.HUY.JumpUltimateStars.map.MapData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Nhân vật đang chiến đấu trong GameScreen.
 *
 * Ghép các thành phần:
 *  - Character:        animation + vẽ sprite
 *  - PlayerController: đọc phím (người chơi hoặc BotController)
 *  - CharacterStats:   chỉ số cố định
 *  - Effect:           Fighter tự giữ danh sách effect do mình sinh ra
 *  - MapData:          để biết platform
 *  - Marker:           ảnh P1 / P2 vẽ trên đầu
 *
 * Sàn: groundY là SÀN ĐANG ĐỨNG (mặt đất chính hoặc 1 platform), mainGroundY là mặt đất chính.
 * Khi đang ở trên không thì groundY luôn = mainGroundY. Chỉ lúc hạ cánh lên platform
 * thì groundY mới đổi sang độ cao platform (và airY = 0).
 *
 * Hitbox / hurtbox: đều là khung của frame sprite đang vẽ (1:1 với asset, đã nhân scale).
 * Hộp chặn nhau (getBodyWidth / getBodyHeight) hẹp hơn, chỉ dùng để 2 nhân vật không đè lên nhau.
 *
 * KHÓA PHÍM (setInputLocked): true thì mọi phím coi như không bấm.
 * HẾT MÁU (knockOut): knockdown rồi nằm vĩnh viễn ở frame cuối animation knockout.
 *
 * State machine:
 *
 *   FREE ──bấm J──> ATTACK ──hết animation──> RECOVERY ──hết giờ──> FREE
 *                     ^                           |
 *                     └────── bấm J (nối đòn) ────┘  (chỉ khi chưa phải đòn cuối)
 *
 *   FREE / RECOVERY / LAND / ATTACK(sau frame gây dame + vài frame) ──bấm U──> SKILL ──hết──> FREE
 *   FREE / RECOVERY / LAND ──bấm I (đủ 1 thanh energy)──> ULTIMATE ──hết (hoặc hụt)──> FREE
 *   FREE / RECOVERY / LAND ──bấm K──> AIR ──chạm đất──> LAND ──hết──> FREE
 *   FREE / RECOVERY / LAND / AIR ──bấm L──> DASH ──hết──> FREE (hoặc AIR nếu lướt trên không)
 *   FREE / RECOVERY / LAND ──GIỮ S──> DEFEND ──thả S──> FREE
 *   AIR ──bấm J (1 lần mỗi lần nhảy)──> AIR_ATTACK ──hết──> AIR (rơi tiếp)
 *   AIR ──bấm U (1 lần mỗi lần nhảy)──> AIR_SKILL ──xong──> LAND hoặc AIR
 *   AIR_ATTACK ──bấm U (sau frame gây dame + vài frame)──> AIR_SKILL
 *   Đang đứng trên platform mà đi ra khỏi mép ──> AIR (rơi)
 *
 *   BẤT KỲ state nào (trừ DEFEND, KNOCKDOWN, GET_UP) ──bị đòn hit──> HIT ──hết HIT_DURATION──> FREE (hoặc AIR)
 *        Đang ULTIMATE mà bị đánh thì chiêu bị hủy.
 *   BẤT KỲ state nào (trừ DEFEND, KNOCKDOWN, GET_UP) ──bị đòn knockdown──> KNOCKDOWN
 *        Bị đánh BẬT ra sau + lên (vòng cung), rơi xuống chạm đất rồi nằm KNOCKDOWN_LIE_TIME giây
 *        ──> GET_UP ──hết animation──> FREE
 *        Nếu dead (hết máu): nằm mãi, không GET_UP.
 *   DEFEND ──bị dính đòn──> vẫn DEFEND (chỉ mất guardDamage, không bị bật)
 */
public class Fighter {

    // ===================== HẰNG SỐ =====================

    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float RUN_FRAME_DURATION = 0.08f;
    private static final float JUMP_FRAME_DURATION = 0.08f;
    private static final float FALL_FRAME_DURATION = 0.08f;
    private static final float LAND_FRAME_DURATION = 0.06f;
    private static final float DASH_FRAME_DURATION = 0.05f;
    private static final float DEFEND_FRAME_DURATION = 0.06f;

    // Bị đánh trúng (hit thường)
    private static final float HIT_DURATION = 0.6f;
    private static final float ENERGY_ON_HURT = 2f;
    private static final float HIT_FRAME_DURATION = 0.05f;

    // Knockout
    private static final String KNOCKDOWN_ACTION = "knockout";
    private static final String GET_UP_ACTION = "get_up";
    private static final float KNOCKDOWN_FRAME_DURATION = 0.1f;
    private static final float GET_UP_FRAME_DURATION = 0.1f;
    private static final float KNOCKDOWN_LIE_TIME = 1f;
    private boolean KNOCKDOWN_ANIM_DONE;

    // bị knockdown thì bật ra sau + lên
    private static final float KNOCKBACK_SPEED = 800f;      // tốc độ bật ngang lúc đầu (pixel/giây)
    private static final float KNOCKBACK_FRICTION = 600f;   // giảm tốc ngang mỗi giây (nhỏ = trượt xa hơn)
    private static final float KNOCKBACK_LIFT = 600f;       // tốc độ bật lên lúc đầu (pixel/giây)

    // hộp thân để 2 nhân vật không đè lên nhau = 60% bề rộng ô neo
    private static final float BODY_WIDTH_RATIO = 0.6f;

    private static final int SKILL_CANCEL_DELAY_FRAMES = 3;
    private static final int AIR_SKILL_CANCEL_DELAY_FRAMES = 5;

    // Marker P1 / P2 trên đầu
    private static final float MARKER_SCALE = 1f;
    private static final float MARKER_GAP = 6f;
    private static final float MARKER_HEAD_OFFSET = 0f;

    private enum State { FREE, ATTACK, RECOVERY, AIR, LAND, DASH, SKILL, DEFEND, AIR_ATTACK, AIR_SKILL, HIT,
        KNOCKDOWN, GET_UP, ULTIMATE, WIN }

    // ===================== THÀNH PHẦN =====================

    private final Character character;
    private final PlayerController controller;
    private final CharacterStats stats;
    private final CharacterAnimationManager animManager;
    private final String charId;
    private final Move[] moves;

    private final Array<Effect> effects = new Array<>();

    private Texture markerTex;
    private float headHeight;

    // ===================== MÁU & VA CHẠM =====================

    private int hp;
    private float energy;
    private float hitTimer;
    private Fighter opponent;
    private final Rectangle hurtbox = new Rectangle();
    private final Rectangle attackBox = new Rectangle();
    private int hitsDealt;

    // ===================== KHÓA PHÍM & HẾT MÁU =====================

    private boolean inputLocked;
    private boolean dead;

    // ===================== VỊ TRÍ & HƯỚNG MẶT =====================

    private float centerX;
    private float groundY;
    private float mainGroundY;
    private boolean facingLeft;
    private float worldWidth = Float.MAX_VALUE;   // để pushX() giữ nhân vật trong map

    private MapData map;

    private State state = State.FREE;

    // ===================== ĐÁNH THƯỜNG =====================

    private Move move;
    private int comboIndex;
    private int frameCount;
    private float stateTime;
    private float recoveryTimer;
    private boolean buffered;
    private boolean skillBuffered;
    private boolean ultBuffered;
    private boolean hitDone;

    // ===================== SKILL =====================

    private Skill skill;
    private int skillFrameCount;
    private float skillTime;
    private int skillNextHit;
    private boolean skillSpawned;
    private float skillCooldown;

    // ===================== ULTIMATE =====================

    private Ultimate ult;
    private final Ultimate.Context ultCtx = new Ultimate.Context();
    private boolean ultReady;

    // ===================== NHẢY / LƯỚT =====================

    private float airY;
    private float velY;
    private int jumpsUsed;
    private float landTimer;

    private float dashTimer;
    private float dashSpeed;
    private int dashDir;
    private float dashCooldown;
    private boolean dashInAir;
    private int airDashesUsed;

    // ===================== ĐÁNH TRÊN KHÔNG =====================

    private boolean airAttackUsed;
    private int airAttackFrameCount;
    private float airAttackTime;
    private boolean airAttackHitDone;
    private boolean airSkillBuffered;

    // ===================== SKILL U TRÊN KHÔNG =====================

    private AirSkill airSkill;
    private final AirSkillContext airCtx = new AirSkillContext(effects);
    private boolean airSkillUsed;
    private int airSkillFrameCount;
    private float airSkillTime;
    private boolean airSkillHitDone;

    // ===================== KNOCKOUT =====================

    private boolean knockGrounded;
    private float knockdownTimer;
    private float getUpTimer;
    private float knockVelX;         //vận tốc bật ngang, 0 = đứng yên

    // Pose thắng
    private static final String WIN_ACTION = "winner";
    private static final float WIN_FRAME_DURATION = 0.2f;

    public Fighter(CharacterAnimationManager animManager, String charId,
                   PlayerController controller, float startCenterX, boolean facingLeft) {
        this.animManager = animManager;
        this.charId = charId;
        this.controller = controller;
        this.stats = CharacterStatsRegistry.get(charId);
        this.moves = stats.moves;
        this.hp = stats.maxHp;
        this.centerX = startCenterX;
        this.facingLeft = facingLeft;

        // Load sẵn animation đòn đánh kiểu NORMAL. Phải làm TRƯỚC mọi chỗ khác.
        for (Move m : moves) {
            animManager.get(charId, m.action, m.frameDuration, Animation.PlayMode.NORMAL);
        }

        character = new Character(animManager, charId);
        character.setScale(stats.scale);
        character.playAnimation("idle", IDLE_FRAME_DURATION);

        TextureRegion idle0 = animManager.get(charId, "idle", IDLE_FRAME_DURATION).getKeyFrames()[0];
        headHeight = idle0.getRegionHeight() * stats.scale;

        animManager.get(charId, "jump", JUMP_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "fall", FALL_FRAME_DURATION, Animation.PlayMode.LOOP);
        animManager.get(charId, "land", LAND_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "dash", DASH_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "def", DEFEND_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "hit", HIT_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, KNOCKDOWN_ACTION, KNOCKDOWN_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, GET_UP_ACTION, GET_UP_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, WIN_ACTION, WIN_FRAME_DURATION, Animation.PlayMode.NORMAL);
        if (stats.airMove != null) {
            animManager.get(charId, stats.airMove.action, stats.airMove.frameDuration, Animation.PlayMode.NORMAL);
        }
        if (stats.airSkillU != null) {
            animManager.get(charId, stats.airSkillU.action, stats.airSkillU.frameDuration, Animation.PlayMode.NORMAL);
        }
        if (stats.skillU != null) {
            animManager.get(charId, stats.skillU.action, stats.skillU.frameDuration, Animation.PlayMode.NORMAL);
        }
        if (stats.ultimate != null) {
            Ultimate u = stats.ultimate.create();
            animManager.get(charId, u.action, u.frameDuration, Animation.PlayMode.NORMAL);
        }

        character.setRefWidth(character.getCurrentFrameWidth());
        character.setFlipX(facingLeft);
    }

    // ===================== UPDATE =====================

    /**
     * Mỗi frame: đọc input, chạy state machine, cập nhật effect, giữ nhân vật trong màn hình.
     *
     * @param delta   giây từ frame trước
     * @param screenW chiều rộng THẾ GIỚI (= ảnh map)
     */
    public void update(float delta, float screenW) {
        worldWidth = screenW;

        if (dashCooldown > 0f) dashCooldown -= delta;
        if (skillCooldown > 0f) skillCooldown -= delta;

        boolean lk = inputLocked;
        boolean atk = !lk && controller.isAttackPressed();
        boolean jump = !lk && controller.isJumpPressed();
        boolean dash = !lk && controller.isDashPressed() && dashCooldown <= 0f;
        boolean skillKey = !lk && controller.isSkillUPressed();
        boolean skillU = skillKey && stats.skillU != null && skillCooldown <= 0f;
        boolean airSkillU = skillKey && stats.airSkillU != null && !airSkillUsed && skillCooldown <= 0f;
        boolean defend = !lk && controller.isDefendHeld();
        ultReady = !lk && controller.isUltimatePressed() && stats.ultimate != null
            && energy >= CharacterStats.ENERGY_PER_BAR;

        switch (state) {
            case FREE:
                if (dash) startDash();
                else if (jump) startJump();
                else if (ultReady) startUltimate();
                else if (skillU) startSkill(stats.skillU);
                else if (atk && moves.length > 0) startAttack(0);
                else if (defend) startDefend();
                else updateMovement(delta);
                break;
            case ATTACK:   updateAttack(delta, atk, skillU); break;
            case RECOVERY: updateRecovery(delta, atk, jump, dash, skillU, defend); break;
            case AIR:      updateAir(delta, atk, jump, dash, airSkillU); break;
            case AIR_SKILL: updateAirSkill(delta); break;
            case AIR_ATTACK: updateAirAttack(delta, airSkillU); break;
            case LAND:     updateLand(delta, atk, jump, dash, skillU, defend); break;
            case DASH:     updateDash(delta); break;
            case SKILL:    updateSkill(delta); break;
            case DEFEND:   updateDefend(delta, defend); break;
            case HIT:      updateHit(delta); break;
            case KNOCKDOWN: updateKnockdown(delta); break;
            case GET_UP:   updateGetUp(delta); break;
            case ULTIMATE: updateUltimate(delta); break;
            case WIN:      break;
        }

        updateEffects(delta);

        character.setFlipX(facingLeft);
        character.update(delta);

        float halfW = character.getRefWidth() / 2f;
        centerX = MathUtils.clamp(centerX, halfW, screenW - halfW);
        checkPlatformEdge();
        character.setCenter(centerX, groundY + airY);
    }

    private void updateEffects(float delta) {
        for (int i = effects.size - 1; i >= 0; i--) {
            Effect e = effects.get(i);
            e.update(delta);
            if (!e.isActive()) effects.removeIndex(i);
        }
    }

    // ===================== PLATFORM =====================

    private void leaveFloor() {
        airY += groundY - mainGroundY;
        groundY = mainGroundY;
    }

    private void checkPlatformEdge() {
        if (map == null || groundY == mainGroundY) return;
        boolean inAir = state == State.AIR || state == State.AIR_ATTACK || state == State.AIR_SKILL
            || (state == State.DASH && dashInAir);
        if (inAir) return;
        if (!map.hasPlatformAt(centerX, groundY)) {
            leaveFloor();
            velY = 0f;
            jumpsUsed = Math.max(jumpsUsed, 1);
            state = State.AIR;
            character.playAnimation("fall", FALL_FRAME_DURATION);
        }
    }

    // ===================== DI CHUYỂN =====================

    private int moveInput() {
        return inputLocked ? 0 : controller.getMoveDirection();
    }

    private int moveHorizontal(float delta) {
        int dir = moveInput();
        if (dir != 0) {
            facingLeft = dir < 0;
            centerX += dir * stats.moveSpeed * delta;
        }
        return dir;
    }

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
        ultBuffered = false;
        hitDone = false;
        state = State.ATTACK;
        character.playFromStart(move.action, move.frameDuration);
    }

    private void updateAttack(float delta, boolean atk, boolean skillU) {
        if (atk) buffered = true;
        if (skillU) skillBuffered = true;
        if (ultReady) ultBuffered = true;
        stateTime += delta;

        int frame = Math.min((int) (stateTime / move.frameDuration), frameCount - 1);

        if (!hitDone && frame >= move.hitFrame) {
            hitDone = true;
            doHit(move);
        }

        int cancelFrame = Math.min(move.hitFrame + SKILL_CANCEL_DELAY_FRAMES, frameCount - 1);

        if (ultBuffered && frame >= cancelFrame
            && stats.ultimate != null && energy >= CharacterStats.ENERGY_PER_BAR) {
            comboIndex = 0;
            startUltimate();
            return;
        }

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

    private void updateRecovery(float delta, boolean atk, boolean jump, boolean dash,
                                boolean skillU, boolean defend) {
        boolean isLast = comboIndex == moves.length - 1;
        recoveryTimer -= delta;

        if (dash)   { comboIndex = 0; startDash(); return; }
        if (jump)   { comboIndex = 0; startJump(); return; }
        if (ultReady) { comboIndex = 0; startUltimate(); return; }
        if (skillU) { comboIndex = 0; startSkill(stats.skillU); return; }
        if (atk && !isLast) { startAttack(comboIndex + 1); return; }
        if (defend) { comboIndex = 0; startDefend(); return; }

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

    private void updateSkill(float delta) {
        skillTime += delta;
        int frame = Math.min((int) (skillTime / skill.frameDuration), skillFrameCount - 1);

        while (skillNextHit < skill.hitFrames.length && frame >= skill.hitFrames[skillNextHit]) {
            doSkillHit(skill, skillNextHit);
            skillNextHit++;
        }

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

    // ===================== ULTIMATE =====================

    private void startUltimate() {
        addEnergy(-CharacterStats.ENERGY_PER_BAR);
        comboIndex = 0;
        ult = stats.ultimate.create();
        ultCtx.effects = effects;
        ultCtx.frameCount = animManager.get(charId, ult.action, ult.frameDuration).getKeyFrames().length;
        ultCtx.dark = false;
        state = State.ULTIMATE;
        updateUltimate(0f);
    }

    private void updateUltimate(float delta) {
        ultCtx.x = centerX;
        ultCtx.feetY = groundY + airY;
        ultCtx.dir = facingLeft ? -1 : 1;
        ultCtx.touching = opponent != null && !opponent.isInvulnerable()
            && getAttackBox().overlaps(opponent.getHurtbox());
        ultCtx.damage = 0;
        ultCtx.knockdown = false;

        boolean finished = ult.update(ultCtx, delta);

        centerX = ultCtx.x;
        character.showFrame(ult.action, ult.frameDuration, ultCtx.frame);
        if (ultCtx.damage > 0) hitOpponent(ultCtx.damage, ultCtx.knockdown, 0f);
        if (finished) state = State.FREE;
    }

    // ===================== THỦ =====================

    private void startDefend() {
        state = State.DEFEND;
        character.playFromStart("def", DEFEND_FRAME_DURATION);
    }

    private void updateDefend(float delta, boolean defend) {
        if (!defend) {
            state = State.FREE;
            updateMovement(delta);
        }
    }

    public boolean isDefending() {
        return state == State.DEFEND;
    }

    public boolean isUsingUltimate() {
        return state == State.ULTIMATE;
    }

    public float getUltimateDarkTime() {
        return ult != null ? ult.darkTime : 0f;
    }

    public void setUltimateDark(boolean dark) {
        ultCtx.dark = dark;
    }

    public int calcIncomingDamage(int rawDamage) {
        return isBlocking() ? stats.guardDamage : rawDamage;
    }

    private boolean isBlocking() {
        return state == State.DEFEND
            || (state == State.FREE && !inputLocked && controller.isDefendHeld());
    }

    // ===================== NHẢY / RƠI / CHẠM ĐẤT =====================

    private void startJump() {
        leaveFloor();
        jumpsUsed++;
        velY = stats.jumpSpeed;
        state = State.AIR;
        character.playFromStart("jump", JUMP_FRAME_DURATION);
    }

    private void updateAir(float delta, boolean atk, boolean jump, boolean dash, boolean airSkillU) {
        if (dash && airDashesUsed < stats.maxAirDashes) { startDash(); return; }
        if (airSkillU) { startAirSkill(); return; }
        if (atk && stats.airMove != null && !airAttackUsed) { startAirAttack(); return; }
        if (jump && jumpsUsed < stats.maxJumps) startJump();

        moveHorizontal(delta);

        float prevFeet = groundY + airY;
        velY -= stats.gravity * delta;
        airY += velY * delta;

        if (map != null && velY <= 0f) {
            float landY = map.findLandingY(centerX, prevFeet, groundY + airY);
            if (landY >= 0f) {
                groundY = landY;
                airY = 0f;
                touchGround();
                return;
            }
        }

        if (airY <= 0f) {
            touchGround();
            return;
        }

        if (velY <= 0f) character.playAnimation("fall", FALL_FRAME_DURATION);
    }

    private void resetAirFlags() {
        velY = 0f;
        jumpsUsed = 0;
        airDashesUsed = 0;
        airAttackUsed = false;
        airSkillUsed = false;
    }

    private void touchGround() {
        airY = 0f;
        resetAirFlags();
        startLand();
    }

    private void startLand() {
        int frames = animManager.get(charId, "land", LAND_FRAME_DURATION).getKeyFrames().length;
        landTimer = frames * LAND_FRAME_DURATION;
        state = State.LAND;
        character.playFromStart("land", LAND_FRAME_DURATION);
    }

    private void updateLand(float delta, boolean atk, boolean jump, boolean dash,
                            boolean skillU, boolean defend) {
        if (dash) { startDash(); return; }
        if (jump) { startJump(); return; }
        if (ultReady) { startUltimate(); return; }
        if (skillU) { startSkill(stats.skillU); return; }
        if (atk && moves.length > 0) { startAttack(0); return; }
        if (defend) { startDefend(); return; }

        if (moveInput() != 0) {
            state = State.FREE;
            updateMovement(delta);
            return;
        }

        landTimer -= delta;
        if (landTimer <= 0f) state = State.FREE;
    }

    // ===================== ĐÁNH TRÊN KHÔNG =====================

    private void startAirAttack() {
        Move m = stats.airMove;
        airAttackUsed = true;
        airAttackFrameCount = animManager.get(charId, m.action, m.frameDuration).getKeyFrames().length;
        airAttackTime = 0f;
        airAttackHitDone = false;
        airSkillBuffered = false;
        state = State.AIR_ATTACK;
        character.playFromStart(m.action, m.frameDuration);
    }

    private void updateAirAttack(float delta, boolean airSkillU) {
        Move m = stats.airMove;
        if (airSkillU) airSkillBuffered = true;
        airAttackTime += delta;

        int frame = Math.min((int) (airAttackTime / m.frameDuration), airAttackFrameCount - 1);
        if (!airAttackHitDone && frame >= m.hitFrame) {
            airAttackHitDone = true;
            doHit(m);
        }

        int cancelFrame = Math.min(m.hitFrame + AIR_SKILL_CANCEL_DELAY_FRAMES, airAttackFrameCount - 1);
        if (airSkillBuffered && frame >= cancelFrame) {
            startAirSkill();
            return;
        }

        if (airAttackTime >= airAttackFrameCount * m.frameDuration) {
            velY = Math.min(velY, 0f);
            state = State.AIR;
        }
    }

    // ===================== SKILL U TRÊN KHÔNG =====================

    private void startAirSkill() {
        airSkill = stats.airSkillU;
        airSkillUsed = true;
        airSkillFrameCount = animManager.get(charId, airSkill.action, airSkill.frameDuration).getKeyFrames().length;
        airSkillTime = 0f;
        airSkillHitDone = false;
        velY = 0f;
        airCtx.reset(centerX, airY, groundY, facingLeft ? -1 : 1);
        airSkill.start(airCtx);
        state = State.AIR_SKILL;
        character.playFromStart(airSkill.action, airSkill.frameDuration);
    }

    private void updateAirSkill(float delta) {
        airSkillTime += delta;

        airCtx.x = centerX;
        airCtx.airY = airY;
        airCtx.groundY = groundY;
        airCtx.time = airSkillTime;
        airCtx.frame = Math.min((int) (airSkillTime / airSkill.frameDuration), airSkillFrameCount - 1);
        airCtx.animDone = airSkillTime >= airSkillFrameCount * airSkill.frameDuration;

        float prevFeet = groundY + airY;

        boolean finished = airSkill.update(airCtx, delta);

        centerX = airCtx.x;
        airY = airCtx.airY;

        boolean landedOnPlatform = false;
        if (map != null && groundY + airY < prevFeet) {
            float landY = map.findLandingY(centerX, prevFeet, groundY + airY);
            if (landY >= 0f) {
                groundY = landY;
                airY = 0f;
                landedOnPlatform = true;
            }
        }

        if (airCtx.hitDamage > 0 && !airSkillHitDone) {
            airSkillHitDone = doAirSkillHit(airCtx.hitDamage, airCtx.hitKnockdown);
        }
        airCtx.hitDamage = 0;
        airCtx.hitKnockdown = false;

        if (finished || landedOnPlatform) {
            skillCooldown = airSkill.cooldown;
            if (airY <= 0f) {
                touchGround();
            } else {
                velY = 0f;
                state = State.AIR;
            }
        }
    }

    // ===================== LƯỚT =====================

    private void startDash() {
        if (state == State.DASH) return;
        dashInAir = (state == State.AIR);
        if (dashInAir) {
            velY = 0f;
            airDashesUsed++;
        }

        dashDir = facingLeft ? -1 : 1;
        dashTimer = stats.dashDuration;
        dashSpeed = stats.dashDistance / stats.dashDuration;

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

    // ===================== KNOCKOUT =====================

    /**
     * Bắt đầu bị knockout. Đang đứng thì nằm luôn; đang trên không thì rơi xuống.
     * (takeDamage sẽ ghi đè velY / knockVelX / knockGrounded sau khi gọi hàm này để bật người ra.)
     */
    private void startKnockdown() {
        knockVelX = 0f;   // đường knockOut() (hết máu) không bị bật
        velY = 0f;
        comboIndex = 0;
        dashInAir = false;
        knockGrounded = airY <= 0f;
        knockdownTimer = KNOCKDOWN_LIE_TIME;
        if (knockGrounded) resetAirFlags();
        state = State.KNOCKDOWN;
        KNOCKDOWN_ANIM_DONE = false;
        character.playFromStart(KNOCKDOWN_ACTION, KNOCKDOWN_FRAME_DURATION);
    }

    private void updateKnockdown(float delta) {
        // bị bật ngang, giảm dần về 0 (đang bay hay đã nằm đều trượt tiếp)
        if (knockVelX != 0f) {
            centerX += knockVelX * delta;
            float drag = KNOCKBACK_FRICTION * delta;
            if (Math.abs(knockVelX) <= drag) knockVelX = 0f;
            else knockVelX -= Math.signum(knockVelX) * drag;
        }

        if (!KNOCKDOWN_ANIM_DONE && character.isAnimationFinished()) {
            KNOCKDOWN_ANIM_DONE = true;
        }

        if (!knockGrounded) {
            float prevFeet = groundY + airY;
            velY -= stats.gravity * delta;
            airY += velY * delta;

            if (map != null && velY <= 0f) {
                float landY = map.findLandingY(centerX, prevFeet, groundY + airY);
                if (landY >= 0f) {
                    groundY = landY;
                    airY = 0f;
                    knockGrounded = true;
                }
            }
            // thêm velY <= 0 để đang bay lên thì không bị coi là chạm đất
            if (!knockGrounded && airY <= 0f && velY <= 0f) {
                airY = 0f;
                knockGrounded = true;
            }
            if (knockGrounded) {
                resetAirFlags();
                knockdownTimer = KNOCKDOWN_LIE_TIME;
            }
            return;
        }

        if (!KNOCKDOWN_ANIM_DONE) return;
        if (dead) return;
        knockdownTimer -= delta;
        if (knockdownTimer <= 0f) startGetUp();
    }

    private void startGetUp() {
        int frames = animManager.get(charId, GET_UP_ACTION, GET_UP_FRAME_DURATION).getKeyFrames().length;
        getUpTimer = frames * GET_UP_FRAME_DURATION;
        state = State.GET_UP;
        character.playFromStart(GET_UP_ACTION, GET_UP_FRAME_DURATION);
    }

    private void updateGetUp(float delta) {
        getUpTimer -= delta;
        if (getUpTimer <= 0f) state = State.FREE;
    }

    /** Đang knockout / đứng dậy / lướt / thắng: không nhận dame. */
    public boolean isInvulnerable() {
        return state == State.KNOCKDOWN || state == State.GET_UP || state == State.DASH || state == State.WIN;
    }

    // ===================== CHẶN NHAU (MỚI) =====================

    /** Lướt / ultimate / skill lao / nằm / đứng dậy thì xuyên qua người kia được. */
    public boolean isPhasing() {
        return state == State.DASH || state == State.ULTIMATE || state == State.AIR_SKILL
            || state == State.KNOCKDOWN || state == State.GET_UP;
    }

    /** Bị đẩy ngang (do va chạm thân), có giữ trong map. */
    public void pushX(float dx) {
        float halfW = character.getRefWidth() / 2f;
        centerX = MathUtils.clamp(centerX + dx, halfW, worldWidth - halfW);
        character.setCenter(centerX, groundY + airY);
    }

    /** Bề rộng thân dùng để chặn nhau (hẹp hơn sprite). Chỉnh BODY_WIDTH_RATIO nếu chưa vừa. */
    public float getBodyWidth() {
        return character.getRefWidth() * BODY_WIDTH_RATIO;
    }

    public float getBodyHeight() {
        return headHeight;
    }

    // ===================== KHÓA PHÍM & HẾT MÁU =====================

    public void setInputLocked(boolean locked) {
        inputLocked = locked;
    }

    public void knockOut() {
        if (dead) return;
        dead = true;
        if (state != State.KNOCKDOWN) startKnockdown();
    }

    public boolean isReadyForWinPose() {
        return state == State.FREE;
    }

    public void playWinPose() {
        comboIndex = 0;
        state = State.WIN;
        character.playFromStart(WIN_ACTION, WIN_FRAME_DURATION);
    }

    public boolean isWinPoseDone() {
        return state == State.WIN && character.isAnimationFinished();
    }

    // ===================== GÂY / NHẬN DAME =====================

    public void setOpponent(Fighter opponent) {
        this.opponent = opponent;
    }

    private Rectangle spriteBox(Rectangle out) {
        float refHalf = character.getRefWidth() / 2f;
        float w = character.getCurrentFrameWidth();
        float h = character.getCurrentFrameHeight();
        float left = facingLeft ? centerX + refHalf - w : centerX - refHalf;
        out.set(left, getFeetY(), w, h);
        return out;
    }

    public Rectangle getHurtbox() {
        return spriteBox(hurtbox);
    }

    private Rectangle getAttackBox() {
        return spriteBox(attackBox);
    }

    private boolean hitOpponent(int damage, boolean knockdown, float energyGain) {
        if (opponent == null || opponent.isInvulnerable()
            || !getAttackBox().overlaps(opponent.getHurtbox())) return false;
        opponent.takeDamage(damage, centerX, knockdown);
        addEnergy(energyGain);
        hitsDealt++;
        return true;
    }

    private void doHit(Move m) {
        hitOpponent(m.damage, m.knockdown, m.energy);
    }

    private void doSkillHit(Skill s, int hitIndex) {
        boolean ko = s.knockdownOnLastHit && hitIndex == s.hitFrames.length - 1;
        hitOpponent(s.damage, ko, s.energy);
    }

    private boolean doAirSkillHit(int damage, boolean knockdown) {
        return hitOpponent(damage, knockdown, airSkill.energy);
    }

    public void takeDamage(int rawDamage, float sourceX) {
        takeDamage(rawDamage, sourceX, false);
    }

    /**
     * Nhận dame (đã tính thủ). sourceX = tâm ngang của kẻ tấn công.
     * Đang knockout / đứng dậy: bỏ qua hoàn toàn.
     * Đang thủ: chỉ mất máu, không stun, không knockout, không bị bật.
     * Không thủ + knockdown: bị đánh BẬT ra sau + lên, rồi rơi xuống nằm.
     * Không thủ + hit thường: vào HIT, đứng yên trong HIT_DURATION giây.
     */
    public void takeDamage(int rawDamage, float sourceX, boolean knockdown) {
        if (isInvulnerable()) return;
        boolean blocking = isBlocking();
        if (blocking && state == State.FREE) startDefend();
        if (state == State.ULTIMATE) ult.cancel();

        hp = Math.max(0, hp - calcIncomingDamage(rawDamage));
        addEnergy(ENERGY_ON_HURT);

        if (blocking) {
            if (sourceX < centerX) facingLeft = true;
            else if (sourceX > centerX) facingLeft = false;
            return;
        }

        if (knockdown) {
            startKnockdown();

            // bật ra xa kẻ tấn công
            knockVelX = (sourceX < centerX ? 1f : -1f) * KNOCKBACK_SPEED;

            // bật lên. Rời sàn đang đứng (nếu là platform) rồi cho vận tốc dọc
            leaveFloor();
            airY += 1f;                 // nhấc nhẹ khỏi sàn để không bị coi là đã chạm đất
            velY = KNOCKBACK_LIFT;
            knockGrounded = false;
            return;
        }

        velY = 0f;
        comboIndex = 0;
        hitTimer = HIT_DURATION;
        state = State.HIT;
        int frames = animManager.get(charId, "hit", HIT_FRAME_DURATION).getKeyFrames().length;
        character.playFromStart("hit", HIT_DURATION / frames);
    }

    private void updateHit(float delta) {
        hitTimer -= delta;
        if (hitTimer <= 0f) {
            if (airY > 0f) {
                velY = 0f;
                state = State.AIR;
            } else {
                state = State.FREE;
            }
        }
    }

    public int getHp() {
        return hp;
    }

    // cho chế độ luyện tập
    public int getMaxHp() {
        return stats.maxHp;
    }

    /** Hồi đầy máu (dùng cho chế độ luyện tập). */
    public void restoreHp() {
        hp = stats.maxHp;
    }

    public void addEnergy(float amount) {
        float max = CharacterStats.ENERGY_PER_BAR * CharacterStats.MAX_ENERGY_BARS;
        energy = MathUtils.clamp(energy + amount, 0f, max);
    }

    public float getEnergy() {
        return energy;
    }

    public int getHitsDealt() {
        return hitsDealt;
    }

    public void addHitCount() {
        hitsDealt++;
    }

    // ===================== API CHO GAMESCREEN =====================

    public void draw(SpriteBatch batch) {
        character.draw(batch);
    }

    public void drawEffects(SpriteBatch batch) {
        for (int i = 0; i < effects.size; i++) effects.get(i).draw(batch);
    }

    public void setMarker(String path) {
        if (markerTex != null) markerTex.dispose();
        markerTex = new Texture(Gdx.files.internal(path));
        markerTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public void drawMarker(SpriteBatch batch) {
        if (markerTex == null) return;
        float w = markerTex.getWidth() * MARKER_SCALE;
        float h = markerTex.getHeight() * MARKER_SCALE;
        float x = centerX - w / 2f;
        float y = getFeetY() + headHeight + MARKER_HEAD_OFFSET + MARKER_GAP;
        batch.draw(markerTex, x, y, w, h);
    }

    public Array<Effect> getEffects() {
        return effects;
    }

    public void setGroundY(float groundY) {
        this.mainGroundY = groundY;
        this.groundY = groundY;
    }

    public void setMap(MapData map) {
        this.map = map;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getFeetY() {
        return groundY + airY;
    }

    public void dispose() {
        for (Effect e : effects) e.cancel();
        effects.clear();
        if (markerTex != null) {
            markerTex.dispose();
            markerTex = null;
        }
    }
}
