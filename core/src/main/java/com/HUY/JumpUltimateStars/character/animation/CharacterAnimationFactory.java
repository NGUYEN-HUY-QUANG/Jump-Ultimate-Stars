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
 *   characters/{charId}/{action}/{action}_{index}.png
 * Load liên tục từ index 0 cho tới khi không tìm thấy file tiếp theo thì dừng.
 * Không cần khai báo trước số lượng frame ở bất kỳ đâu.
 *
 * Ví dụ: charId = "mario", action = "run"
 *   -> sẽ load lần lượt: characters/mario/run/run_0.png, run_1.png, run_2.png, ...
 *      cho tới khi gặp file không tồn tại (vd run_5.png không có) thì dừng lại ở run_4.png.
 */
public class CharacterAnimationFactory {

    /**
     * Overload rút gọn: load animation với playMode mặc định là LOOP
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
     * Load toàn bộ frame của 1 animation từ file PNG trên disk.
     *
     * @param charId        id của nhân vật
     * @param action        tên hành động/animation
     * @param frameDuration thời gian hiển thị mỗi frame (giây)
     * @param playMode      chế độ chạy animation (LOOP, NORMAL, REVERSED, ...)
     * @return đối tượng Animation đã sẵn sàng sử dụng để vẽ (render)
     * @throws GdxRuntimeException nếu không tìm thấy frame nào (kể cả frame_0)
     */
    public Animation<TextureRegion> load(String charId, String action, float frameDuration,
                                         Animation.PlayMode playMode) {
        // Mảng chứa các frame (TextureRegion) sẽ được load tuần tự
        Array<TextureRegion> frames = new Array<>();

        // Bắt đầu từ frame số 0, load liên tục cho tới khi không còn file
        int i = 0;
        while (true) {
            // Xây dựng đường dẫn theo quy ước: characters/{charId}/{action}/{action}_{i}.png
            String path = "characters/" + charId + "/" + action + "/" + action + "_" + i + ".png";
            FileHandle file = Gdx.files.internal(path);

            // Nếu file không tồn tại -> đã hết frame, dừng vòng lặp
            if (!file.exists()) break;

            // Tạo Texture từ file ảnh và bật lọc mượt (Linear filter)
            // giúp ảnh không bị vỡ nét/răng cưa khi scale (phóng to/thu nhỏ)
            Texture texture = new Texture(file);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // Bọc Texture vào TextureRegion rồi thêm vào danh sách frame
            frames.add(new TextureRegion(texture));

            // Tăng index để load frame tiếp theo
            i++;
        }

        // Nếu không load được frame nào (kể cả frame đầu tiên _0)
        // -> ném exception để báo lỗi rõ ràng, tránh crash mơ hồ ở chỗ khác
        if (frames.size == 0) {
            throw new GdxRuntimeException(
                "Không tìm thấy frame nào cho '" + charId + "/" + action +
                    "' — kiểm tra đường dẫn: characters/" + charId + "/" + action + "/" + action + "_0.png"
            );
        }

        // Tạo đối tượng Animation từ danh sách frame vừa load,
        // với thời gian hiển thị mỗi frame là frameDuration
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);

        // Thiết lập chế độ chạy (loop, chạy 1 lần, đảo ngược, ...)
        anim.setPlayMode(playMode);

        return anim;
    }
}
