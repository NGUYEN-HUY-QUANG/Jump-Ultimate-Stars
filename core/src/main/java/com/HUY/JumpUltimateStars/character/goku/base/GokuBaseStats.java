package com.HUY.JumpUltimateStars.character.goku.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.Move;

public class GokuBaseStats extends CharacterStats {
    public GokuBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery
            new Move("J1", 0.065f,  5, 2, 0.15f),
            new Move("J2", 0.065f,  6, 2, 0.15f),
            new Move("J3", 0.065f,  7, 3, 0.15f),
            new Move("J4", 0.065f, 12, 3, 0.3f),  // đòn cuối: 0.3s khóa input
        };
    }
}
