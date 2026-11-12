package com.HUY.JumpUltimateStars.ui;

import com.HUY.JumpUltimateStars.character.stats.CharacterStatsRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
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
 * Các thành phần dính liền nhau, cả hàng tự scale cho vừa chiều rộng cửa sổ và căn giữa.
 *
 * Góc dưới màn hình: khung energy của mỗi bên
 *
 *   [energy P1][energy_bar P1]                    [energy_bar P2][energy P2]
 *
 * (hiện mới vẽ khung, chưa có logic năng lượng chạy bên trong)
 *
 * Tài nguyên dùng:
 *   HUD/avatar.png     khung avatar (xem FRAME_PATH)
 *   HUD/hp_bar.png     thanh máu (bản của P1: mép trong, sát time_bar, nằm bên phải)
 *   HUD/time_bar.png   thanh thời gian ở giữa
 *   HUD/time.png       vẽ đè lên time_bar
 *   HUD/energy.png     ô energy (bên trong cùng, sát mép màn hình)
 *   HUD/energy_bar.png thanh energy nằm cạnh ô energy
 *   avatar nhân vật: CharacterData.avatar (TextureRegion, cùng ảnh CharacterSlot dùng)
 *
 * Cách dùng trong GameScreen:
 *   show():    hud = new HUD(p1Id, p2Id, p1Avatar, p2Avatar); hud.resize(w, h);
 *   render():  vẽ SAU CÙNG (sau nhân vật + effect): hud.render(batch);
 *   resize():  hud.resize(width, height);
 *   dispose(): hud.dispose();
 *   Khi máu đổi: hud.setP1Hp(hp) / hud.setP2Hp(hp)
 */
public class HUD {

    // ===================== ĐƯỜNG DẪN (chỉnh cho đúng thư mục asset của m) =====================

    private static final String FRAME_PATH = "HUD/avatar.png";
    private static final String HP_BAR_PATH = "HUD/hp_bar.png";
    private static final String TIME_BAR_PATH = "HUD/time_bar.png";
    private static final String TIME_OVERLAY_PATH = "HUD/time.png";   // vẽ đè lên giữa time_bar
    private static final String ENERGY_ICON_PATH = "HUD/energy.png";
    private static final String ENERGY_BAR_PATH = "HUD/energy_bar.png";

    // ===================== BỐ CỤC CHUNG =====================

    /** Cả hàng chiếm tối đa bao nhiêu phần chiều rộng cửa sổ (1 = kín hết trừ lề). */
    private static final float WIDTH_RATIO = 1f;
    /** Lề trái/phải (pixel màn hình). */
    private static final float MARGIN_X = 8f;
    /** Lề trên (pixel màn hình). */
    private static final float MARGIN_TOP = 8f;
    /** Hàng HUD cao tối đa bao nhiêu phần chiều cao cửa sổ (tránh quá to trên màn hình rộng). */
    private static final float MAX_HEIGHT_RATIO = 0.16f;
    /** Phóng to thêm toàn bộ HUD sau khi đã tự scale (1 = như cũ, 1.2 = to hơn 20%). */
    private static final float HUD_SCALE_MULT = 1.2f;

    // ===================== CHỈNH THANH MÁU & time.png =====================

    /** Độ dài thanh máu (nhân với chiều rộng ảnh gốc, 1 = giữ nguyên, 1.3 = dài hơn 30%). Cao không đổi. */
    private static final float HP_STRETCH_X = 1f;

    // Offset tính bằng pixel của ẢNH GỐC (tự nhân theo scale).
    // X dương = dịch vào phía time_bar (P2 tự đảo chiều), âm = ra ngoài. Y dương = lên, âm = xuống.
    private static final float FRAME_OFFSET_X = 0f;   // khung + avatar
    private static final float FRAME_OFFSET_Y = 0f;

    private static final float HP_OFFSET_X = 0f;      // thanh máu
    private static final float HP_OFFSET_Y = -5f;

    private static final float TIME_OFFSET_X = 0f;    // time_bar
    private static final float TIME_OFFSET_Y = -5f;

    /** time.png mặc định nằm chính giữa time_bar, offset tính từ chính giữa đó. */
    private static final float TIME_OVERLAY_OFFSET_X = 0f;
    private static final float TIME_OVERLAY_OFFSET_Y = 0f;

    /** Màu phần máu đã mất (vẽ thanh đầy bằng màu này làm nền, phần máu còn lại vẽ đè lên). */
    private static final Color EMPTY_TINT = new Color(0.2f, 0.2f, 0.2f, 1f);

    // ===================== ENERGY (góc dưới) =====================

    /** Lề dưới + lề trái/phải của cụm energy (pixel màn hình). */
    private static final float ENERGY_MARGIN = 12f;

    // ===================== DỮ LIỆU =====================

    private final Texture frameTex;
    private final Texture hpTex;
    private final Texture timeTex;
    private final Texture timeOverlayTex;
    private final Texture energyIconTex;
    private final Texture energyBarTex;
    private final Texture avatar1;
    private final Texture avatar2;      // null khi chơi 1 người

    private final float maxHp1;
    private final float maxHp2;
    private float hp1;
    private float hp2;

    private final OrthographicCamera camera = new OrthographicCamera();

    // Toạ độ + kích thước đã tính sẵn trong layout() (pixel màn hình)
    private float frameW, frameH, hpW, hpH, timeW, timeH;
    private float overlayW, overlayH;
    private float frameY, hpY, timeY, overlayY;
    private float p1FrameX, p1BarX, timeX, p2BarX, p2FrameX, overlayX;

    // Energy
    private float eIconW, eIconH, eBarW, eBarH, eIconY, eBarY;
    private float p1EIconX, p1EBarX, p2EIconX, p2EBarX;

    /**
     * @param p1Id         id nhân vật P1 (để lấy maxHp)
     * @param p2Id         id nhân vật P2, null nếu chơi 1 người (khi đó chỉ vẽ phía P1 + thanh time)
     * @param p1Avatar     CharacterData.avatar của P1 (HUD chỉ đọc pixel, không dispose texture này)
     * @param p2Avatar     CharacterData.avatar của P2, null nếu chơi 1 người
     */
    public HUD(String p1Id, String p2Id, TextureRegion p1Avatar, TextureRegion p2Avatar) {
        frameTex = new Texture(Gdx.files.internal(FRAME_PATH));
        frameTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest); // khung pixel art
        hpTex = new Texture(Gdx.files.internal(HP_BAR_PATH));
        timeTex = new Texture(Gdx.files.internal(TIME_BAR_PATH));
        timeOverlayTex = new Texture(Gdx.files.internal(TIME_OVERLAY_PATH));
        energyIconTex = new Texture(Gdx.files.internal(ENERGY_ICON_PATH));
        energyIconTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        energyBarTex = new Texture(Gdx.files.internal(ENERGY_BAR_PATH));

        FileHandle frameFile = Gdx.files.internal(FRAME_PATH);
        avatar1 = buildAvatar(frameFile, p1Avatar, false);
        avatar2 = p2Id != null ? buildAvatar(frameFile, p2Avatar, true) : null;

        maxHp1 = CharacterStatsRegistry.get(p1Id).maxHp;
        maxHp2 = p2Id != null ? CharacterStatsRegistry.get(p2Id).maxHp : 1f;
        hp1 = maxHp1;
        hp2 = maxHp2;
    }

    // ===================== API =====================

    /** Đặt máu hiện tại của P1 (tự kẹp trong 0..maxHp). */
    public void setP1Hp(float hp) {
        hp1 = MathUtils.clamp(hp, 0f, maxHp1);
    }

    /** Đặt máu hiện tại của P2 (tự kẹp trong 0..maxHp). */
    public void setP2Hp(float hp) {
        hp2 = MathUtils.clamp(hp, 0f, maxHp2);
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

        drawSide(batch, avatar1, p1FrameX, p1BarX, false, hp1 / maxHp1);
        if (avatar2 != null) drawSide(batch, avatar2, p2FrameX, p2BarX, true, hp2 / maxHp2);

        batch.draw(timeTex, timeX, timeY, timeW, timeH);
        batch.draw(timeOverlayTex, overlayX, overlayY, overlayW, overlayH);   // time.png đè lên time_bar

        // Khung energy ở góc dưới
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
        avatar1.dispose();
        if (avatar2 != null) avatar2.dispose();
    }

    // ===================== BỐ CỤC =====================

    private void layout(int screenW, int screenH) {
        float fw = frameTex.getWidth(), fh = frameTex.getHeight();
        float hw = hpTex.getWidth() * HP_STRETCH_X, hh = hpTex.getHeight();
        float tw = timeTex.getWidth(), th = timeTex.getHeight();

        float totalW = 2f * (fw + hw) + tw;           // bề rộng cả hàng ở scale 1
        float maxH = Math.max(fh, Math.max(hh, th));  // bề cao hàng ở scale 1

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
        frameY = rowY + (rowH - frameH) / 2f + FRAME_OFFSET_Y * scale;   // mỗi phần căn giữa theo chiều dọc
        hpY = rowY + (rowH - hpH) / 2f + HP_OFFSET_Y * scale;
        timeY = rowY + (rowH - timeH) / 2f + TIME_OFFSET_Y * scale;

        // Vị trí gốc: các phần dính liền nhau, căn giữa cả hàng
        float left = (screenW - totalW * scale) / 2f;
        float baseP1Frame = left;
        float baseP1Bar = baseP1Frame + frameW;
        float baseTime = baseP1Bar + hpW;
        float baseP2Bar = baseTime + timeW;
        float baseP2Frame = baseP2Bar + hpW;

        // Cộng offset riêng cho từng phần (P2 đảo chiều X vì là bản lật)
        p1FrameX = baseP1Frame + FRAME_OFFSET_X * scale;
        p2FrameX = baseP2Frame - FRAME_OFFSET_X * scale;
        p1BarX = baseP1Bar + HP_OFFSET_X * scale;
        p2BarX = baseP2Bar - HP_OFFSET_X * scale;
        timeX = baseTime + TIME_OFFSET_X * scale;

        // time.png nằm chính giữa time_bar (đã tính offset của time_bar) rồi cộng offset riêng
        overlayX = timeX + (timeW - overlayW) / 2f + TIME_OVERLAY_OFFSET_X * scale;
        overlayY = timeY + (timeH - overlayH) / 2f + TIME_OVERLAY_OFFSET_Y * scale;

        // ---- Energy: góc dưới, P1 bên trái, P2 bên phải (lật) ----
        eIconW = energyIconTex.getWidth() * scale;  eIconH = energyIconTex.getHeight() * scale;
        eBarW = energyBarTex.getWidth() * scale;    eBarH = energyBarTex.getHeight() * scale;
        float eRowH = Math.max(eIconH, eBarH);
        eIconY = ENERGY_MARGIN + (eRowH - eIconH) / 2f;
        eBarY = ENERGY_MARGIN + (eRowH - eBarH) / 2f;

        p1EIconX = ENERGY_MARGIN;                      p1EBarX = p1EIconX + eIconW;
        p2EIconX = screenW - ENERGY_MARGIN - eIconW;   p2EBarX = p2EIconX - eBarW;
    }

    // ===================== VẼ =====================

    /**
     * Vẽ 1 bên: avatar -> khung đè lên -> thanh máu.
     * mirror = true cho P2 (khung và thanh máu bị lật ngang).
     * Máu mất đi từ mép NGOÀI vào, mép trong (sát time_bar) cố định.
     */
    private void drawSide(SpriteBatch batch, Texture avatar, float frameX, float barX,
                          boolean mirror, float ratio) {
        // Avatar rồi khung đè lên (avatar đã bị đục theo hình khung nên không lòi ra ngoài)
        batch.draw(avatar, frameX, frameY, frameW, frameH);
        batch.draw(frameTex, frameX, frameY, frameW, frameH,
            0, 0, frameTex.getWidth(), frameTex.getHeight(), mirror, false);

        int texW = hpTex.getWidth(), texH = hpTex.getHeight();

        // Nền: cả thanh bằng màu "máu đã mất"
        batch.setColor(EMPTY_TINT);
        batch.draw(hpTex, barX, hpY, hpW, hpH, 0, 0, texW, texH, mirror, false);

        // Phần máu còn lại: cắt từ mép trong (bên phải ảnh gốc) ra phía ngoài
        int visible = Math.round(texW * MathUtils.clamp(ratio, 0f, 1f));
        if (visible > 0) {
            float destW = hpW * visible / texW;
            float destX = mirror ? barX : barX + hpW - destW;
            batch.setColor(Color.WHITE);
            batch.draw(hpTex, destX, hpY, destW, hpH, texW - visible, 0, visible, texH, mirror, false);
        }
    }

    /**
     * Vẽ khung energy 1 bên: ô energy.png rồi energy_bar.png bên cạnh.
     * mirror = true cho P2 (cả hai ảnh bị lật ngang). Mới là khung, chưa có phần năng lượng chạy.
     * Nếu energy_bar.png vẽ theo hướng ngược (mép sát icon nằm bên phải ảnh) thì đổi mirror thành !mirror ở dòng vẽ bar.
     */
    private void drawEnergyFrame(SpriteBatch batch, float iconX, float barX, boolean mirror) {
        batch.setColor(Color.WHITE);
        batch.draw(energyIconTex, iconX, eIconY, eIconW, eIconH,
            0, 0, energyIconTex.getWidth(), energyIconTex.getHeight(), mirror, false);
        batch.draw(energyBarTex, barX, eBarY, eBarW, eBarH,
            0, 0, energyBarTex.getWidth(), energyBarTex.getHeight(), mirror, false);
    }

    // ===================== AVATAR ĐỤC THEO KHUNG =====================

    /**
     * Tạo texture avatar to bằng đúng khung, trong suốt ở mọi chỗ NGOÀI hình khung.
     * Vẽ avatar này trước, khung đè lên sau thì avatar không lòi ra góc vát.
     *
     * "Ngoài khung" = vùng trong suốt nối với 4 mép ảnh khung (flood fill), nên ruột kính
     * bán trong suốt của khung không bị khoét nhầm.
     *
     * @param mirror true cho P2: mặt nạ lật ngang để khớp với khung bị lật (avatar không bị lật mặt)
     */
    private static Texture buildAvatar(FileHandle frameFile, TextureRegion avatar, boolean mirror) {
        Pixmap frame = new Pixmap(frameFile);

        // Lấy pixel của texture chứa avatar (đã nằm trên GPU nên phải đọc lại dữ liệu gốc)
        TextureData td = avatar.getTexture().getTextureData();
        if (!td.isPrepared()) td.prepare();
        Pixmap src = td.consumePixmap();
        boolean disposeSrc = td.disposePixmap();
        int rx = avatar.getRegionX(), ry = avatar.getRegionY();
        int rw = avatar.getRegionWidth(), rh = avatar.getRegionHeight();
        int W = frame.getWidth(), H = frame.getHeight();

        // 1) Tìm vùng ngoài khung bằng flood fill từ 4 mép
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

        // 2) Crop avatar cho đúng tỉ lệ khung. Dịch xuống ~6% để lấy rõ mặt hơn
        int cw = rw, ch = rh;
        float aspect = (float) W / H;
        if (aspect > 1f) ch = Math.round(cw / aspect); else cw = Math.round(ch * aspect);
        int sx = rx + (rw - cw) / 2;
        int sy = ry + (rh - ch) / 2 + Math.round(rh * 0.06f);
        sx = MathUtils.clamp(sx, rx, rx + rw - cw);
        sy = MathUtils.clamp(sy, ry, ry + rh - ch);

        // 3) Thu nhỏ từng bước (chia đôi) cho mịn, rồi mới đưa vào kích thước khung
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

        // 4) Xoá avatar ở vùng ngoài khung
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

    /** Pixel khung gần như trong suốt (alpha < 40) và chưa đánh dấu thì đánh dấu "ngoài" rồi đẩy vào stack. */
    private static void pushOutside(Pixmap frame, boolean[] outside, IntArray stack, int x, int y, int W) {
        int i = y * W + x;
        if (!outside[i] && (frame.getPixel(x, y) & 0xff) < 40) {
            outside[i] = true;
            stack.add(i);
        }
    }
}
