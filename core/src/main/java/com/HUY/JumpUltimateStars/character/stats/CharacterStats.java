package com.HUY.JumpUltimateStars.character.stats;

/** Chỉ số của 1 nhân vật/dạng. Mỗi dạng kế thừa class này. Sau này thêm chiêu thức vào đây. */
public abstract class CharacterStats {

    public float moveSpeed = 450f; // pixel / giây

    public float jumpSpeed = 1000f;  // vận tốc ban đầu khi nhảy (px/s), càng lớn càng cao
    public float gravity = 2000f;   // gia tốc rơi (px/s²)
    public int maxJumps = 2;        // 2 = nhảy kép

    public float dashDistance = 350f;  // quãng đường lướt (px)
    public float dashDuration = 0.2f;  // thời gian lướt (giây)
    public float dashRecovery = 0.1f;  // khoảng nghỉ sau mỗi lần lướt, chưa lướt lại được (giây)

    public float scale = 2.0f;     // độ lớn nhân vật trong map
    public int maxHp = 1000;
    public Move[] moves = {};   // <-- THÊM: nhân vật không có đòn thì để rỗng
}
