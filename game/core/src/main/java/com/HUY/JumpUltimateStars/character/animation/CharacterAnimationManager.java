package com.HUY.JumpUltimateStars.character.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý cache Animation cho toàn bộ nhân vật, tránh load lại texture
 * nhiều lần khi cùng 1 (charId, action) được yêu cầu lặp lại.
 * Dùng chung 1 instance cho toàn bộ CharacterSelectScreen (và cả BattleScreen sau này).
 *
 * Lưu ý: đây KHÔNG phải class thread-safe (dùng HashMap thường).
 * Nếu load animation từ nhiều thread khác nhau (vd loading song song),
 * cần đổi sang ConcurrentHashMap hoặc đồng bộ hóa thủ công.
 */
public class CharacterAnimationManager {

    // Factory dùng để thực sự load frame ảnh từ disk khi cache chưa có
    private final CharacterAnimationFactory factory = new CharacterAnimationFactory();

    // Cache lưu Animation đã load, key là "{charId}_{action}"
    // Mục đích: tránh việc gọi factory.load() (đọc file + tạo Texture) nhiều lần
    // cho cùng 1 animation, vì thao tác đó tốn tài nguyên (I/O + GPU memory)
    private final Map<String, Animation<TextureRegion>> cache = new HashMap<>();

    /**
     * Lấy animation với playMode mặc định (LOOP, do CharacterAnimationFactory quyết định).
     * Nếu đã có trong cache thì trả về ngay, nếu chưa thì load mới rồi lưu vào cache.
     *
     * @param charId        id của nhân vật
     * @param action        tên hành động/animation (vd: "run", "jump")
     * @param frameDuration thời gian hiển thị mỗi frame (giây)
     */
    public Animation<TextureRegion> get(String charId, String action, float frameDuration) {
        // Key duy nhất cho mỗi cặp (charId, action) để tra cứu trong cache
        String key = charId + "_" + action;

        // computeIfAbsent: nếu key đã tồn tại -> trả về giá trị cũ (không load lại)
        // nếu chưa tồn tại -> gọi factory.load() để load frame, lưu vào cache rồi trả về
        return cache.computeIfAbsent(key, k -> factory.load(charId, action, frameDuration));
    }

    /**
     * Overload cho phép chỉ định rõ playMode (LOOP, NORMAL, REVERSED, ...).
     * Logic cache tương tự như hàm get() ở trên.
     *
     * Lưu ý: nếu cùng 1 (charId, action) được gọi trước đó với playMode khác
     * (vd LOOP rồi sau đó gọi lại với NORMAL), hàm này vẫn trả về animation
     * đã cache từ lần gọi đầu tiên (playMode cũ), KHÔNG load lại hay đổi playMode.
     * Đây là điểm cần lưu ý khi sử dụng để tránh nhầm lẫn.
     */
    public Animation<TextureRegion> get(String charId, String action, float frameDuration,
                                        Animation.PlayMode playMode) {
        String key = charId + "_" + action;
        return cache.computeIfAbsent(key, k -> factory.load(charId, action, frameDuration, playMode));
    }

    /**
     * Giải phóng toàn bộ texture đã load trong cache (giải phóng bộ nhớ GPU).
     * BẮT BUỘC phải gọi hàm này trong dispose() của Screen khi không còn dùng
     * animation nữa, nếu không sẽ gây leak Texture (Texture không tự động
     * được Java GC dọn dẹp vì nó là tài nguyên native/GPU).
     */
    public void dispose() {
        // Duyệt qua tất cả animation đã cache
        for (Animation<TextureRegion> anim : cache.values()) {
            // Duyệt qua từng frame (TextureRegion) của animation đó
            for (TextureRegion region : anim.getKeyFrames()) {
                // Giải phóng Texture gốc mà TextureRegion đang tham chiếu tới
                region.getTexture().dispose();
            }
        }

        // Xóa toàn bộ entry trong cache, tránh giữ tham chiếu tới
        // Texture đã dispose (nếu không sẽ gây lỗi khi lỡ dùng lại)
        cache.clear();
    }
}
