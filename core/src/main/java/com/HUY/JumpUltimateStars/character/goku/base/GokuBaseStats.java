package com.HUY.JumpUltimateStars.character.goku.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;

public class GokuBaseStats extends CharacterStats {
    public GokuBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery
            new Move("J1", 0.065f,  5, 2, 0.15f),
            new Move("J2", 0.065f,  6, 2, 0.15f),
            new Move("J3", 0.065f,  7, 3, 0.15f),
            new Move("J4", 0.065f, 12, 3, 0.3f),  // đòn cuối: khóa input 0.3s
        };

        //                  tên  giây/frame  dame/hit  các frame gây dame  cooldown
        skillU = new Skill("U",0.07f,4, new int[]{3, 4, 5, 6, 7}, 0.3f);
    }
}
