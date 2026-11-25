package com.HUY.JumpUltimateStars.character.stats;

/** Configuration for a form change that plays an animation before switching characters. */
public class Transformation {

    public final String targetCharacterId;
    public final String action;
    public final float frameDuration;
    public final float energyCost;

    public Transformation(String targetCharacterId, String action, float frameDuration, float energyCost) {
        this.targetCharacterId = targetCharacterId;
        this.action = action;
        this.frameDuration = frameDuration;
        this.energyCost = energyCost;
    }
}
