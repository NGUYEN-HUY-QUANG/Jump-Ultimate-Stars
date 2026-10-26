package com.HUY.JumpUltimateStars.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách tất cả map trong game (giống CharacterRegistry của nhân vật).
 *
 * Nơi duy nhất cần sửa khi thêm map mới: thêm 1 dòng ALL.add(...) trong static block.
 * Các màn hình khác (chọn map, GameScreen) chỉ đọc từ đây, không tự tạo MapInfo.
 *
 * Lưu ý: MapInfo chỉ là dữ liệu tĩnh nên dùng chung 1 instance được. Còn Texture
 * thì do MapData giữ, mỗi lần vào trận tạo MapData mới từ MapInfo lấy ở đây.
 */
public class MapRegistry {

    /** Tất cả map, theo thứ tự thêm vào (thứ tự này cũng là thứ tự hiển thị ở màn chọn map sau này). */
    public static final List<MapInfo> ALL = new ArrayList<>();

    static {
        // Thứ tự tham số: id, tên hiển thị, ảnh nền (null = map tạm),
        //                 groundYRatio, spawn P1 (tỉ lệ chiều rộng), spawn P2
        ALL.add(new MapInfo("test", "Test Map", null, 0.50f, 0.30f, 0.70f));
        // ALL.add(new MapInfo("forest", "Forest", "maps/forest.png", 0.22f, 0.25f, 0.75f));
    }

    /**
     * Tìm map theo id.
     *
     * @param id id map (vd "test")
     * @return MapInfo tương ứng
     * @throws IllegalArgumentException nếu không có map nào có id đó
     *         (ném lỗi thay vì trả null để sai id báo ngay, không crash mơ hồ ở chỗ khác)
     */
    public static MapInfo getById(String id) {
        for (MapInfo m : ALL) if (m.id.equals(id)) return m;
        throw new IllegalArgumentException("Không có map id: " + id);
    }
}
