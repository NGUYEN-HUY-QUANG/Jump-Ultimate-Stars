package com.HUY.JumpUltimateStars.character.goku.ssj;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.DiveAirSkill;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;

public class GokuSsjStats extends CharacterStats {
    public GokuSsjStats() {
        moveSpeed = 650f;

        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery  knockdown   energy
            new Move("J1", 0.065f, 20, 2, 0.15f, false).energy(10f),
            new Move("J2", 0.065f, 22, 2, 0.15f, false).energy(12f),
            new Move("J3", 0.065f, 24, 3, 0.15f, false).energy(14f),
            new Move("J4", 0.065f, 26, 3, 0.3f, false).energy(16f),
        };

        airMove = new Move("J_AIR", 0.06f, 25, 2, 0f, false).energy(10f);
        airSkillU = new DiveAirSkill("U_AIR", 0.06f, 0.3f, 35, 60f, 1200f, true).energy(15f);
        skillU = new Skill("U", 0.07f, 25, new int[]{3, 4, 5, 6, 7}, 0.3f, false)
            .energy(6f).rush(600f, 1500f);

        ultimate = GokuSsjUltimate::new;
    }
}
