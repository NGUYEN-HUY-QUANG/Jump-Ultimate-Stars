package com.HUY.JumpUltimateStars.character.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Tự động load các frame ảnh PNG của 1 animation theo quy ước đặt tên:
 *   {thư mục}/{tiền tố}_{index}.png
 * Load liên tục từ index 0 cho tới khi không tìm thấy file tiếp theo thì dừng.
 * Không cần khai báo trước số lượng frame ở bất kỳ đâu.
 *
 * Có 2 cách dùng:
 *  - Nhân vật: load(charId, action, ...) -> characters/{charId}/{action}/{action}_{index}.png
 *    Ví dụ charId = "mario", action = "run"
 *      -> load lần lượt characters/mario/run/run_0.png, run_1.png, run_2.png, ...
 *         cho tới khi gặp file không tồn tại (vd run_5.png không có) thì dừng ở run_4.png.
 *  - Thư mục bất kỳ (effect...): loadFrames(folder, prefix, ...)
 *    Ví dụ effects/small_shuriken/small_shuriken_0.png, small_shuriken_1.png, ...
 *
 * Lưu ý: Texture là tài nguyên native (GPU), class này chỉ TẠO chứ không dispose.
 * Nơi gọi (CharacterAnimationManager, EffectManager) phải tự dispose các Texture đã load.
 */
public class CharacterAnimationFactory {

    /**
     * Overload rút gọn: load animation nhân vật với playMode mặc định là LOOP
     * (animation sẽ tự lặp lại từ đầu sau khi chạy hết các frame).
     *
     * @param charId        id của nhân vật, dùng để xác định thư mục ảnh (vd: "mario")
     * @param action        tên hành động/animation (vd: "run", "jump", "idle")
     * @param frameDuration thời gian hiển thị mỗi frame (giây), quyết định tốc độ animation
     */
    public Animation<TextureRegion> load(String charId, String action, float frameDuration) {
        return load(charId, action, frameDuration, Animation.PlayMode.LOOP);
    }

    /**
     * Load animation của nhân vật theo quy ước characters/{charId}/{action}/{action}_{i}.png.
     * Thực chất chỉ ghép đường dẫn rồi giao cho loadFrames() làm việc.
     *
     * @param charId        id của nhân vật
     * @param action        tên hành động/animation (cũng là tên thư mục và tiền tố file ảnh)
     * @param frameDuration thời gian hiển thị mỗi frame (giây)
     * @param playMode      chế độ chạy animation (LOOP, NORMAL, REVERSED, ...)
     * @return Animation đã sẵn sàng để vẽ
     * @throws GdxRuntimeException nếu không tìm thấy frame nào (kể cả frame _0)
     */
    public Animation<TextureRegion> load(String charId, String action, float frameDuration, Animation.PlayMode playMode) {
        return loadFrames("characters/" + charId + "/" + action, action, frameDuration, playMode);
    }

    /**
     * Load frame theo quy ước {folder}/{prefix}_{index}.png, từ 0 tới khi hết file.
     * Dùng chung cho nhân vật (characters/...) và effect (effects/...).
     *
     * @param folder        thư mục chứa ảnh (vd "effects/small_shuriken")
     * @param prefix        tiền tố tên file ảnh (vd "small_shuriken" -> small_shuriken_0.png)
     * @param frameDuration thời gian hiển thị mỗi frame (giây)
     * @param playMode      chế độ chạy animation
     * @return Animation đã sẵn sàng để vẽ
     * @throws GdxRuntimeException nếu không tìm thấy frame nào (kể cả frame _0)
     */
    public Animation<TextureRegion> loadFrames(String folder, String prefix, float frameDuration, Animation.PlayMode playMode) {
        // Mảng chứa các frame sẽ được load tuần tự
        Array<TextureRegion> frames = new Array<>(TextureRegion.class);

        // Bắt đầu từ frame số 0, load liên tục cho tới khi không còn file
        int i = 0;
        while (true) {
            // Đường dẫn theo quy ước: {folder}/{prefix}_{i}.png
            FileHandle file = Gdx.files.internal(folder + "/" + prefix + "_" + i + ".png");

            // File không tồn tại -> đã hết frame, dừng vòng lặp
            if (!file.exists()) break;

            // Tạo Texture từ file ảnh, bật lọc Linear để ảnh mượt khi phóng to/thu nhỏ
            Texture texture = new Texture(file);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // Bọc Texture vào TextureRegion rồi thêm vào danh sách frame
            frames.add(new TextureRegion(texture));
            i++;
        }

        // Không load được frame nào (kể cả _0): ném lỗi rõ ràng thay vì để crash mơ hồ ở chỗ khác
        if (frames.size == 0) {
            throw new GdxRuntimeException("Không tìm thấy frame nào, kiểm tra: " + folder + "/" + prefix + "_0.png");
        }

        // Tạo Animation từ các frame vừa load, playMode được truyền thẳng vào constructor
        return new Animation<>(frameDuration, frames, playMode);
    }
}
