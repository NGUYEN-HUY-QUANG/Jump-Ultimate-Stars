package com.HUY.JumpUltimateStars.ui;

import com.HUY.JumpUltimateStars.ui.FrameAnim;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Điều phối luồng trận: ROUND -> FIGHT -> PLAYING -> KO -> ZOOM -> WINNER -> (round mới | hết trận).
 *
 * ZOOM : camera zoom vào người thắng. Chờ GameScreen báo người thắng đã rảnh (hết chiêu, đứng yên),
 *        rồi đợi thêm WIN_DELAY giây thì chuyển sang WINNER và yêu cầu chạy pose winner.
 * WINNER: chữ winner + pose winner chạy cùng lúc, cả hai xong mới kết thúc round.
 *
 * Không biết gì về Fighter. GameScreen báo qua onKnockout() và setWinnerStatus(), hỏi lại qua consumeWinPose().
 */
public class RoundManager {

    public enum Phase { ROUND, FIGHT, PLAYING, KO, ZOOM, WINNER }

    private static final int WINS_NEEDED = 2;
    private static final int MAX_ROUNDS = 3;

    private static final String NUM_PATH = "HUD/round/";   // + "1.png", "2.png", "3.png"

    // ===================== THỜI GIAN (giây) =====================
    private static final float ROUND_FRAME = 0.07f;   // tốc độ chạy chữ ROUND (lớn = chậm)
    private static final float FIGHT_FRAME = 0.07f;
    private static final float KO_FRAME = 0.07f;
    private static final float WINNER_FRAME = 0.15f;

    private static final float ROUND_HOLD = 1.4f;     // giữ "ROUND + số" sau khi chữ chạy xong
    private static final float KO_HOLD = 1.2f;        // giữ chữ K.O sau khi chạy xong
    private static final float WIN_DELAY = 1.0f;      // từ lúc người thắng rảnh tới lúc chạy pose winner

    // ===================== KÍCH THƯỚC (nhân với màn 720p) =====================
    private static final float ROUND_SCALE = 1.8f;
    private static final float FIGHT_SCALE = 1.4f;
    private static final float KO_SCALE = 1.8f;
    private static final float WINNER_SCALE = 1.6f;
    private static final float ROUND_GAP = 15f;       // khoảng cách chữ ROUND và số (nhân theo ROUND_SCALE)

    // ===================== VỊ TRÍ: lệch so với tâm màn hình (pixel 720p, X dương = phải, Y dương = lên) =====================
    private static final float ROUND_OFFSET_X = 0f,  ROUND_OFFSET_Y = 0f;
    private static final float FIGHT_OFFSET_X = 0f,  FIGHT_OFFSET_Y = 0f;
    private static final float KO_OFFSET_X = 0f,     KO_OFFSET_Y = 0f;
    private static final float WINNER_OFFSET_X = 0f, WINNER_OFFSET_Y = 250f;

    private final FrameAnim roundAnim = new FrameAnim("HUD/round", "round_", ROUND_FRAME);
    private final FrameAnim fightAnim = new FrameAnim("HUD/fight", "", FIGHT_FRAME);
    private final FrameAnim koAnim = new FrameAnim("HUD/KO", "", KO_FRAME);
    private final FrameAnim winnerAnim = new FrameAnim("HUD/winner", "", WINNER_FRAME);
    private final Texture[] nums = new Texture[MAX_ROUNDS];

    private final OrthographicCamera camera = new OrthographicCamera();
    private float screenW, screenH, scale = 1f;

    private Phase phase;
    private int round = 1;
    private int p1Wins, p2Wins;
    private boolean winnerIsP1;
    private float timer;
    private boolean nextRound, matchOver;

    // GameScreen báo mỗi frame: người thắng đã rảnh chưa, pose winner đã chạy xong chưa
    private boolean winnerReady, winPoseDone;
    // Bật 1 lần khi tới lúc chạy pose winner
    private boolean winPoseRequested;

    public RoundManager() {
        for (int i = 0; i < MAX_ROUNDS; i++) nums[i] = new Texture(NUM_PATH + (i + 1) + ".png");
        startRound();
    }

    private void startRound() {
        phase = Phase.ROUND;
        timer = ROUND_HOLD;
        roundAnim.start();
    }

    /** GameScreen gọi khi 1 bên hết máu (chỉ có tác dụng lúc đang PLAYING). */
    public void onKnockout(boolean p1Lost) {
        if (phase != Phase.PLAYING) return;
        winnerIsP1 = !p1Lost;
        if (winnerIsP1) p1Wins++; else p2Wins++;
        phase = Phase.KO;
        timer = KO_HOLD;
        koAnim.start();
    }

    /** GameScreen gọi mỗi frame (trước update): người thắng đã rảnh chưa, pose winner đã xong chưa. */
    public void setWinnerStatus(boolean ready, boolean poseDone) {
        winnerReady = ready;
        winPoseDone = poseDone;
    }

    public void update(float delta) {
        switch (phase) {
            case ROUND:
                roundAnim.update(delta);
                if (roundAnim.isFinished()) {
                    timer -= delta;
                    if (timer <= 0f) { phase = Phase.FIGHT; fightAnim.start(); }
                }
                break;
            case FIGHT:
                fightAnim.update(delta);
                if (fightAnim.isFinished()) phase = Phase.PLAYING;
                break;
            case KO:
                koAnim.update(delta);
                if (koAnim.isFinished()) {
                    timer -= delta;
                    if (timer <= 0f) { phase = Phase.ZOOM; timer = WIN_DELAY; }
                }
                break;
            case ZOOM:
                // Chỉ đếm giờ khi người thắng đã rảnh (hết chiêu đang ra)
                if (winnerReady) {
                    timer -= delta;
                    if (timer <= 0f) {
                        phase = Phase.WINNER;
                        winnerAnim.start();
                        winPoseRequested = true;
                    }
                }
                break;
            case WINNER:
                if (matchOver) break;
                winnerAnim.update(delta);
                // Chữ winner VÀ pose winner đều xong mới đi tiếp
                if (winnerAnim.isFinished() && winPoseDone) {
                    if (p1Wins >= WINS_NEEDED || p2Wins >= WINS_NEEDED) {
                        matchOver = true;
                    } else {
                        round = Math.min(round + 1, MAX_ROUNDS);
                        nextRound = true;
                        startRound();
                    }
                }
                break;
            default: break;
        }
    }

    public void resize(int w, int h) {
        screenW = w; screenH = h;
        camera.setToOrtho(false, w, h);
        scale = h / 720f;
    }

    /** Vẽ hiệu ứng (toạ độ màn hình). Gọi SAU HUD. */
    public void render(SpriteBatch batch) {
        float cx = screenW / 2f, cy = screenH / 2f;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        switch (phase) {
            case ROUND: {
                float s = scale * ROUND_SCALE;
                Texture num = nums[round - 1];
                float rw = roundAnim.lastWidth() * s, gap = ROUND_GAP * s;
                float nw = num.getWidth() * s, nh = num.getHeight() * s;
                float x0 = cx + ROUND_OFFSET_X * scale;
                float y0 = cy + ROUND_OFFSET_Y * scale;
                float left = x0 - (rw + gap + nw) / 2f;   // canh sẵn cả cụm nên chữ ROUND không bị xê dịch
                roundAnim.drawCentered(batch, left + rw / 2f, y0, s);
                if (roundAnim.isFinished()) batch.draw(num, left + rw + gap, y0 - nh / 2f, nw, nh);
                break;
            }
            case FIGHT:
                fightAnim.drawCentered(batch, cx + FIGHT_OFFSET_X * scale, cy + FIGHT_OFFSET_Y * scale,
                    scale * FIGHT_SCALE);
                break;
            case KO:
                koAnim.drawCentered(batch, cx + KO_OFFSET_X * scale, cy + KO_OFFSET_Y * scale,
                    scale * KO_SCALE);
                break;
            case WINNER:
                winnerAnim.drawCentered(batch, cx + WINNER_OFFSET_X * scale, cy + WINNER_OFFSET_Y * scale,
                    scale * WINNER_SCALE);
                break;
            default: break;
        }
        batch.end();
    }

    // ===== Trạng thái cho GameScreen =====
    public boolean isPlaying() { return phase == Phase.PLAYING; }
    public boolean isHudVisible() { return phase != Phase.ZOOM && phase != Phase.WINNER; }
    public boolean isZoomPhase() { return phase == Phase.ZOOM || phase == Phase.WINNER; }
    public boolean isWinnerP1() { return winnerIsP1; }
    public int getP1Wins() { return p1Wins; }
    public int getP2Wins() { return p2Wins; }
    public boolean isMatchOver() { return matchOver; }

    /** Trả true đúng 1 lần khi cần reset sang round mới. */
    public boolean consumeNextRound() {
        boolean r = nextRound;
        nextRound = false;
        return r;
    }

    /** Trả true đúng 1 lần khi tới lúc người thắng chạy pose winner. */
    public boolean consumeWinPose() {
        boolean r = winPoseRequested;
        winPoseRequested = false;
        return r;
    }

    public void dispose() {
        roundAnim.dispose(); fightAnim.dispose(); koAnim.dispose(); winnerAnim.dispose();
        for (Texture t : nums) t.dispose();
    }
}
