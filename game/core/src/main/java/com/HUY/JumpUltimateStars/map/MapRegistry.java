package com.HUY.JumpUltimateStars.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách tất cả map trong game (giống CharacterRegistry của nhân vật).
 *
 * Nơi duy nhất cần sửa khi thêm map mới / chỉnh đất: sửa trong static block.
 * Các màn hình khác (chọn map, GameScreen) chỉ đọc từ đây, không tự tạo MapInfo.
 *
 * Thêm map mới: tạo thư mục assets/maps/<id>/ chứa <id>.png (bắt buộc),
 * <id>_bg.png và <id>_front.png (tùy chọn), rồi thêm 1 dòng ALL.add(...) bên dưới.
 */
public class MapRegistry {

    /** Tất cả map, theo thứ tự thêm vào (cũng là thứ tự hiển thị ở màn chọn map). */
    public static final List<MapInfo> ALL = new ArrayList<>();

    static {
        // Thứ tự tham số:
        //   id, tên hiển thị, scale,
        //   groundY (pixel ảnh, từ mép DƯỚI lên), spawn P1 x, spawn P2 x (pixel ảnh, từ mép trái),
        //   platforms {xTrái, xPhải, y}  (pixel ảnh, y từ mép DƯỚI lên)
        //
        // Đo số: bật MapData.DEBUG_PLATFORMS, chạy game, click chuột lên ảnh,
        // console sẽ in ra tọa độ pixel ảnh tại điểm click.

        // Map 0: phẳng, không có platform
        ALL.add(new MapInfo("map_0", "Map 0", 1.5f,
            30f, 100f, 700f,
            new float[][] {}));

        // Map 1: 3 platform
        ALL.add(new MapInfo("map_1", "Map 1", 1.5f,
            18f, 44f, 360f,
            new float[][] {
                {0f, 300f, 110f},   // trái
                {715f, 1015f, 110f}, // phải
            }));

        // Map 2: 2 platform
        ALL.add(new MapInfo("map_2", "Map 2", 1.5f,
            0f, 40f, 400f,
            new float[][] {}));
    }

    /**
     * Tìm map theo id.
     *
     * @throws IllegalArgumentException nếu không có map nào có id đó
     */
    public static MapInfo getById(String id) {
        for (MapInfo m : ALL) if (m.id.equals(id)) return m;
        throw new IllegalArgumentException("Không có map id: " + id);
    }
}
