package com.HUY.JumpUltimateStars.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Class dữ liệu (data holder) đại diện cho thông tin của 1 nhân vật
 * trong game. Hiện tại chỉ lưu id và avatar.
 *
 * Lưu ý: KHÔNG lưu Animation ở đây. Animation được load riêng qua
 * CharacterAnimationManager (lazy-load theo charId + action), tách biệt
 * khỏi data tĩnh này để tận dụng cache và tránh load animation cho
 * toàn bộ nhân vật ngay khi khởi tạo registry.
 *
 * Đây là kiểu immutable đơn giản (các field là final), dùng để truyền
 * dữ liệu nhân vật cho các thành phần UI như CharacterSlot render ra.
 */
public class CharacterData {

    // Định danh duy nhất của nhân vật (dùng để so sánh, tìm kiếm, load animation, lưu save...).
    public final String id;

    // Ảnh đại diện (avatar) của nhân vật, dùng để vẽ trong slot chọn nhân vật.
    public final TextureRegion avatar;

    public CharacterData(String id, TextureRegion avatar) {
        this.id = id;
        this.avatar = avatar;
    }
}
