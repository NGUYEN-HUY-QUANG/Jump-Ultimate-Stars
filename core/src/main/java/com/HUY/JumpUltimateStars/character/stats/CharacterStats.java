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

    // ===================== DI CHUYỂN =====================

    /** Tốc độ đi ngang (pixel / giây), dùng cho cả đi dưới đất lẫn trên không. */
    public float moveSpeed = 450f;

    // ===================== NHẢY =====================

    /** Vận tốc ban đầu khi nhảy (px/s). Càng lớn thì nhảy càng cao. */
    public float jumpSpeed = 1000f;

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

    // ===================== HIỂN THỊ & MÁU =====================

    /** Hệ số phóng to nhân vật khi vẽ trong trận (nhân vào kích thước ảnh gốc). */
    public float scale = 2.0f;

    /** Máu tối đa. */
    public int maxHp = 1000;

    // ===================== ĐÒN ĐÁNH & SKILL =====================

    /**
     * Danh sách đòn thường theo thứ tự combo (phần tử đầu là đòn 1, phần tử cuối là đòn kết).
     * Nhân vật không có đòn thường thì để mảng rỗng.
     */
    public Move[] moves = {};

    /** Skill gắn với phím U. null = nhân vật chưa có skill U (bấm U không có tác dụng). */
    public Skill skillU = null;
}
