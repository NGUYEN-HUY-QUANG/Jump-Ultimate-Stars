package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.character.stats.AirSkill;
import com.HUY.JumpUltimateStars.character.stats.AirSkillContext;
import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.CharacterStatsRegistry;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;
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
 *  - PlayerController: đọc phím
 *  - CharacterStats:   chỉ số cố định (tốc độ, nhảy, lướt, đòn đánh, skill, dame khi thủ)
 *  - Effect:           Fighter tự giữ danh sách effect do mình sinh ra (phi tiêu...).
 *                      Effect tự bay, tự vẽ, tự tính dame; Fighter chỉ gọi ra đúng frame
 *                      và cập nhật/vẽ chúng.
 *  - MapData:          để biết platform (vùng đứng thêm ngoài mặt đất chính)
 *  - Marker:           ảnh P1 / P2 vẽ trên đầu để phân biệt 2 nhân vật (đi theo nhân vật)
 *
 * Sàn: groundY là SÀN ĐANG ĐỨNG (mặt đất chính hoặc 1 platform), mainGroundY là mặt đất chính.
 * Khi đang ở trên không (nhảy / rơi / skill trên không) thì groundY luôn = mainGroundY,
 * nên AirSkill vẫn tính airY so với mặt đất chính như cũ. Chỉ lúc hạ cánh lên platform
 * thì groundY mới đổi sang độ cao platform (và airY = 0).
 *
 * Hitbox / hurtbox: đều là khung của frame sprite đang vẽ (1:1 với asset, đã nhân scale).
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
 *   FREE / RECOVERY / LAND ──GIỮ S──> DEFEND ──thả S──> FREE
 *   AIR ──bấm J (1 lần mỗi lần nhảy)──> AIR_ATTACK (đứng yên giữa trời) ──hết──> AIR (rơi tiếp)
 *   AIR ──bấm U (1 lần mỗi lần nhảy)──> AIR_SKILL (mỗi loại skill tự quyết cách di chuyển)
 *         ──xong, đang ở đất / platform──> LAND   |   ──xong, còn trên không──> AIR (rơi tiếp)
 *   AIR_ATTACK ──bấm U (sau frame gây dame + vài frame)──> AIR_SKILL   (U ngắt J trên không, thành combo)
 *   Đang đứng trên platform mà đi ra khỏi mép ──> AIR (rơi)
 *
 *   BẤT KỲ state nào (trừ DEFEND, KNOCKDOWN, GET_UP) ──bị đòn hit──> HIT ──hết HIT_DURATION──> FREE
 *        (hoặc AIR nếu đang trên không). Trên không thì HIT đứng yên giữa trời, hết HIT mới rơi.
 *   BẤT KỲ state nào (trừ DEFEND, KNOCKDOWN, GET_UP) ──bị đòn knockout──> KNOCKDOWN
 *        Đang trên không: rơi xuống (vẫn animation knockout). Chạm đất rồi nằm KNOCKDOWN_LIE_TIME giây
 *        ──> GET_UP ──hết animation──> FREE
 *        KNOCKDOWN / GET_UP: không nhận dame, không điều khiển được.
 *   DEFEND ──bị dính đòn──> vẫn DEFEND (chỉ mất guardDamage, quay mặt về phía đòn nếu đánh từ sau,
 *        knockout cũng bị chặn)
 */
public class Fighter {

    // ===================== HẰNG SỐ =====================

    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float RUN_FRAME_DURATION = 0.08f;
    private static final float JUMP_FRAME_DURATION = 0.08f;
    private static final float FALL_FRAME_DURATION = 0.08f;
    private static final float LAND_FRAME_DURATION = 0.06f;
    private static final float DASH_FRAME_DURATION = 0.05f;   // chỉ để preload, thời gian thật tính ở startDash()
    private static final float DEFEND_FRAME_DURATION = 0.06f; // def_0 -> def_1 rồi giữ ở def_1

    // Bị đánh trúng (hit thường)
    private static final float HIT_DURATION = 0.6f;          // thời gian stun (giây)
    private static final float HIT_FRAME_DURATION = 0.05f;   // chỉ để preload, thời gian thật tính ở takeDamage()

    // Knockout
    private static final String KNOCKDOWN_ACTION = "knockout";   // tên thư mục ảnh, sửa cho đúng asset
    private static final String GET_UP_ACTION = "get_up";
    private static final float KNOCKDOWN_FRAME_DURATION = 0.1f;
    private static final float GET_UP_FRAME_DURATION = 0.1f;
    private static final float KNOCKDOWN_LIE_TIME = 1f;        // nằm sau khi chạm đất rồi mới đứng dậy
    private boolean KNOCKDOWN_ANIM_DONE;

    // Skill ngắt được đòn thường sau frame gây dame bao nhiêu frame (0 = ngắt ngay)
    private static final int SKILL_CANCEL_DELAY_FRAMES = 3;

    // Skill U trên không ngắt được đòn J trên không sau frame gây dame bao nhiêu frame (0 = ngắt ngay)
    private static final int AIR_SKILL_CANCEL_DELAY_FRAMES = 5;

    // Marker P1 / P2 trên đầu
    private static final float MARKER_SCALE = 1f;        // phóng to / thu nhỏ ảnh marker
    private static final float MARKER_GAP = 6f;          // khoảng cách từ đỉnh đầu lên marker (pixel thế giới)
    private static final float MARKER_HEAD_OFFSET = 0f;  // chỉnh nếu sprite có viền trống phía trên đầu (âm = hạ xuống)

    // FREE: tự do | ATTACK: đang ra đòn | RECOVERY: nghỉ sau đòn | AIR: trên không
    // LAND: chạm đất (không khóa) | DASH: lướt (khóa cứng) | SKILL: tung skill (khóa cứng)
    // DEFEND: đang giữ thủ (đứng im, thả phím mới thoát)
    // AIR_ATTACK: đánh trên không (khóa cứng, đứng yên giữa trời, không rơi / không bay lên)
    // AIR_SKILL: skill U trên không (khóa cứng, không có trọng lực, AirSkill tự điều khiển vị trí)
    // HIT: bị đánh trúng (stun, đứng yên trong HIT_DURATION giây, trên không cũng đứng yên)
    // KNOCKDOWN: bị knockout (trên không thì rơi xuống, chạm đất rồi nằm), không nhận dame
    // GET_UP: đứng dậy sau knockout, không nhận dame
    private enum State { FREE, ATTACK, RECOVERY, AIR, LAND, DASH, SKILL, DEFEND, AIR_ATTACK, AIR_SKILL, HIT,
        KNOCKDOWN, GET_UP }

    // ===================== THÀNH PHẦN =====================

    private final Character character;
    private final PlayerController controller;
    private final CharacterStats stats;
    private final CharacterAnimationManager animManager;   // dùng chung, GameScreen sở hữu
    private final String charId;
    private final Move[] moves;

    // Effect do Fighter này sinh ra. Fighter sở hữu, tự dispose khi xong
    private final Array<Effect> effects = new Array<>();

    // Marker P1 / P2 trên đầu (Fighter sở hữu texture này, dispose trong dispose())
    private Texture markerTex;      // null nếu chưa gắn marker
    private float headHeight;       // chiều cao nhân vật (pixel thế giới), tính 1 lần trong constructor

    // ===================== MÁU & VA CHẠM =====================

    private int hp;
    private float hitTimer;                     // thời gian stun còn lại
    private Fighter opponent;                   // null nếu SINGLE
    private final Rectangle hurtbox = new Rectangle();    // dùng lại, tránh new mỗi frame
    private final Rectangle attackBox = new Rectangle();

    // ===================== VỊ TRÍ & HƯỚNG MẶT =====================

    private float centerX;
    private float groundY;          // SÀN ĐANG ĐỨNG (mặt đất chính hoặc platform)
    private float mainGroundY;      // mặt đất chính của map
    private boolean facingLeft;

    private MapData map;            // để hỏi platform; null thì chỉ có mặt đất chính

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

    private float airY;             // độ cao so với sàn đang đứng (groundY)
    private float velY;
    private int jumpsUsed;
    private float landTimer;

    private float dashTimer;
    private float dashSpeed;
    private int dashDir;
    private float dashCooldown;
    private boolean dashInAir;
    private int airDashesUsed;      // số lần đã lướt trên không trong lượt nhảy này (reset khi chạm đất)

    // ===================== ĐÁNH TRÊN KHÔNG =====================

    private boolean airAttackUsed;  // đã đánh trên không trong lượt nhảy này (reset khi chạm đất)
    private int airAttackFrameCount;
    private float airAttackTime;
    private boolean airAttackHitDone;
    private boolean airSkillBuffered;   // đã bấm U lúc đang đánh J trên không -> ra skill khi tới frame hủy

    // ===================== SKILL U TRÊN KHÔNG =====================

    private AirSkill airSkill;
    private final AirSkillContext airCtx = new AirSkillContext(effects);   // trạng thái lần dùng này
    private boolean airSkillUsed;   // đã dùng skill U trên không trong lượt nhảy này (reset khi chạm đất)
    private int airSkillFrameCount;
    private float airSkillTime;
    private boolean airSkillHitDone;    // skill đã trúng đối thủ chưa (chỉ tính dame 1 lần mỗi lần dùng)

    // ===================== KNOCKOUT =====================

    private boolean knockGrounded;   // đã chạm đất chưa (chưa thì còn đang rơi)
    private float knockdownTimer;    // đếm thời gian nằm sau khi chạm đất
    private float getUpTimer;        // thời gian còn lại của animation đứng dậy

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
        this.hp = stats.maxHp;
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

        // Chiều cao nhân vật (để đặt marker trên đầu): frame đầu của idle * scale
        TextureRegion idle0 = animManager.get(charId, "idle", IDLE_FRAME_DURATION).getKeyFrames()[0];
        headHeight = idle0.getRegionHeight() * stats.scale;

        // Load sẵn nhảy / rơi / chạm đất / lướt / thủ / bị đánh / knockout / skill (thiếu asset sẽ lỗi ngay lúc vào trận)
        animManager.get(charId, "jump", JUMP_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "fall", FALL_FRAME_DURATION, Animation.PlayMode.LOOP);
        animManager.get(charId, "land", LAND_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "dash", DASH_FRAME_DURATION, Animation.PlayMode.NORMAL);
        // NORMAL: chạy hết frame cuối thì libGDX tự giữ ở def_1, không cần code giữ frame
        animManager.get(charId, "def", DEFEND_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, "hit", HIT_FRAME_DURATION, Animation.PlayMode.NORMAL);
        // NORMAL: knockout chạy hết thì giữ frame cuối (nằm) cho tới khi get_up
        animManager.get(charId, KNOCKDOWN_ACTION, KNOCKDOWN_FRAME_DURATION, Animation.PlayMode.NORMAL);
        animManager.get(charId, GET_UP_ACTION, GET_UP_FRAME_DURATION, Animation.PlayMode.NORMAL);
        if (stats.airMove != null) {
            animManager.get(charId, stats.airMove.action, stats.airMove.frameDuration, Animation.PlayMode.NORMAL);
        }
        if (stats.airSkillU != null) {
            animManager.get(charId, stats.airSkillU.action, stats.airSkillU.frameDuration, Animation.PlayMode.NORMAL);
        }
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
     * @param screenW chiều rộng THẾ GIỚI (= ảnh map), dùng để giữ nhân vật trong map
     */
    public void update(float delta, float screenW) {
        if (dashCooldown > 0f) dashCooldown -= delta;
        if (skillCooldown > 0f) skillCooldown -= delta;

        // Mỗi phím đọc 1 lần mỗi frame. dash và skillU đã gồm điều kiện cooldown
        boolean atk = controller.isAttackPressed();
        boolean jump = controller.isJumpPressed();
        boolean dash = controller.isDashPressed() && dashCooldown <= 0f;
        boolean skillKey = controller.isSkillUPressed();   // đọc 1 lần, rồi tách ra skill dưới đất / trên không
        boolean skillU = skillKey && stats.skillU != null && skillCooldown <= 0f;
        boolean airSkillU = skillKey && stats.airSkillU != null && !airSkillUsed && skillCooldown <= 0f;
        boolean defend = controller.isDefendHeld();   // phím GIỮ, đúng liên tục

        switch (state) {
            case FREE:
                // Ưu tiên khi bấm cùng lúc: lướt > nhảy > skill > đánh > thủ > đi
                if (dash) startDash();
                else if (jump) startJump();
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
        }

        updateEffects(delta);

        character.setFlipX(facingLeft);
        character.update(delta);

        float halfW = character.getRefWidth() / 2f;
        centerX = MathUtils.clamp(centerX, halfW, screenW - halfW);
        checkPlatformEdge();
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

    // ===================== PLATFORM =====================

    /**
     * Rời sàn hiện tại (platform): quy đổi airY về tính từ mặt đất chính, groundY về mặt đất chính.
     * Vị trí thật trên màn hình (groundY + airY) không đổi.
     */
    private void leaveFloor() {
        airY += groundY - mainGroundY;
        groundY = mainGroundY;
    }

    /** Đang đứng trên platform mà đi ra khỏi mép thì chuyển sang AIR và rơi. */
    private void checkPlatformEdge() {
        if (map == null || groundY == mainGroundY) return;
        boolean inAir = state == State.AIR || state == State.AIR_ATTACK || state == State.AIR_SKILL
            || (state == State.DASH && dashInAir);
        if (inAir) return;
        if (!map.hasPlatformAt(centerX, groundY)) {
            leaveFloor();
            velY = 0f;
            jumpsUsed = Math.max(jumpsUsed, 1);   // rơi khỏi mép tính như đã dùng 1 lần nhảy
            state = State.AIR;
            character.playAnimation("fall", FALL_FRAME_DURATION);
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

    /** Nghỉ sau đòn. Lướt / nhảy / skill hủy ngay; bấm J nối đòn nếu chưa phải đòn cuối; giữ S thì thủ. */
    private void updateRecovery(float delta, boolean atk, boolean jump, boolean dash,
                                boolean skillU, boolean defend) {
        boolean isLast = comboIndex == moves.length - 1;
        recoveryTimer -= delta;

        if (dash)   { comboIndex = 0; startDash(); return; }
        if (jump)   { comboIndex = 0; startJump(); return; }
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

    // ===================== THỦ =====================

    private void startDefend() {
        state = State.DEFEND;
        character.playFromStart("def", DEFEND_FRAME_DURATION);
    }

    /**
     * Giữ phím thì đứng im ở frame cuối của "def" (NORMAL mode tự giữ frame cuối).
     * Thả phím thì về FREE ngay. Không đi, không đổi hướng mặt khi đang thủ
     * (trừ lúc bị đánh từ phía sau, xem takeDamage()).
     */
    private void updateDefend(float delta, boolean defend) {
        if (!defend) {
            state = State.FREE;
            updateMovement(delta);   // tránh 1 frame còn giữ pose thủ
        }
    }

    /** Đang thủ hay không (để GameScreen / logic dame hỏi). */
    public boolean isDefending() {
        return state == State.DEFEND;
    }

    /**
     * Sát thương thực sự nhận vào sau khi tính thủ: thủ thì chỉ nhận stats.guardDamage,
     * không thủ thì nhận nguyên dame.
     */
    public int calcIncomingDamage(int rawDamage) {
        return state == State.DEFEND ? stats.guardDamage : rawDamage;
    }

    // ===================== NHẢY / RƠI / CHẠM ĐẤT =====================

    private void startJump() {
        leaveFloor();   // nhảy từ platform thì airY tính lại từ mặt đất chính
        jumpsUsed++;
        velY = stats.jumpSpeed;
        state = State.AIR;
        character.playFromStart("jump", JUMP_FRAME_DURATION);
    }

    /** Trên không: lướt, skill U, đánh, nhảy kép, đi ngang, chịu trọng lực. Chạm đất / platform thì sang LAND. */
    private void updateAir(float delta, boolean atk, boolean jump, boolean dash, boolean airSkillU) {
        if (dash && airDashesUsed < stats.maxAirDashes) { startDash(); return; }   // hết lượt lướt trên không thì bỏ qua
        // Ưu tiên: lướt > skill U > đánh trên không > nhảy kép
        if (airSkillU) { startAirSkill(); return; }
        // Đánh trên không chỉ được 1 lần mỗi lượt nhảy
        if (atk && stats.airMove != null && !airAttackUsed) { startAirAttack(); return; }
        if (jump && jumpsUsed < stats.maxJumps) startJump();

        moveHorizontal(delta);

        float prevFeet = groundY + airY;   // độ cao chân TRƯỚC khi áp trọng lực
        velY -= stats.gravity * delta;
        airY += velY * delta;

        // Hạ cánh lên platform: chỉ khi đang rơi và chân vừa xuyên qua mặt platform
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

    /** Reset các cờ trên không khi chạm đất (dùng chung cho LAND và KNOCKDOWN). */
    private void resetAirFlags() {
        velY = 0f;
        jumpsUsed = 0;
        airDashesUsed = 0;       // chạm đất / hạ cánh platform -> lướt trên không được lại
        airAttackUsed = false;   // chạm đất -> lượt nhảy sau được đánh / dùng skill lại
        airSkillUsed = false;
    }

    /** Chạm đất (từ rơi thường hoặc từ skill lao): reset trạng thái trên không rồi sang LAND. */
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

    /** Chạm đất không khóa: lướt / nhảy / skill / đánh / thủ / đi đều hủy được ngay. */
    private void updateLand(float delta, boolean atk, boolean jump, boolean dash,
                            boolean skillU, boolean defend) {
        if (dash) { startDash(); return; }
        if (jump) { startJump(); return; }
        if (skillU) { startSkill(stats.skillU); return; }
        if (atk && moves.length > 0) { startAttack(0); return; }
        if (defend) { startDefend(); return; }

        if (controller.getMoveDirection() != 0) {
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

    /**
     * Đang đánh trên không (khóa cứng): đứng yên tại chỗ, không áp trọng lực, không đi ngang.
     * velY và airY được giữ nguyên trong lúc đánh. Bấm U thì nhớ lại, tới khi qua
     * hitFrame + AIR_SKILL_CANCEL_DELAY_FRAMES thì ngắt sang skill U trên không (giống skill dưới đất).
     * Không bấm U, hết animation thì về AIR và rơi xuống: đang rơi thì rơi tiếp,
     * đang bay lên (velY > 0) thì bị ép về 0 để bắt đầu rơi.
     */
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

    /**
     * Đang tung skill trên không (khóa cứng). Fighter chỉ cập nhật thời gian rồi giao cho AirSkill.update()
     * quyết định di chuyển / bắn effect / gây dame, sau đó chép vị trí mới về.
     * Nếu skill đi xuống mà xuyên qua mặt platform thì dừng lại trên platform (coi như skill kết thúc).
     * Skill báo xong thì: đang ở đất / platform -> LAND, còn trên không -> AIR (rơi tiếp).
     */
    private void updateAirSkill(float delta) {
        airSkillTime += delta;

        // Đồng bộ lại vị trí (centerX có thể vừa bị clamp ở mép màn hình)
        airCtx.x = centerX;
        airCtx.airY = airY;
        airCtx.groundY = groundY;
        airCtx.time = airSkillTime;
        airCtx.frame = Math.min((int) (airSkillTime / airSkill.frameDuration), airSkillFrameCount - 1);
        airCtx.animDone = airSkillTime >= airSkillFrameCount * airSkill.frameDuration;

        float prevFeet = groundY + airY;   // độ cao chân TRƯỚC khi skill di chuyển

        boolean finished = airSkill.update(airCtx, delta);

        centerX = airCtx.x;
        airY = airCtx.airY;

        // Skill đang đi xuống mà xuyên qua mặt platform -> dừng lại trên platform
        boolean landedOnPlatform = false;
        if (map != null && groundY + airY < prevFeet) {
            float landY = map.findLandingY(centerX, prevFeet, groundY + airY);
            if (landY >= 0f) {
                groundY = landY;
                airY = 0f;
                landedOnPlatform = true;
            }
        }

        // Skill báo hitbox đang bật (c.hit) mỗi frame; chỉ tính dame 1 lần, trúng rồi thì thôi
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
                velY = 0f;          // rơi tiếp từ đứng yên
                state = State.AIR;
            }
        }
    }

    // ===================== LƯỚT =====================

    private void startDash() {
        if (state == State.DASH) return;
        dashInAir = (state == State.AIR);   // phải đọc TRƯỚC khi đổi state
        if (dashInAir) {
            velY = 0f;
            airDashesUsed++;                // tính 1 lần lướt trên không
        }

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

    // ===================== KNOCKOUT =====================

    /**
     * Bắt đầu bị knockout. Đang đứng (đất / platform) thì nằm luôn; đang trên không thì rơi xuống
     * (animation knockout vẫn chạy), chạm đất mới bắt đầu đếm KNOCKDOWN_LIE_TIME.
     */
    private void startKnockdown() {
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

    /** Chưa chạm đất: rơi theo trọng lực. Chạm đất rồi mới đếm giờ nằm, hết giờ thì đứng dậy. */
    private void updateKnockdown(float delta) {
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
            if (!knockGrounded && airY <= 0f) {
                airY = 0f;
                knockGrounded = true;
            }
            if (knockGrounded) {
                resetAirFlags();
                knockdownTimer = KNOCKDOWN_LIE_TIME;
            }
            return;
        }

        if (!KNOCKDOWN_ANIM_DONE) return;   // chưa xong animation thì chưa đếm giờ nằm
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

    /** Đang knockout / đứng dậy: không nhận dame, không điều khiển được. */
    public boolean isInvulnerable() {
        return state == State.KNOCKDOWN || state == State.GET_UP;
    }

    // ===================== GÂY / NHẬN DAME =====================

    /** Gắn đối thủ để đòn cận chiến biết đánh ai. Gọi trong GameScreen.show(). */
    public void setOpponent(Fighter opponent) {
        this.opponent = opponent;
    }

    /**
     * Khung của frame sprite đang vẽ (1:1 với asset, đã nhân scale).
     * Khớp với Character.draw(): không lật thì vẽ từ mép trái ô neo sang phải,
     * lật mặt thì vẽ từ mép phải ô neo sang trái.
     */
    private Rectangle spriteBox(Rectangle out) {
        float refHalf = character.getRefWidth() / 2f;
        float w = character.getCurrentFrameWidth();
        float h = character.getCurrentFrameHeight();
        float left = facingLeft ? centerX + refHalf - w : centerX - refHalf;
        out.set(left, getFeetY(), w, h);
        return out;
    }

    /** Hộp thân (bên nhận dame) = khung sprite hiện tại. */
    public Rectangle getHurtbox() {
        return spriteBox(hurtbox);
    }

    /** Hộp đánh (bên gây dame) = khung sprite hiện tại. */
    private Rectangle getAttackBox() {
        return spriteBox(attackBox);
    }

    /**
     * Hộp đánh hiện tại chạm đối thủ thì trừ máu. Trả true nếu trúng.
     * Đối thủ đang knockout / đứng dậy thì bỏ qua (không trúng, đòn không bị tiêu).
     */
    private boolean hitOpponent(int damage, boolean knockdown) {
        if (opponent == null || opponent.isInvulnerable()
            || !getAttackBox().overlaps(opponent.getHurtbox())) return false;
        opponent.takeDamage(damage, centerX, knockdown);
        return true;
    }

    /** Gọi 1 lần khi đòn thường tới frame gây dame. */
    private void doHit(Move m) {
        hitOpponent(m.damage, m.knockdown);
    }

    /** Gọi mỗi khi skill cận chiến tới 1 frame gây dame. Hit cuối knockout nếu skill bật knockdownOnLastHit. */
    private void doSkillHit(Skill s, int hitIndex) {
        boolean ko = s.knockdownOnLastHit && hitIndex == s.hitFrames.length - 1;
        hitOpponent(s.damage, ko);
    }

    /** Skill U trên không: trúng thì trả true (không gọi lại nữa), chưa trúng thì frame sau thử lại. */
    private boolean doAirSkillHit(int damage, boolean knockdown) {
        return hitOpponent(damage, knockdown);
    }

    /** Nhận dame, chỉ hit thường (giữ cho code cũ). */
    public void takeDamage(int rawDamage, float sourceX) {
        takeDamage(rawDamage, sourceX, false);
    }

    /**
     * Nhận dame (đã tính thủ). sourceX = tâm ngang của kẻ tấn công.
     * Đang knockout / đứng dậy: bỏ qua hoàn toàn.
     * Đang thủ: chỉ mất máu, không stun, không knockout, và nếu đòn đến từ phía sau thì quay mặt sang
     * phía đó (vẫn thủ).
     * Không thủ + knockdown: vào KNOCKDOWN (trên không thì rơi xuống).
     * Không thủ + hit thường: vào HIT, đứng yên (kể cả trên không) trong HIT_DURATION giây.
     */
    public void takeDamage(int rawDamage, float sourceX, boolean knockdown) {
        if (isInvulnerable()) return;

        hp = Math.max(0, hp - calcIncomingDamage(rawDamage));

        if (state == State.DEFEND) {
            if (sourceX < centerX) facingLeft = true;
            else if (sourceX > centerX) facingLeft = false;
            return;   // update() sẽ tự setFlipX(facingLeft) ở cuối frame
        }

        if (knockdown) {
            startKnockdown();
            return;
        }

        velY = 0f;
        comboIndex = 0;
        hitTimer = HIT_DURATION;
        state = State.HIT;
        int frames = animManager.get(charId, "hit", HIT_FRAME_DURATION).getKeyFrames().length;
        character.playFromStart("hit", HIT_DURATION / frames);
    }

    /** Bị stun: đứng yên, hết giờ thì về AIR (nếu đang trên không, updateAir sẽ chơi "fall") hoặc FREE. */
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

    // ===================== API CHO GAMESCREEN =====================

    /** Vẽ nhân vật. */
    public void draw(SpriteBatch batch) {
        character.draw(batch);
    }

    /** Vẽ effect của nhân vật này (gọi sau khi vẽ cả 2 nhân vật để effect nằm đè lên). */
    public void drawEffects(SpriteBatch batch) {
        for (int i = 0; i < effects.size; i++) effects.get(i).draw(batch);
    }

    /** Gắn ảnh marker (P1.png / P2.png) vẽ trên đầu nhân vật. Fighter sở hữu texture này. */
    public void setMarker(String path) {
        if (markerTex != null) markerTex.dispose();
        markerTex = new Texture(Gdx.files.internal(path));
        markerTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    /** Vẽ marker trên đầu, đi theo nhân vật (không lật theo hướng mặt). */
    public void drawMarker(SpriteBatch batch) {
        if (markerTex == null) return;
        float w = markerTex.getWidth() * MARKER_SCALE;
        float h = markerTex.getHeight() * MARKER_SCALE;
        float x = centerX - w / 2f;
        float y = getFeetY() + headHeight + MARKER_HEAD_OFFSET + MARKER_GAP;
        batch.draw(markerTex, x, y, w, h);
    }

    /** Effect đang sống, để GameScreen check va chạm với đối thủ. */
    public Array<Effect> getEffects() {
        return effects;
    }

    /** Đặt mặt đất chính của map (cũng là sàn đang đứng lúc mới đặt). */
    public void setGroundY(float groundY) {
        this.mainGroundY = groundY;
        this.groundY = groundY;
    }

    /** Gắn map để Fighter biết platform. Gọi 1 lần trong GameScreen.show(). */
    public void setMap(MapData map) {
        this.map = map;
    }

    /** Tâm ngang (pixel thế giới), để camera theo dõi. */
    public float getCenterX() {
        return centerX;
    }

    /** Độ cao chân (pixel thế giới) = sàn đang đứng + độ cao trên không, để camera theo dõi. */
    public float getFeetY() {
        return groundY + airY;
    }

    /** Giải phóng Texture của effect còn sống và marker. Gọi khi thoát trận. */
    public void dispose() {
        for (Effect e : effects) e.cancel();
        effects.clear();
        if (markerTex != null) {
            markerTex.dispose();
            markerTex = null;
        }
    }
}
