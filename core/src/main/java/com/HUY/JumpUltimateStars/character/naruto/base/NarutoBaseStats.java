package com.HUY.JumpUltimateStars.character.naruto.base;

import com.HUY.JumpUltimateStars.character.stats.CharacterStats;
import com.HUY.JumpUltimateStars.character.stats.Move;

public class NarutoBaseStats extends CharacterStats {
    public NarutoBaseStats() {
        moves = new Move[] {
            //        tên   giây/frame  dame  hitFrame  recovery
            new Move("J1", 0.07f,  5, 2, 0.15f),
            new Move("J2", 0.07f,  6, 2, 0.15f),
            new Move("J3", 0.07f,  7, 3, 0.15f),
            new Move("J4", 0.07f, 12, 3, 0.3f),  // đòn cuối: 0.3s khóa input
        };
    }
}
