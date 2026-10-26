package com.HUY.JumpUltimateStars.character.stats;

/** Chỉ số của 1 nhân vật/dạng. Mỗi dạng kế thừa class này. Sau này thêm chiêu thức vào đây. */
public abstract class CharacterStats {

    public float moveSpeed = 450f; // pixel / giây
    public float scale = 2.0f;     // độ lớn nhân vật trong map
    public int maxHp = 1000;
}
