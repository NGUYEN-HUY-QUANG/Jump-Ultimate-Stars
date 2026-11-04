package com.HUY.JumpUltimateStars.character.naruto.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;
import com.HUY.JumpUltimateStars.effect.ProjectileSpec;

public class NarutoBaseStats extends CharacterStats {
    public NarutoBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery
            new Move("J1", 0.07f,  5, 1, 0.15f),
            new Move("J2", 0.07f,  6, 2, 0.15f),
            new Move("J3", 0.07f,  7, 2, 0.15f),
            new Move("J4", 0.07f, 12, 3, 0.3f),  // đòn cuối: 0.3s khóa input
        };
        skillU = new Skill("U", 0.07f, 0.3f,
            new ProjectileSpec("small_shuriken",
                0.05f,   // giây/frame của phi tiêu
                1000f,    // speed
                1000f,    // range
                8,       // dame của cả loạt
                40f,     // offsetX
                60f,     // offsetY (viên giữa)
                1f,      // scale
                3,       // count
                40f),    // spacingY
            2);          // spawnFrame
    }
}
