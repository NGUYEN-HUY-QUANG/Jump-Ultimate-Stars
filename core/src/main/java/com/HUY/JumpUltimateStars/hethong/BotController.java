package com.HUY.JumpUltimateStars.hethong;

import com.HUY.JumpUltimateStars.character.Fighter;
import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.map.MapData;
import com.badlogic.gdx.math.MathUtils;

/**
 * Bot đơn giản, có 3 mức độ khó (xem enum Difficulty).
 * Cứ mỗi reactMin ~ reactMax giây thì "suy nghĩ" 1 lần, random 1 hành động theo khoảng cách.
 *
 * Biết xử lý khi đối thủ đứng khác tầng (platform):
 *  - Đối thủ ở trên: đi tới dưới chân đối thủ rồi nhảy.
 *  - Đối thủ ở dưới: đi tới mép platform (mép không sát biên map) để rớt xuống.
 *  - Khác tầng thì không đánh cận chiến, chỉ bắn skill / ultimate.
 */
public class BotController extends PlayerController {

    /** Mỗi mức độ khó là 1 bộ số. Muốn tinh chỉnh thì sửa số ở đây. */
    public enum Difficulty {
        //       phản ứng(s)    gần: đánh  skill  thủ   | ultimate gần/giữa | giữa: skill
        EASY  (0.30f, 0.60f,    0.45f, 0.10f, 0.05f,    0.05f, 0.00f,        0.15f),
        NORMAL(0.10f, 0.25f,    0.60f, 0.10f, 0.10f,    0.20f, 0.10f,        0.25f),
        HARD  (0.04f, 0.10f,    0.65f, 0.15f, 0.12f,    0.45f, 0.30f,        0.35f);

        final float reactMin, reactMax;
        final float nearAttack, nearSkill, nearDefend;   // phần còn lại của tầm gần là nhảy
        final float ultNear, ultMid;                     // xác suất dùng ultimate khi đủ 1 thanh
        final float midSkill;                            // xác suất bắn skill ở tầm giữa

        Difficulty(float reactMin, float reactMax,
                   float nearAttack, float nearSkill, float nearDefend,
                   float ultNear, float ultMid, float midSkill) {
            this.reactMin = reactMin;
            this.reactMax = reactMax;
            this.nearAttack = nearAttack;
            this.nearSkill = nearSkill;
            this.nearDefend = nearDefend;
            this.ultNear = ultNear;
            this.ultMid = ultMid;
            this.midSkill = midSkill;
        }
    }

    private static final float STOP_RANGE = 60f;     // gần hơn mức này thì đứng lại
    private static final float ATTACK_RANGE = 110f;  // trong tầm này thì đánh cận chiến
    private static final float MID_RANGE = 350f;     // tầm trung: skill / ultimate / lướt

    // ===== Khác tầng (platform) =====
    private static final float HEIGHT_DIFF = 60f;     // chênh độ cao chân quá mức này = khác tầng
    private static final float JUMP_RANGE = 200f;     // đối thủ ở trên: đứng gần hơn mức này (theo chiều ngang) thì nhảy
    private static final float JUMP_COOLDOWN = 0.6f;  // nhảy lại sau bao lâu (giây)

    private final Difficulty diff;

    private Fighter self;
    private Fighter target;
    private MapData map;

    private float thinkTimer;
    private float defendTimer;

    // Theo dõi độ cao để biết ai đang đứng yên trên sàn (không nhảy / rơi)
    private float lastSelfFeet = Float.NaN;
    private float lastTargetFeet = Float.NaN;
    private float jumpTimer;
    private int dropDir = 1;
    private boolean sameLevel = true;

    private int moveDir;
    // Các phím "bấm": chỉ đúng 1 frame, đầu mỗi update() sẽ reset về false
    private boolean attack, jump, dash, skillU, ultimate;

    public BotController() {
        this(Difficulty.NORMAL);
    }

    public BotController(Difficulty diff) {
        super(0, 0, 0, 0, 0, 0, 0, 0);   // mã phím vô nghĩa vì mọi hàm đọc phím đã bị override
        this.diff = diff;
    }

    public void setSelf(Fighter self) { this.self = self; }
    public void setTarget(Fighter target) { this.target = target; }
    public void setMap(MapData map) { this.map = map; }

    /** GameScreen gọi mỗi frame, TRƯỚC khi fighter.update(). */
    public void update(float delta) {
        attack = jump = dash = skillU = ultimate = false;
        if (self == null || target == null) return;

        float dx = target.getCenterX() - self.getCenterX();
        float dist = Math.abs(dx);
        int toward = dx > 0 ? 1 : -1;

        if (defendTimer > 0f) defendTimer -= delta;
        if (jumpTimer > 0f) jumpTimer -= delta;

        // --- Ai đang đứng yên theo chiều dọc? (feetY không đổi so với frame trước) ---
        float selfFeet = self.getFeetY();
        float targetFeet = target.getFeetY();
        boolean stable = !Float.isNaN(lastSelfFeet)
            && Math.abs(selfFeet - lastSelfFeet) < 0.01f
            && Math.abs(targetFeet - lastTargetFeet) < 0.01f;
        lastSelfFeet = selfFeet;
        lastTargetFeet = targetFeet;

        float dy = targetFeet - selfFeet;   // dương = đối thủ ở TRÊN bot
        boolean targetHigher = stable && dy > HEIGHT_DIFF;
        boolean targetLower = stable && dy < -HEIGHT_DIFF;
        // Đang nhảy / rơi (không stable) thì giữ nguyên nhận định của frame trước
        if (stable) sameLevel = !(targetHigher || targetLower);

        // --- Di chuyển ---
        if (defendTimer > 0f) {
            moveDir = 0;
        } else if (targetHigher) {
            // Đối thủ ở trên: đi tới dưới chân đối thủ rồi nhảy
            moveDir = dist > 15f ? toward : 0;
            if (dist < JUMP_RANGE && jumpTimer <= 0f) {
                jump = true;
                jumpTimer = JUMP_COOLDOWN;
            }
        } else if (targetLower) {
            // Đối thủ ở dưới: đi tới đúng mép platform để rớt xuống
            float[] range = map != null ? map.getPlatformRange(self.getCenterX(), selfFeet) : null;
            if (range != null) {
                // Mép sát biên map thì không rớt được (nhân vật bị chặn lại), nên bỏ mép đó
                boolean leftOk = range[0] > 1f;
                boolean rightOk = range[1] < map.getWorldWidth() - 1f;
                float tx = target.getCenterX();

                if (leftOk && rightOk) {
                    // Cả 2 mép đều rớt được: chọn mép gần đối thủ hơn
                    dropDir = Math.abs(range[0] - tx) < Math.abs(range[1] - tx) ? -1 : 1;
                } else if (leftOk) {
                    dropDir = -1;
                } else if (rightOk) {
                    dropDir = 1;
                }
            }
            moveDir = dropDir;   // không tìm được platform thì giữ hướng cũ
        } else {
            // Cùng tầng (hoặc đang nhảy / rơi): đi tới như bình thường
            moveDir = dist > STOP_RANGE ? toward : 0;
            dropDir = toward;
        }

        // --- Suy nghĩ ---
        thinkTimer -= delta;
        if (thinkTimer > 0f) return;
        thinkTimer = MathUtils.random(diff.reactMin, diff.reactMax);
        think(dist);
    }

    private void think(float dist) {
        boolean hasBar = self.getEnergy() >= CharacterStats.ENERGY_PER_BAR;
        float r = MathUtils.random();

        // Khác tầng: không đánh cận chiến (sẽ hụt), chỉ bắn skill / ultimate
        if (!sameLevel) {
            if (hasBar && MathUtils.random() < diff.ultMid) ultimate = true;
            else if (r < diff.midSkill) skillU = true;
            return;
        }

        if (dist <= ATTACK_RANGE) {
            // Gần
            if (hasBar && MathUtils.random() < diff.ultNear) { ultimate = true; return; }

            float t = diff.nearAttack;
            if (r < t)                           attack = true;
            else if (r < (t += diff.nearSkill))  skillU = true;
            else if (r < (t += diff.nearDefend)) defendTimer = MathUtils.random(0.3f, 0.7f);
            else                                 jump = true;

        } else if (dist <= MID_RANGE) {
            // Tầm giữa
            if (hasBar && MathUtils.random() < diff.ultMid) { ultimate = true; return; }

            if (r < diff.midSkill)              skillU = true;
            else if (r < diff.midSkill + 0.10f) jump = true;
            else if (r < diff.midSkill + 0.20f) dash = true;

        } else {
            // Xa
            if (r < 0.30f)      dash = true;
            else if (r < 0.40f) jump = true;
        }
    }

    // ===== Override: trả kết quả bot đã quyết định thay vì đọc phím =====
    @Override public int getMoveDirection()      { return moveDir; }
    @Override public boolean isAttackPressed()   { return attack; }
    @Override public boolean isJumpPressed()     { return jump; }
    @Override public boolean isDashPressed()     { return dash; }
    @Override public boolean isSkillUPressed()   { return skillU; }
    @Override public boolean isUltimatePressed() { return ultimate; }
    @Override public boolean isDefendHeld()      { return defendTimer > 0f; }
}
