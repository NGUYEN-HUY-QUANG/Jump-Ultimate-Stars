package com.HUY.JumpUltimateStars.character.goku.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.DiveAirSkill;
import com.HUY.JumpUltimateStars.character.stats.Move;
import com.HUY.JumpUltimateStars.character.stats.Skill;

public class GokuBaseStats extends CharacterStats {
    public GokuBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery
            new Move("J1", 0.065f,  15, 2, 0.15f, false),
            new Move("J2", 0.065f,  16, 2, 0.15f, false),
            new Move("J3", 0.065f,  17, 3, 0.15f, false),
            new Move("J4", 0.065f, 18, 3, 0.3f, false),  // đòn cuối: khóa input 0.3s
        };

        airMove = new Move("J_AIR", 0.06f, 18, 2, 0f, false);  // đòn trên không: bấm J khi đang AIR, chỉ đánh được 1 lần mỗi lượt nhảy
        airSkillU = new DiveAirSkill("U_AIR", 0.06f, 0.3f, 25, 60f, 1200f, true);  // skill U trên không: lao xuống đất

        //                  tên  giây/frame  dame/hit  các frame gây dame  cooldown
        skillU = new Skill("U",0.07f,15, new int[]{3, 4, 5, 6, 7}, 0.3f, false);
    }
}
