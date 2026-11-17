package com.HUY.JumpUltimateStars.effect;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Kamehameha: hiện quả cầu -> thân dài ra -> giữ -> cả tia mỏng dần rồi biến mất.
 * Gây dame liên tục: cứ TICK_INTERVAL giây dame 1 phát khi còn chạm hurtbox.
 * Từ lúc bắt đầu thu nhỏ (SHRINK_AT) thì phát dame kế tiếp sẽ knockdown.
 * Ảnh: effects/kamehameha/kamehameha_start_0.png, _head_0.png, _body_0.png, _tail_0.png
 */
public class Kamehameha implements Effect {

    // ===== Chỉnh số ở đây =====
    private static final String FOLDER = "effects/kamehameha";
    private static final float FRAME_DURATION = 0.05f;
    private static final float OFFSET_X = 60f;
    private static final float OFFSET_Y = 60f;
    private static final float SCALE = 4f;

    private static final float START_TIME = 0.2f;       // hiện quả cầu tới giây này thì bắn
    private static final float GROW_SPEED = 6000f;      // px/s thân dài ra
    private static final float MAX_BODY_LEN = 2000f;
    private static final float SHRINK_AT = 2f;        // tới giây này thì bắt đầu thu nhỏ
    private static final float SHRINK_TIME = 1f;      // thu nhỏ trong bao lâu rồi biến mất
    private static final float MIN_THICKNESS = 0.15f;   // thu tới tỉ lệ này

    private static final float TICK_INTERVAL = 0.1f;
    private static final int DAMAGE = 8;
    private static final float ENERGY = 0f;             // kame chỉ dùng cho ultimate nên không hồi energy

    private final Array<Texture> textures = new Array<>();
    private final Animation<TextureRegion> startAnim, headAnim, bodyAnim, tailAnim;
    private final int dir;
    private final float x;           // mép trước của tia, ngay trước mặt nhân vật
    private final float midY;
    private float time;              // giây kể từ lúc spawn
    private float bodyLen;
    private float thickness = 1f;    // hệ số chiều cao
    private float tickTimer;
    private boolean active = true;
    private final Rectangle hitbox = new Rectangle();

    public Kamehameha(float centerX, float footY, int dir) {
        this.dir = dir;
        this.x = centerX + dir * OFFSET_X;
        this.midY = footY + OFFSET_Y;
        startAnim = loadAnim("kamehameha_start");
        headAnim = loadAnim("kamehameha_head");
        bodyAnim = loadAnim("kamehameha_body");
        tailAnim = loadAnim("kamehameha_tail");
    }

    private Animation<TextureRegion> loadAnim(String name) {
        Array<TextureRegion> frames = new Array<>(TextureRegion.class);
        for (int i = 0; ; i++) {
            FileHandle f = Gdx.files.internal(FOLDER + "/" + name + "_" + i + ".png");
            if (!f.exists()) break;
            Texture t = new Texture(f);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.add(t);
            frames.add(new TextureRegion(t));
        }
        if (frames.size == 0) {
            throw new GdxRuntimeException("Thiếu ảnh: " + FOLDER + "/" + name + "_0.png");
        }
        return new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.LOOP);
    }

    @Override
    public void update(float delta) {
        if (!active) return;
        time += delta;
        tickTimer -= delta;

        bodyLen = Math.max(0, Math.min(MAX_BODY_LEN, (time - START_TIME) * GROW_SPEED));

        if (time > SHRINK_AT) {
            float t = Math.min(1f, (time - SHRINK_AT) / SHRINK_TIME);
            thickness = 1f - (1f - MIN_THICKNESS) * t;
            if (t >= 1f) cancel();
        }
    }

    /** Vẽ 1 đoạn cách mép trước 'from' px, rộng w, cao h. */
    private void drawPart(SpriteBatch batch, TextureRegion r, float from, float w, float h) {
        float y = midY - h / 2f;
        if (dir < 0) batch.draw(r, x - from, y, -w, h);   // width âm = lật ngang
        else batch.draw(r, x + from, y, w, h);
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (!active) return;

        if (time < START_TIME) {
            TextureRegion s = startAnim.getKeyFrame(time);
            drawPart(batch, s, 0, s.getRegionWidth() * SCALE, s.getRegionHeight() * SCALE);
            return;
        }

        TextureRegion head = headAnim.getKeyFrame(time);
        TextureRegion body = bodyAnim.getKeyFrame(time);
        TextureRegion tail = tailAnim.getKeyFrame(time);
        float headW = head.getRegionWidth() * SCALE;
        float tailW = tail.getRegionWidth() * SCALE;

        drawPart(batch, head, 0,               headW,   head.getRegionHeight() * SCALE * thickness);
        drawPart(batch, body, headW,           bodyLen, body.getRegionHeight() * SCALE * thickness);
        drawPart(batch, tail, headW + bodyLen, tailW,   tail.getRegionHeight() * SCALE * thickness);
    }

    @Override
    public int checkHit(Rectangle hurtbox) {
        if (!active || time < START_TIME || tickTimer > 0) return 0;

        float len = headAnim.getKeyFrame(time).getRegionWidth() * SCALE
            + bodyLen
            + tailAnim.getKeyFrame(time).getRegionWidth() * SCALE;
        float h = bodyAnim.getKeyFrame(time).getRegionHeight() * SCALE * thickness;
        hitbox.set(dir > 0 ? x : x - len, midY - h / 2f, len, h);

        if (hitbox.overlaps(hurtbox)) {
            tickTimer = TICK_INTERVAL;
            return DAMAGE;
        }
        return 0;
    }

    // Từ lúc bắt đầu thu nhỏ, phát dame kế tiếp sẽ knockdown (đối thủ bị knockdown thì
    // được miễn dame nên đó cũng là phát cuối)
    @Override
    public boolean isKnockdown() { return time >= SHRINK_AT; }

    @Override
    public float getEnergy() { return ENERGY; }

    @Override
    public boolean isActive() { return active; }

    @Override
    public void cancel() {
        if (!active) return;
        active = false;
        for (Texture t : textures) t.dispose();
        textures.clear();
    }
}
