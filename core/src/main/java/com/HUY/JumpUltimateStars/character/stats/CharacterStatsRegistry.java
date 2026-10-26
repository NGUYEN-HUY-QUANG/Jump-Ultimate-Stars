package com.HUY.JumpUltimateStars.character.stats;

import com.HUY.JumpUltimateStars.character.goku.base.GokuBaseStats;
import com.HUY.JumpUltimateStars.character.naruto.base.NarutoBaseStats;

import java.util.HashMap;
import java.util.Map;

/** Tra stats theo id nhân vật. Thêm nhân vật/dạng mới = thêm 1 dòng. */
public class CharacterStatsRegistry {

    private static final Map<String, CharacterStats> BY_ID = new HashMap<>();

    static {
        BY_ID.put("naruto_base", new NarutoBaseStats());
        BY_ID.put("goku_base", new GokuBaseStats());
    }

    /** Chưa đăng ký thì dùng DefaultStats, không crash. */
    public static CharacterStats get(String charId) {
        CharacterStats s = BY_ID.get(charId);
        return s != null ? s : new DefaultStats();
    }
}
