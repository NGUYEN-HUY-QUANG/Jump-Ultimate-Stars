package com.HUY.JumpUltimateStars.character.goku.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.DiveAirSkill;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;

public class GokuBaseStats extends CharacterStats {
    public GokuBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery  knockdown   energy
            new Move("J1", 0.065f, 15, 2, 0.15f, false).energy(7f),
            new Move("J2", 0.065f, 16, 2, 0.15f, false).energy(8f),
            new Move("J3", 0.065f, 17, 3, 0.15f, false).energy(9f),
            new Move("J4", 0.065f, 18, 3, 0.3f, false).energy(10f),   // đòn cuối: khóa input 0.3s
        };

        airMove = new Move("J_AIR", 0.06f, 18, 2, 0f, false).energy(5f);
        airSkillU = new DiveAirSkill("U_AIR", 0.06f, 0.3f, 25, 60f, 1200f, true).energy(10f);

        //                  tên  giây/frame  dame/hit  các frame gây dame  cooldown  knockout
        skillU = new Skill("U", 0.07f, 15, new int[]{3, 4, 5, 6, 7}, 0.3f, false).energy(4f);   // 3 mỗi hit

        ultimate = GokuUltimate::new;
    }
}
