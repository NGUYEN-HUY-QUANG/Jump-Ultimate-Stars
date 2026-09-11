package com.HUY.JumpUltimateStars.character.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Tự động load các frame ảnh PNG của 1 animation theo quy ước đặt tên:
 *   characters/{charId}/{action}/{action}_{index}.png
 * Load liên tục từ index 0 cho tới khi không tìm thấy file tiếp theo thì dừng.
 * Không cần khai báo trước số lượng frame ở bất kỳ đâu.
 */
public class CharacterAnimationFactory {

    public Animation<TextureRegion> load(String charId, String action, float frameDuration) {
        return load(charId, action, frameDuration, Animation.PlayMode.LOOP);
    }

    public Animation<TextureRegion> load(String charId, String action, float frameDuration,
                                         Animation.PlayMode playMode) {
        Array<TextureRegion> frames = new Array<>();

        int i = 0;
        while (true) {
            String path = "characters/" + charId + "/" + action + "/" + action + "_" + i + ".png";
            FileHandle file = Gdx.files.internal(path);
            if (!file.exists()) break;

            Texture texture = new Texture(file);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            frames.add(new TextureRegion(texture));
            i++;
        }

        if (frames.size == 0) {
            throw new GdxRuntimeException(
                "Không tìm thấy frame nào cho '" + charId + "/" + action +
                    "' — kiểm tra đường dẫn: characters/" + charId + "/" + action + "/" + action + "_0.png"
            );
        }

        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
        anim.setPlayMode(playMode);
        return anim;
    }
}
