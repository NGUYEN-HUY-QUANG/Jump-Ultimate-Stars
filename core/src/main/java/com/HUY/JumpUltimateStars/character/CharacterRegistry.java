package com.HUY.JumpUltimateStars.character;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.ArrayList;
import java.util.List;

/**
 * Nơi đăng ký (registry) tất cả các nhân vật có trong game.
 * Đóng vai trò như 1 "bảng danh sách" tĩnh, được nạp 1 lần duy nhất
 * khi class được load (thông qua static initializer bên dưới).
 *
 * Các màn hình khác (ví dụ màn hình chọn nhân vật) sẽ đọc từ danh sách
 * ALL này để tạo ra các CharacterSlot tương ứng, thay vì phải tự tạo
 * CharacterData rải rác ở nhiều nơi trong code.
 */
public class CharacterRegistry {

    // Danh sách chứa toàn bộ nhân vật đã được đăng ký trong game.
    // Dùng static + final vì đây là dữ liệu dùng chung, không đổi trong lúc chạy
    // (chỉ được nạp 1 lần lúc khởi động).
    public static final List<CharacterData> ALL = new ArrayList<>();

    // Static initializer: chạy đúng 1 lần khi class CharacterRegistry được load lần đầu.
    // Đây là nơi khai báo tất cả nhân vật của game.
    static {
        ALL.add(new CharacterData("goku_base", loadAvatar("characters/goku_base/icon.png")));
        ALL.add(new CharacterData("naruto_base", loadAvatar("characters/naruto_base/icon.png")));
        // thêm nhân vật mới: chỉ cần thêm 1 dòng ở đây
        // ALL.add(new CharacterData("archer", loadAvatar("archer_avatar.png")));
    }

    /**
     * Load ảnh avatar từ đường dẫn nội bộ (internal assets) và bọc thành TextureRegion.
     * path: đường dẫn tương đối tính từ thư mục assets, ví dụ "characters/goku_base/icon.png".
     *
     * Lưu ý: mỗi lần gọi sẽ tạo 1 Texture mới (load từ file), các Texture này
     * hiện chưa được dispose() ở đâu — cần chú ý quản lý vòng đời nếu sau này
     * cần giải phóng bộ nhớ (ví dụ khi thoát game hoặc đổi màn hình nhiều lần).
     */
    private static TextureRegion loadAvatar(String path) {
        return new TextureRegion(new Texture(Gdx.files.internal(path)));
    }

}
