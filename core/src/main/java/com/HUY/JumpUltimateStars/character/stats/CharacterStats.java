package com.HUY.JumpUltimateStars.character.stats;

/**
 * Chỉ số cố định của 1 nhân vật/dạng. Mỗi dạng kế thừa class này (vd NarutoBaseStats)
 * và ghi đè các giá trị muốn khác mặc định, thường là trong constructor.
 *
 * Đây chỉ là dữ liệu tĩnh, KHÔNG chứa logic và KHÔNG ghi dữ liệu trong trận vào đây
 * (máu hiện tại, vị trí, cooldown... nằm ở Fighter), vì 1 instance stats được dùng chung
 * giữa các Fighter cùng nhân vật (lấy qua CharacterStatsRegistry).
 */
public abstract class CharacterStats {

    // ===================== ENERGY =====================

    /** Energy của 1 thanh. */
    public static final float ENERGY_PER_BAR = 100f;

    /** Số thanh tích trữ tối đa. */
    public static final int MAX_ENERGY_BARS = 3;

    // ===================== DI CHUYỂN =====================

    /** Tốc độ đi ngang (pixel / giây), dùng cho cả đi dưới đất lẫn trên không. */
    public float moveSpeed = 450f;

    // ===================== NHẢY =====================

    /** Vận tốc ban đầu khi nhảy (px/s). Càng lớn thì nhảy càng cao. */
    public float jumpSpeed = 800f;

    /** Gia tốc rơi (px/s²). Càng lớn thì rơi càng nhanh, nhảy càng "nặng". */
    public float gravity = 2000f;

    /** Số lần nhảy tối đa trước khi chạm đất lại. 1 = nhảy đơn, 2 = nhảy kép. */
    public int maxJumps = 2;

    // ===================== LƯỚT =====================

    /** Quãng đường lướt (px). Lướt chạm mép màn hình thì thực tế có thể ngắn hơn. */
    public float dashDistance = 350f;

    /** Thời gian lướt (giây). Vận tốc lướt = dashDistance / dashDuration. */
    public float dashDuration = 0.2f;

    /** Khoảng nghỉ sau mỗi lần lướt, chưa lướt lại được (giây). Đếm từ lúc lướt xong. */
    public float dashRecovery = 0.1f;

    /**
     * Số lần lướt tối đa trên không trước khi chạm đất lại (chạm đất hoặc hạ cánh lên platform
     * thì reset). Lướt dưới đất không tính. 0 = không cho lướt trên không.
     */
    public int maxAirDashes = 2;

    // ===================== THỦ =====================

    /** Sát thương nhận vào khi đang thủ (cố định, bất kể đòn mạnh hay yếu). */
    public int guardDamage = 2;

    // ===================== HIỂN THỊ & MÁU =====================

    /** Hệ số phóng to nhân vật khi vẽ trong trận (nhân vào kích thước ảnh gốc). */
    public float scale = 1.8f;

    /** Máu tối đa. */
    public int maxHp = 1000;

    // ===================== ĐÒN ĐÁNH & SKILL =====================

    /**
     * Danh sách đòn thường theo thứ tự combo (phần tử đầu là đòn 1, phần tử cuối là đòn kết).
     * Nhân vật không có đòn thường thì để mảng rỗng.
     */
    public Move[] moves = {};

    /**
     * Đòn đánh trên không (bấm J khi đang AIR), mỗi lượt nhảy chỉ đánh được 1 lần.
     * Ví dụ new Move("J_AIR", 0.06f, 40, 1, 0f) -> characters/{charId}/J_AIR/J_AIR_0.png, ...
     * Trường recovery không dùng. null = nhân vật không có đòn trên không.
     */
    public Move airMove = null;

    /**
     * Skill U khi bấm trên không, mỗi lượt nhảy chỉ dùng được 1 lần.
     * Ví dụ: airSkillU = new DiveAirSkill("U_AIR", 0.06f, 0.3f, 10, 60f, 1500f);
     * null = nhân vật không có skill U trên không. Dùng chung cooldown với skillU dưới đất.
     */
    public AirSkill airSkillU = null;

    /** Skill gắn với phím U. null = nhân vật chưa có skill U (bấm U không có tác dụng). */
    public Skill skillU = null;

    /** Ultimate (phím I), tốn 1 thanh energy. null = không có. Mỗi lần dùng tạo instance mới. */
    public Ultimate.Factory ultimate = null;
}
