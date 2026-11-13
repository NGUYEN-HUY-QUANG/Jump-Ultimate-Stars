package com.HUY.JumpUltimateStars.ui;

import com.HUY.JumpUltimateStars.character.stats.CharacterStatsRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;

/**
 * HUD hàng trên cùng của trận đấu, vẽ theo toạ độ MÀN HÌNH (không theo camera thế giới).
 *
 * Bố cục từ trái sang phải (P2 là bản lật ngược của P1):
 *
 *   [khung+avatar P1][ hp_bar P1 ][ time_bar ][ hp_bar P2 ][khung+avatar P2]
 *
 * Thanh máu gồm 2 rect nằm dưới hp_bar.png (hp_bar.png và khung đè lên):
 *   - rect ĐỎ  : dài tới mức "máu trễ" (trail), tụt dần về máu thật sau TRAIL_DELAY giây
 *   - rect XANH: dài tới máu thật, vẽ đè lên đỏ (phần đỏ lộ ra = máu vừa mất)
 * Số máu còn lại nằm sát mép phải của thanh P1 (trừ HP_TEXT_PAD), P2 đối xứng (mép trái).
 *
 * Góc dưới màn hình: khung energy của mỗi bên (mới vẽ khung, chưa có logic).
 *
 * Cách dùng trong GameScreen:
 *   show():    hud = new HUD(p1Id, p2Id, p1Avatar, p2Avatar); hud.resize(w, h);
 *   render():  hud.update(delta); ... vẽ SAU CÙNG: hud.render(batch);
 *   resize():  hud.resize(width, height);
 *   dispose(): hud.dispose();
 *   Khi máu đổi: hud.setP1Hp(hp) / hud.setP2Hp(hp)
 */
public class HUD {

    // ===================== ĐƯỜNG DẪN =====================

    private static final String FRAME_PATH = "HUD/avatar.png";
    private static final String HP_BAR_PATH = "HUD/hp_bar.png";
    private static final String TIME_BAR_PATH = "HUD/time_bar.png";
    private static final String TIME_OVERLAY_PATH = "HUD/time.png";
    private static final String ENERGY_ICON_PATH = "HUD/energy.png";
    private static final String ENERGY_BAR_PATH = "HUD/energy_bar.png";

    // ===================== BỐ CỤC CHUNG =====================

    private static final float WIDTH_RATIO = 1f;
    private static final float MARGIN_X = 8f;
    private static final float MARGIN_TOP = 8f;
    private static final float MAX_HEIGHT_RATIO = 0.16f;
    private static final float HUD_SCALE_MULT = 1.2f;

    // ===================== CHỈNH THANH MÁU & time.png =====================

    private static final float HP_STRETCH_X = 1f;

    private static final float FRAME_OFFSET_X = 0f;
    private static final float FRAME_OFFSET_Y = 0f;

    private static final float HP_OFFSET_X = 0f;
    private static final float HP_OFFSET_Y = -5f;

    private static final float TIME_OFFSET_X = 0f;
    private static final float TIME_OFFSET_Y = -5f;

    private static final float TIME_OVERLAY_OFFSET_X = 0f;
    private static final float TIME_OVERLAY_OFFSET_Y = 0f;

    // ===================== MÁU (2 RECT + SỐ) =====================

    private static final Color HP_FILL_COLOR = new Color(0.2f, 0.8f, 0.2f, 1f);    // xanh: máu thật
    private static final Color HP_TRAIL_COLOR = new Color(0.85f, 0.15f, 0.15f, 1f); // đỏ: máu vừa mất

    /** Chờ bao lâu sau lần dính dame cuối thì đỏ bắt đầu tụt (giây). */
    private static final float TRAIL_DELAY = 0.4f;
    /** Tốc độ đỏ tụt, tính theo % maxHp mỗi giây (1 = hết cả thanh trong 1s). */
    private static final float TRAIL_SPEED = 0.8f;

    /** Số máu cách mép (pixel màn hình): P1 cách mép phải của thanh, P2 cách mép trái. */
    private static final float HP_TEXT_PAD = 20f;
    /** Chiều cao chữ so với chiều cao thanh máu (0.4 = 40%). */
    private static final float HP_TEXT_RATIO = 0.4f;

    // ===================== ENERGY (góc dưới) =====================

    private static final float ENERGY_MARGIN = 12f;

    // ===================== DỮ LIỆU =====================

    private final Texture frameTex;
    private final Texture hpTex;
    private final Texture timeTex;
    private final Texture timeOverlayTex;
    private final Texture energyIconTex;
    private final Texture energyBarTex;
    private final Texture whitePx;      // 1 pixel trắng để vẽ rect
    private final Texture avatar1;
    private final Texture avatar2;      // null khi chơi 1 người

    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout glyph = new GlyphLayout();

    private final float maxHp1;
    private final float maxHp2;
    private float hp1;
    private float hp2;
    private float trail1, trail2;           // máu trễ (đơn vị HP)
    private float trailTimer1, trailTimer2; // đồng hồ chờ trước khi đỏ tụt

    private final OrthographicCamera camera = new OrthographicCamera();

    private float frameW, frameH, hpW, hpH, timeW, timeH;
    private float overlayW, overlayH;
    private float frameY, hpY, timeY, overlayY;
    private float p1FrameX, p1BarX, timeX, p2BarX, p2FrameX, overlayX;

    // Energy
    private float eIconW, eIconH, eBarW, eBarH, eIconY, eBarY;
    private float p1EIconX, p1EBarX, p2EIconX, p2EBarX;

    public HUD(String p1Id, String p2Id, TextureRegion p1Avatar, TextureRegion p2Avatar) {
        frameTex = new Texture(Gdx.files.internal(FRAME_PATH));
        frameTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        hpTex = new Texture(Gdx.files.internal(HP_BAR_PATH));
        timeTex = new Texture(Gdx.files.internal(TIME_BAR_PATH));
        timeOverlayTex = new Texture(Gdx.files.internal(TIME_OVERLAY_PATH));
        energyIconTex = new Texture(Gdx.files.internal(ENERGY_ICON_PATH));
        energyIconTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        energyBarTex = new Texture(Gdx.files.internal(ENERGY_BAR_PATH));

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        whitePx = new Texture(px);
        px.dispose();

        FileHandle frameFile = Gdx.files.internal(FRAME_PATH);
        avatar1 = buildAvatar(frameFile, p1Avatar, false);
        avatar2 = p2Id != null ? buildAvatar(frameFile, p2Avatar, true) : null;

        maxHp1 = CharacterStatsRegistry.get(p1Id).maxHp;
        maxHp2 = p2Id != null ? CharacterStatsRegistry.get(p2Id).maxHp : 1f;
        hp1 = maxHp1;
        hp2 = maxHp2;
        trail1 = hp1;
        trail2 = hp2;
    }

    // ===================== API =====================

    /** Đặt máu hiện tại của P1 (tự kẹp trong 0..maxHp). */
    public void setP1Hp(float hp) {
        float nh = MathUtils.clamp(hp, 0f, maxHp1);
        if (nh < hp1) trailTimer1 = TRAIL_DELAY;   // vừa dính dame -> reset chờ
        if (nh > trail1) trail1 = nh;              // hồi máu: kéo đỏ lên theo
        hp1 = nh;
    }

    /** Đặt máu hiện tại của P2 (tự kẹp trong 0..maxHp). */
    public void setP2Hp(float hp) {
        float nh = MathUtils.clamp(hp, 0f, maxHp2);
        if (nh < hp2) trailTimer2 = TRAIL_DELAY;
        if (nh > trail2) trail2 = nh;
        hp2 = nh;
    }

    /** Gọi mỗi frame (trước render) để rect đỏ tụt dần về máu thật. */
    public void update(float delta) {
        trailTimer1 -= delta;
        if (trailTimer1 <= 0f && trail1 > hp1) {
            trail1 = Math.max(hp1, trail1 - TRAIL_SPEED * maxHp1 * delta);
        }
        trailTimer2 -= delta;
        if (trailTimer2 <= 0f && trail2 > hp2) {
            trail2 = Math.max(hp2, trail2 - TRAIL_SPEED * maxHp2 * delta);
        }
    }

    /** Gọi khi cửa sổ đổi kích thước (và 1 lần trong show()). */
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        layout(width, height);
    }

    /** Vẽ HUD. Tự begin/end batch và tự đặt projection theo màn hình. */
    public void render(SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        drawSide(batch, avatar1, p1FrameX, p1BarX, false, hp1, trail1, maxHp1);
        if (avatar2 != null) drawSide(batch, avatar2, p2FrameX, p2BarX, true, hp2, trail2, maxHp2);

        batch.draw(timeTex, timeX, timeY, timeW, timeH);
        batch.draw(timeOverlayTex, overlayX, overlayY, overlayW, overlayH);

        drawEnergyFrame(batch, p1EIconX, p1EBarX, false);
        if (avatar2 != null) drawEnergyFrame(batch, p2EIconX, p2EBarX, true);

        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        frameTex.dispose();
        hpTex.dispose();
        timeTex.dispose();
        timeOverlayTex.dispose();
        energyIconTex.dispose();
        energyBarTex.dispose();
        whitePx.dispose();
        font.dispose();
        avatar1.dispose();
        if (avatar2 != null) avatar2.dispose();
    }

    // ===================== BỐ CỤC =====================

    private void layout(int screenW, int screenH) {
        float fw = frameTex.getWidth(), fh = frameTex.getHeight();
        float hw = hpTex.getWidth() * HP_STRETCH_X, hh = hpTex.getHeight();
        float tw = timeTex.getWidth(), th = timeTex.getHeight();

        float totalW = 2f * (fw + hw) + tw;
        float maxH = Math.max(fh, Math.max(hh, th));

        float scale = Math.min(
            (screenW - 2f * MARGIN_X) / totalW,
            screenH * MAX_HEIGHT_RATIO / maxH
        );

        scale = Math.min(scale * HUD_SCALE_MULT, scale);

        frameW = fw * scale; frameH = fh * scale;
        hpW = hw * scale;    hpH = hh * scale;
        timeW = tw * scale;  timeH = th * scale;
        overlayW = timeOverlayTex.getWidth() * scale;
        overlayH = timeOverlayTex.getHeight() * scale;

        float rowH = maxH * scale;
        float rowY = screenH - MARGIN_TOP - rowH;
        frameY = rowY + (rowH - frameH) / 2f + FRAME_OFFSET_Y * scale;
        hpY = rowY + (rowH - hpH) / 2f + HP_OFFSET_Y * scale;
        timeY = rowY + (rowH - timeH) / 2f + TIME_OFFSET_Y * scale;

        float left = (screenW - totalW * scale) / 2f;
        float baseP1Frame = left;
        float baseP1Bar = baseP1Frame + frameW;
        float baseTime = baseP1Bar + hpW;
        float baseP2Bar = baseTime + timeW;
        float baseP2Frame = baseP2Bar + hpW;

        p1FrameX = baseP1Frame + FRAME_OFFSET_X * scale;
        p2FrameX = baseP2Frame - FRAME_OFFSET_X * scale;
        p1BarX = baseP1Bar + HP_OFFSET_X * scale;
        p2BarX = baseP2Bar - HP_OFFSET_X * scale;
        timeX = baseTime + TIME_OFFSET_X * scale;

        overlayX = timeX + (timeW - overlayW) / 2f + TIME_OVERLAY_OFFSET_X * scale;
        overlayY = timeY + (timeH - overlayH) / 2f + TIME_OVERLAY_OFFSET_Y * scale;

        // Energy
        eIconW = energyIconTex.getWidth() * scale;  eIconH = energyIconTex.getHeight() * scale;
        eBarW = energyBarTex.getWidth() * scale;    eBarH = energyBarTex.getHeight() * scale;
        float eRowH = Math.max(eIconH, eBarH);
        eIconY = ENERGY_MARGIN + (eRowH - eIconH) / 2f;
        eBarY = ENERGY_MARGIN + (eRowH - eBarH) / 2f;

        p1EIconX = ENERGY_MARGIN;                      p1EBarX = p1EIconX + eIconW;
        p2EIconX = screenW - ENERGY_MARGIN - eIconW;   p2EBarX = p2EIconX - eBarW;

        // Cỡ chữ số máu theo chiều cao thanh máu
        font.getData().setScale(1f);
        font.getData().setScale(hpH * HP_TEXT_RATIO / font.getCapHeight());
    }

    // ===================== VẼ =====================

    /**
     * Vẽ 1 bên: avatar -> rect đỏ -> rect xanh -> hp_bar.png -> khung avatar -> số máu.
     * mirror = true cho P2. Máu mất từ mép NGOÀI vào, mép trong (sát time_bar) cố định.
     * Rect to bằng hp_bar.png và nằm đúng chỗ hp_bar.png.
     */
    private void drawSide(SpriteBatch batch, Texture avatar, float frameX, float barX,
                          boolean mirror, float hp, float trail, float maxHp) {
        // Avatar
        batch.setColor(Color.WHITE);
        batch.draw(avatar, frameX, frameY, frameW, frameH);

        float hpRatio = MathUtils.clamp(hp / maxHp, 0f, 1f);
        float trailRatio = MathUtils.clamp(trail / maxHp, 0f, 1f);

        // Rect đỏ: tới mức máu trễ
        float trailW = hpW * trailRatio;
        batch.setColor(HP_TRAIL_COLOR);
        batch.draw(whitePx, mirror ? barX : barX + hpW - trailW, hpY, trailW, hpH);

        // Rect xanh: tới mức máu thật, đè lên đỏ
        float curW = hpW * hpRatio;
        batch.setColor(HP_FILL_COLOR);
        batch.draw(whitePx, mirror ? barX : barX + hpW - curW, hpY, curW, hpH);

        // hp_bar.png rồi khung avatar đè lên các rect
        batch.setColor(Color.WHITE);
        batch.draw(hpTex, barX, hpY, hpW, hpH, 0, 0, hpTex.getWidth(), hpTex.getHeight(), mirror, false);
        batch.draw(frameTex, frameX, frameY, frameW, frameH,
            0, 0, frameTex.getWidth(), frameTex.getHeight(), mirror, false);

        // Số máu còn lại: P1 sát mép phải thanh (trừ HP_TEXT_PAD), P2 sát mép trái (cộng HP_TEXT_PAD)
        glyph.setText(font, String.valueOf((int) Math.ceil(hp)));
        float tx = mirror ? barX + HP_TEXT_PAD : barX + hpW - HP_TEXT_PAD - glyph.width;
        float ty = hpY + (hpH + glyph.height) / 2f;
        font.setColor(Color.WHITE);
        font.draw(batch, glyph, tx, ty);
    }

    private void drawEnergyFrame(SpriteBatch batch, float iconX, float barX, boolean mirror) {
        batch.setColor(Color.WHITE);
        batch.draw(energyIconTex, iconX, eIconY, eIconW, eIconH,
            0, 0, energyIconTex.getWidth(), energyIconTex.getHeight(), mirror, false);
        batch.draw(energyBarTex, barX, eBarY, eBarW, eBarH,
            0, 0, energyBarTex.getWidth(), energyBarTex.getHeight(), mirror, false);
    }

    // ===================== AVATAR ĐỤC THEO KHUNG =====================

    private static Texture buildAvatar(FileHandle frameFile, TextureRegion avatar, boolean mirror) {
        Pixmap frame = new Pixmap(frameFile);

        TextureData td = avatar.getTexture().getTextureData();
        if (!td.isPrepared()) td.prepare();
        Pixmap src = td.consumePixmap();
        boolean disposeSrc = td.disposePixmap();
        int rx = avatar.getRegionX(), ry = avatar.getRegionY();
        int rw = avatar.getRegionWidth(), rh = avatar.getRegionHeight();
        int W = frame.getWidth(), H = frame.getHeight();

        boolean[] outside = new boolean[W * H];
        IntArray stack = new IntArray();
        for (int x = 0; x < W; x++) {
            pushOutside(frame, outside, stack, x, 0, W);
            pushOutside(frame, outside, stack, x, H - 1, W);
        }
        for (int y = 0; y < H; y++) {
            pushOutside(frame, outside, stack, 0, y, W);
            pushOutside(frame, outside, stack, W - 1, y, W);
        }
        while (stack.size > 0) {
            int p = stack.pop();
            int x = p % W, y = p / W;
            if (x > 0)     pushOutside(frame, outside, stack, x - 1, y, W);
            if (x < W - 1) pushOutside(frame, outside, stack, x + 1, y, W);
            if (y > 0)     pushOutside(frame, outside, stack, x, y - 1, W);
            if (y < H - 1) pushOutside(frame, outside, stack, x, y + 1, W);
        }

        int cw = rw, ch = rh;
        float aspect = (float) W / H;
        if (aspect > 1f) ch = Math.round(cw / aspect); else cw = Math.round(ch * aspect);
        int sx = rx + (rw - cw) / 2;
        int sy = ry + (rh - ch) / 2 + Math.round(rh * 0.06f);
        sx = MathUtils.clamp(sx, rx, rx + rw - cw);
        sy = MathUtils.clamp(sy, ry, ry + rh - ch);

        Pixmap cur = new Pixmap(cw, ch, Pixmap.Format.RGBA8888);
        cur.drawPixmap(src, 0, 0, sx, sy, cw, ch);
        while (cur.getWidth() >= W * 2 && cur.getHeight() >= H * 2) {
            Pixmap half = new Pixmap(cur.getWidth() / 2, cur.getHeight() / 2, Pixmap.Format.RGBA8888);
            half.setFilter(Pixmap.Filter.BiLinear);
            half.drawPixmap(cur, 0, 0, cur.getWidth(), cur.getHeight(), 0, 0, half.getWidth(), half.getHeight());
            cur.dispose();
            cur = half;
        }
        Pixmap out = new Pixmap(W, H, Pixmap.Format.RGBA8888);
        out.setFilter(Pixmap.Filter.BiLinear);
        out.drawPixmap(cur, 0, 0, cur.getWidth(), cur.getHeight(), 0, 0, W, H);

        out.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int fx = mirror ? W - 1 - x : x;
                if (outside[y * W + fx]) out.drawPixel(x, y, 0);
            }
        }

        Texture tex = new Texture(out);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        frame.dispose();
        if (disposeSrc) src.dispose();
        cur.dispose();
        out.dispose();
        return tex;
    }

    private static void pushOutside(Pixmap frame, boolean[] outside, IntArray stack, int x, int y, int W) {
        int i = y * W + x;
        if (!outside[i] && (frame.getPixel(x, y) & 0xff) < 40) {
            outside[i] = true;
            stack.add(i);
        }
    }
}
