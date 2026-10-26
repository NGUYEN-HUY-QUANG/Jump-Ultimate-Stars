package com.HUY.JumpUltimateStars.map;

/**
 * Dữ liệu TĨNH của một map: chỉ chứa số liệu, không giữ tài nguyên (Texture...),
 * nên dùng chung được ở nhiều nơi (MapRegistry, màn chọn map, GameScreen).
 *
 * Muốn vẽ map trong trận thì bọc MapInfo vào MapData (MapData giữ Texture và biết cách vẽ).
 *
 * Các giá trị "Ratio" đều là TỈ LỆ theo kích thước màn hình (0..1), không phải pixel,
 * để map vẫn đúng khi đổi kích thước cửa sổ. Thêm map mới = thêm 1 dòng trong MapRegistry.
 */
public class MapInfo {

    // Id duy nhất của map, dùng để tra cứu (MapRegistry.getById) và đặt tên file nếu cần
    public final String id;

    // Tên hiển thị cho người chơi (vd ở màn chọn map)
    public final String name;

    // Đường dẫn ảnh nền trong assets (vd "maps/forest.png").
    // null = map tạm: nền đen + đường đỏ làm mặt đất
    public final String backgroundPath;

    // Độ cao mặt đất theo tỉ lệ chiều cao màn hình (0 = đáy, 1 = đỉnh).
    // Chân nhân vật đứng ở độ cao này. Với map có ảnh: đo pixel mặt đất trong ảnh / chiều cao ảnh
    public final float groundYRatio;

    // Tâm ngang nơi P1 xuất hiện, theo tỉ lệ chiều rộng màn hình (0 = mép trái, 1 = mép phải)
    public final float p1SpawnXRatio;

    // Tâm ngang nơi P2 xuất hiện, tương tự p1SpawnXRatio (thường lớn hơn để P2 đứng bên phải)
    public final float p2SpawnXRatio;

    /**
     * @param id              id duy nhất của map
     * @param name            tên hiển thị
     * @param backgroundPath  đường dẫn ảnh nền, hoặc null nếu là map tạm
     * @param groundYRatio    độ cao mặt đất theo tỉ lệ chiều cao màn hình
     * @param p1SpawnXRatio   vị trí xuất hiện P1 theo tỉ lệ chiều rộng
     * @param p2SpawnXRatio   vị trí xuất hiện P2 theo tỉ lệ chiều rộng
     */
    public MapInfo(String id, String name, String backgroundPath,
                   float groundYRatio, float p1SpawnXRatio, float p2SpawnXRatio) {
        this.id = id;
        this.name = name;
        this.backgroundPath = backgroundPath;
        this.groundYRatio = groundYRatio;
        this.p1SpawnXRatio = p1SpawnXRatio;
        this.p2SpawnXRatio = p2SpawnXRatio;
    }
}
