package com.HUY.JumpUltimateStars.character;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.ArrayList;
import java.util.List;

public class CharacterRegistry {

    public static final List<CharacterData> ALL = new ArrayList<>();

    static {
        ALL.add(new CharacterData("goku_base", loadAvatar("characters/goku_base/icon.png")));
        ALL.add(new CharacterData("naruto_base", loadAvatar("characters/naruto_base/icon.png")));
        // thêm nhân vật mới: chỉ cần thêm 1 dòng ở đây
        // ALL.add(new CharacterData("archer", loadAvatar("archer_avatar.png")));
    }

    private static TextureRegion loadAvatar(String path) {
        return new TextureRegion(new Texture(Gdx.files.internal(path)));
    }

}
