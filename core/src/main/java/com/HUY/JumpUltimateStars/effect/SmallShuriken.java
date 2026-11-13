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
 * Loạt 3 phi tiêu xếp cột dọc, bay thẳng. Trúng đối thủ 1 lần là biến mất cả loạt.
 * Ảnh: effects/small_shuriken/small_shuriken_0.png, _1.png, ... (tự đếm số frame)
 */
public class SmallShuriken implements Effect {

    // ===== Chỉnh số ở đây =====
    private static final String FOLDER = "effects/small_shuriken";
    private static final float FRAME_DURATION = 0.05f;  // giây mỗi frame
    private static final float SPEED = 1000f;           // px/s
    private static final float RANGE = 1000f;           // bay tối đa bao xa (px)
    private static final float OFFSET_X = 40f;          // lệch ngang so với tâm nhân vật
    private static final float OFFSET_Y = 60f;          // độ cao viên giữa so với chân
    private static final float SCALE = 2f;
    private static final int COUNT = 3;                 // số viên
    private static final float SPACING_Y = 40f;         // khoảng cách dọc giữa 2 viên
    private static final int DAMAGE = 20;
    private static final boolean KNOCKDOWN = false;     // true = trúng thì knockout, false = chỉ hit

    private final Array<Texture> textures = new Array<>();
    private final Animation<TextureRegion> anim;
    private final int dir;
    private final float midY;
    private float x;                  // tâm ngang của cột phi tiêu
    private float traveled;           // quãng đã bay
    private float stateTime;
    private boolean active = true;
    private final Rectangle hitbox = new Rectangle();

    public SmallShuriken(float centerX, float footY, int dir) {
        this.dir = dir;
        this.x = centerX + dir * OFFSET_X;
        this.midY = footY + OFFSET_Y;

        // Load ảnh từ _0 tới khi hết file
        Array<TextureRegion> frames = new Array<>(TextureRegion.class);
        for (int i = 0; ; i++) {
            FileHandle f = Gdx.files.internal(FOLDER + "/small_shuriken_" + i + ".png");
            if (!f.exists()) break;
            Texture t = new Texture(f);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.add(t);
            frames.add(new TextureRegion(t));
        }
        if (frames.size == 0) {
            throw new GdxRuntimeException("Thiếu ảnh: " + FOLDER + "/small_shuriken_0.png");
        }
        anim = new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.LOOP);
    }

    // Độ cao của viên thứ i. COUNT = 3 -> lệch -1, 0, +1 so với viên giữa
    private float yOf(int i) {
        return midY + (i - (COUNT - 1) / 2f) * SPACING_Y;
    }

    @Override
    public void update(float delta) {
        if (!active) return;
        stateTime += delta;
        float step = SPEED * delta;
        x += dir * step;
        traveled += step;
        if (traveled >= RANGE) cancel();
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (!active) return;
        TextureRegion f = anim.getKeyFrame(stateTime);
        float w = f.getRegionWidth() * SCALE;
        float h = f.getRegionHeight() * SCALE;
        for (int i = 0; i < COUNT; i++) {
            float y = yOf(i) - h / 2f;
            if (dir < 0) batch.draw(f, x + w / 2f, y, -w, h);   // width âm = lật ngang
            else         batch.draw(f, x - w / 2f, y, w, h);
        }
    }

    @Override
    public int checkHit(Rectangle hurtbox) {
        if (!active) return 0;
        TextureRegion f = anim.getKeyFrame(stateTime);
        float w = f.getRegionWidth() * SCALE;
        float h = f.getRegionHeight() * SCALE;
        for (int i = 0; i < COUNT; i++) {
            hitbox.set(x - w / 2f, yOf(i) - h / 2f, w, h);
            if (hitbox.overlaps(hurtbox)) {
                cancel();            // trúng 1 viên là cả loạt biến mất
                return DAMAGE;
            }
        }
        return 0;
    }

    @Override
    public boolean isKnockdown() { return KNOCKDOWN; }

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
