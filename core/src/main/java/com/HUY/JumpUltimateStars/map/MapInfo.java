package com.HUY.JumpUltimateStars.map;

/**
 * Dữ liệu TĨNH của một map: chỉ chứa số liệu, không giữ tài nguyên (Texture...),
 * nên dùng chung được ở nhiều nơi (MapRegistry, màn chọn map, GameScreen).
 *
 * Muốn vẽ map trong trận thì bọc MapInfo vào MapData (MapData giữ Texture và biết cách vẽ).
 *
 * TỌA ĐỘ: mọi số (groundY, spawn, platform) là PIXEL CỦA ẢNH MAP, gốc ở GÓC DƯỚI TRÁI:
 * x tính từ mép trái, y tính từ mép DƯỚI lên (y càng lớn càng cao).
 * MapData tự nhân với `scale`, nên đổi scale thì đất đi theo.
 */
public class MapInfo {

    // Id duy nhất của map, dùng để tra cứu (MapRegistry.getById)
    public final String id;

    // Tên hiển thị cho người chơi (vd ở màn chọn map)
    public final String name;

    // Đường dẫn ảnh nền trong assets (vd "map/map_0.png").
    // null = map tạm: nền đen + đường đỏ làm mặt đất
    public final String backgroundPath;

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

    public MapInfo(String id, String name, String backgroundPath, float scale,
                   float groundY, float p1SpawnX, float p2SpawnX,
                   float[][] platforms) {
        this.id = id;
        this.name = name;
        this.backgroundPath = backgroundPath;
        this.scale = scale;
        this.groundY = groundY;
        this.p1SpawnX = p1SpawnX;
        this.p2SpawnX = p2SpawnX;
        this.platforms = platforms;
    }
}
