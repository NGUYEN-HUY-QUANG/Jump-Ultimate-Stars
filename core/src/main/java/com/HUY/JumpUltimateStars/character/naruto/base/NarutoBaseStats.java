package com.HUY.JumpUltimateStars.character.naruto.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;
import com.HUY.JumpUltimateStars.effect.SmallShuriken;

public class NarutoBaseStats extends CharacterStats {
    public NarutoBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery
            new Move("J1", 0.07f,  5, 1, 0.15f),
            new Move("J2", 0.07f,  6, 2, 0.15f),
            new Move("J3", 0.07f,  7, 2, 0.15f),
            new Move("J4", 0.07f, 12, 3, 0.3f),   // đòn cuối: khóa input 0.3s
        };

        //                  tên   giây/frame  cooldown  spawnFrame  tạo effect
        skillU = new Skill("U",0.07f,0.3f,2, (x, y, dir) -> new SmallShuriken(x, y, dir));
    }
}
