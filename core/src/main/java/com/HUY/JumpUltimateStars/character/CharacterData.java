package com.HUY.JumpUltimateStars.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class CharacterData {

    public final String id;
    public final TextureRegion avatar;
    // sau này thêm: Animation<TextureRegion> idleAnimation;

    public CharacterData(String id, TextureRegion avatar) {
        this.id = id;
        this.avatar = avatar;
    }

}
