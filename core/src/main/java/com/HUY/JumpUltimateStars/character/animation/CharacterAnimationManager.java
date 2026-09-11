package com.HUY.JumpUltimateStars.character.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý cache Animation cho toàn bộ nhân vật, tránh load lại texture
 * nhiều lần khi cùng 1 (charId, action) được yêu cầu lặp lại.
 * Dùng chung 1 instance cho toàn bộ CharacterSelectScreen (và cả BattleScreen sau này).
 */
public class CharacterAnimationManager {
    private final CharacterAnimationFactory factory = new CharacterAnimationFactory();
    private final Map<String, Animation<TextureRegion>> cache = new HashMap<>();

    public Animation<TextureRegion> get(String charId, String action, float frameDuration) {
        String key = charId + "_" + action;
        return cache.computeIfAbsent(key, k -> factory.load(charId, action, frameDuration));
    }

    public Animation<TextureRegion> get(String charId, String action, float frameDuration,
                                        Animation.PlayMode playMode) {
        String key = charId + "_" + action;
        return cache.computeIfAbsent(key, k -> factory.load(charId, action, frameDuration, playMode));
    }

    /** Giải phóng toàn bộ texture đã load. Gọi trong dispose() của Screen. */
    public void dispose() {
        for (Animation<TextureRegion> anim : cache.values()) {
            for (TextureRegion region : anim.getKeyFrames()) {
                region.getTexture().dispose();
            }
        }
        cache.clear();
    }
}
