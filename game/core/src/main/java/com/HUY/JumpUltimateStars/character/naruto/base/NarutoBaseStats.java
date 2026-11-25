package com.HUY.JumpUltimateStars.character.naruto.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.DiveAirSkill;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;
import com.HUY.JumpUltimateStars.effect.SmallShuriken;

public class NarutoBaseStats extends CharacterStats {
    public NarutoBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery  knockdown   energy
            new Move("J1", 0.07f, 15, 1, 0.15f, false).energy(6f),
            new Move("J2", 0.07f, 16, 2, 0.15f, false).energy(7f),
            new Move("J3", 0.07f, 17, 2, 0.15f, false).energy(8f),
            new Move("J4", 0.07f, 18, 3, 0.3f, false).energy(9f),   // đòn cuối: khóa input 0.3s
        };

        airMove = new Move("J_AIR", 0.06f, 15, 2, 0f, false).energy(5f);
        airSkillU = new DiveAirSkill("U_AIR", 0.06f, 0.3f, 25, 60f, 1200f, true).energy(10f);

        //                  tên   giây/frame  cooldown  spawnFrame  tạo effect (energy ghi trong SmallShuriken)
        skillU = new Skill("U", 0.07f, 0.3f, 2, (x, y, dir) -> new SmallShuriken(x, y, dir));

        ultimate = NarutoUltimate::new;
    }
}
