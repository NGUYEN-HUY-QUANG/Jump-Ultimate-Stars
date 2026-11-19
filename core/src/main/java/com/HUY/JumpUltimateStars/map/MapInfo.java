package com.HUY.JumpUltimateStars.map;

/**
 * Dữ liệu TĨNH của một map: chỉ chứa số liệu, không giữ tài nguyên (Texture...),
 * nên dùng chung được ở nhiều nơi (MapRegistry, màn chọn map, GameScreen).
 *
 * Muốn vẽ map trong trận thì bọc MapInfo vào MapData (MapData giữ Texture và biết cách vẽ).
 *
 * ẢNH: mỗi map có tối đa 3 lớp, đường dẫn tự sinh từ id (ví dụ id = "map_0"):
 *   maps/map_0/map_0_bg.png     phía sau map (tùy chọn)
 *   maps/map_0/map_0.png        map chính: đất + platform (bắt buộc)
 *   maps/map_0/map_0_front.png  phía trước nhân vật (tùy chọn)
 * Thứ tự vẽ: bg -> map -> nhân vật -> front. Cả 3 lớp nên cùng kích thước ảnh.
 *
 * TỌA ĐỘ: mọi số (groundY, spawn, platform) là PIXEL CỦA ẢNH MAP CHÍNH, gốc ở GÓC DƯỚI TRÁI:
 * x tính từ mép trái, y tính từ mép DƯỚI lên (y càng lớn càng cao).
 * MapData tự nhân với `scale`, nên đổi scale thì đất đi theo.
 */
public class MapInfo {

    // Id duy nhất của map, dùng để tra cứu (MapRegistry.getById) và sinh đường dẫn ảnh
    public final String id;

    // Tên hiển thị cho người chơi (vd ở màn chọn map)
    public final String name;

    // Đường dẫn 3 lớp ảnh trong assets (bg và front có thể không có file)
    public final String bgPath;
    public final String mapPath;
    public final String frontPath;

    // Hệ số phóng to map: 1 = đúng kích thước ảnh, 1.5 = to gấp rưỡi, 0.5 = nhỏ một nửa.
    // Phóng to map = nhân vật trông nhỏ hơn so với map (nhân vật không bị scale).
    public final float scale;

    // Độ cao mặt đất chính: pixel ảnh, tính từ mép DƯỚI lên. Chân nhân vật đứng ở đây.
    public final float groundY;

    // Tâm ngang nơi P1 / P2 xuất hiện: pixel ảnh, tính từ mép trái.
    public final float p1SpawnX;
    public final float p2SpawnX;

    // Các vùng đứng THÊM ngoài mặt đất chính (one-way: rơi xuống thì đứng được, nhảy lên thì xuyên qua).
    // Mỗi phần tử: {xTrái, xPhải, y} đều là pixel ảnh (y tính từ mép DƯỚI lên).
    // Map phẳng thì truyền mảng rỗng: new float[][] {}
    public final float[][] platforms;

    public MapInfo(String id, String name, float scale,
                   float groundY, float p1SpawnX, float p2SpawnX,
                   float[][] platforms) {
        this.id = id;
        this.name = name;
        String base = "maps/" + id + "/" + id;
        this.bgPath = base + "_bg.png";
        this.mapPath = base + ".png";
        this.frontPath = base + "_front.png";
        this.scale = scale;
        this.groundY = groundY;
        this.p1SpawnX = p1SpawnX;
        this.p2SpawnX = p2SpawnX;
        this.platforms = platforms;
    }
}
