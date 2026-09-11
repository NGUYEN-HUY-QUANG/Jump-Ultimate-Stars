package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.Character;
import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.ui.CharacterSlot;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình chọn nhân vật (character select).
 * Bố cục màn hình chia làm 3 cột: trái (preview P1) - giữa (danh sách nhân vật) - phải (preview P2).
 *
 * Luồng hoạt động chính:
 *  1. show(): khởi tạo texture/renderer, tạo danh sách slot từ CharacterRegistry,
 *     xếp vị trí các slot, và tạo preview P1/P2 ban đầu.
 *  2. render(): mỗi frame xử lý input, update animation, rồi vẽ theo thứ tự
 *     nền -> glow/border của slot -> avatar của slot + preview nhân vật.
 *  3. dispose(): giải phóng toàn bộ tài nguyên (texture, batch, shapeRenderer, animation cache).
 */
public class CharacterSelectScreen implements Screen {

    // Tham chiếu tới game chính, dùng để chuyển màn hình khác (vd sau khi confirm xong)
    private final MainGame game;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture backgroundTexture;

    // Dùng để vẽ hình khối (glow, border) cho các slot nhân vật
    private ShapeRenderer shapeRenderer;

    // Danh sách toàn bộ slot hiển thị nhân vật ở cột giữa
    private List<CharacterSlot> slots = new ArrayList<>();

    // Tỉ lệ chia màn hình theo chiều ngang: trái 25% - giữa 50% - phải 25%
    private static final float LEFT_RATIO  = 0.25f;
    private static final float MID_RATIO   = 0.5f;
    private static final float RIGHT_RATIO = 0.25f;

    // Kích thước và khoảng cách giữa các slot nhân vật ở cột giữa
    private static final float SLOT_SIZE = 60f;
    private static final float SLOT_GAP  = 15f;

    // Index của nhân vật đang được P1 chọn trong danh sách slots
    private int p1Index = 0;

    // ===== Preview nhân vật (animation đứng yên) cho P1/P2 =====

    // Manager dùng chung để load/cache animation cho cả P1 và P2 preview,
    // tránh việc mỗi lần đổi lựa chọn lại load texture mới từ đầu
    private CharacterAnimationManager animManager;

    // Character preview hiển thị ở cột trái/phải, đại diện cho nhân vật
    // đang được P1/P2 chọn (hiện tại P2 tạm thời đi theo P1, xem refreshPreviews())
    private Character p1Preview;
    private Character p2Preview;

    // Tốc độ chạy animation "idle" của preview (giây/frame)
    private static final float IDLE_FRAME_DURATION = 0.12f;

    // Chỉnh 2 giá trị này để đổi kích thước và độ cao chân nhân vật preview.
    private static final float PREVIEW_SCALE = 4f;      // <-- chỉnh to/nhỏ nhân vật ở đây
    private static final float PREVIEW_BASELINE_Y = 200f; // <-- chỉnh độ cao chân nhân vật ở đây

    // Lệch thêm theo trục X riêng cho từng cột nếu cần (để dịch gần/xa danh sách giữa hơn).
    private static final float P1_OFFSET_X = 60f;
    private static final float P2_OFFSET_X = -60f;

    public CharacterSelectScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {

        // Khởi tạo các đối tượng vẽ cơ bản
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        backgroundTexture = new Texture("background/character_select_screen.png");

        shapeRenderer = new ShapeRenderer();

        // Tạo 1 CharacterSlot (UI) cho mỗi CharacterData có trong registry
        for (CharacterData data : CharacterRegistry.ALL) {
            slots.add(new CharacterSlot(data));
        }
        layoutSlots();

        // Mặc định chọn nhân vật đầu tiên cho P1
        p1Index = 0;
        slots.get(p1Index).setState(CharacterSlot.State.SELECTED);

        // Tạo manager quản lý animation, rồi tạo preview ban đầu cho P1/P2
        animManager = new CharacterAnimationManager();
        refreshPreviews();
    }

    /**
     * Tính toán và gán vị trí (x, y) cho từng slot nhân vật ở cột giữa,
     * canh giữa theo chiều ngang trong vùng MID_RATIO của màn hình.
     * Đồng thời cập nhật lại vị trí preview P1/P2 (vì vị trí phụ thuộc kích thước màn hình).
     * Được gọi lại mỗi khi resize() màn hình.
     */
    private void layoutSlots() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Điểm bắt đầu và bề rộng vùng cột giữa (nơi chứa danh sách slot)
        float midX = screenW * LEFT_RATIO;
        float midW = screenW * MID_RATIO;

        // Tính tổng bề rộng của toàn bộ slot (kể cả khoảng cách giữa chúng)
        // để canh giữa cả dãy slot trong vùng cột giữa
        int count = slots.size();
        float totalW = count * SLOT_SIZE + (count - 1) * SLOT_GAP;
        float startX = midX + (midW - totalW) / 2f;
        float y = screenH / 2f - SLOT_SIZE / 2f;

        // Gán vị trí lần lượt cho từng slot, cách đều nhau SLOT_GAP
        for (int i = 0; i < count; i++) {
            float x = startX + i * (SLOT_SIZE + SLOT_GAP);
            slots.get(i).setPosition(x, y, SLOT_SIZE);
        }

        // Preview có thể chưa được tạo (vd lần đầu show() gọi layoutSlots()
        // trước khi refreshPreviews() chạy), nên cần check null trước khi định vị lại
        if (p1Preview != null) positionPreview(p1Preview, true, P1_OFFSET_X);
        if (p2Preview != null) positionPreview(p2Preview, false, P2_OFFSET_X);
    }

    /**
     * Tạo (hoặc thay) Character preview cho P1 và P2 theo nhân vật đang được chọn,
     * rồi cho chạy animation "idle". Gọi khi mới show() và mỗi khi lựa chọn P1 đổi.
     */
    private void refreshPreviews() {
        String charId = slots.get(p1Index).getData().id;

        // Preview P1: đứng quay mặt bình thường (không lật), ở cột trái
        p1Preview = new Character(animManager, charId);
        p1Preview.playAnimation("idle", IDLE_FRAME_DURATION);
        p1Preview.setFlipX(false);
        p1Preview.setScale(PREVIEW_SCALE);
        positionPreview(p1Preview, true, P1_OFFSET_X);

        // TODO: chưa có input riêng cho P2, tạm cho P2 đi theo lựa chọn của P1.
        // Khi thêm p2Index + input riêng, đổi dòng dưới thành: slots.get(p2Index).getData().id
        // Preview P2: lật ngang (quay mặt vào P1) để 2 nhân vật đối diện nhau, ở cột phải
        p2Preview = new Character(animManager, charId);
        p2Preview.playAnimation("idle", IDLE_FRAME_DURATION);
        p2Preview.setFlipX(true);
        p2Preview.setScale(PREVIEW_SCALE);
        positionPreview(p2Preview, false, P2_OFFSET_X);
    }

    /**
     * Canh giữa Character preview trong cột trái (P1) hoặc cột phải (P2),
     * dựa theo width THẬT của frame hiện tại (đã nhân scale), không hardcode.
     *
     * @param c             preview cần định vị
     * @param isLeftColumn  true = canh giữa trong cột trái (P1), false = cột phải (P2)
     * @param offsetX       độ lệch thêm theo trục X (để tinh chỉnh vị trí thủ công)
     */
    private void positionPreview(Character c, boolean isLeftColumn, float offsetX) {
        float screenW = Gdx.graphics.getWidth();

        // Tính tâm theo chiều ngang của cột trái hoặc cột phải
        float centerX = isLeftColumn
            ? screenW * LEFT_RATIO / 2f
            : screenW * (LEFT_RATIO + MID_RATIO) + screenW * RIGHT_RATIO / 2f;

        // Lùi lại nửa bề rộng frame để tâm của nhân vật trùng với centerX
        float w = c.getCurrentFrameWidth();
        c.setPosition(centerX - w / 2f + offsetX, PREVIEW_BASELINE_Y);
    }

    /**
     * Xử lý input bàn phím của người chơi để duyệt qua danh sách nhân vật (A/D)
     * và xác nhận lựa chọn (J). Không xử lý gì nếu slot hiện tại đang trong
     * trạng thái CONFIRMING hoặc đã CONFIRMED (tránh đổi lựa chọn sau khi đã chốt).
     */
    private void handleInput() {
        CharacterSlot current = slots.get(p1Index);

        if (current.getState() == CharacterSlot.State.CONFIRMING
            || current.getState() == CharacterSlot.State.CONFIRMED) {
            return;
        }

        int newIndex = p1Index;

        // A = qua trái, D = qua phải trong danh sách nhân vật
        if (Gdx.input.isKeyJustPressed(Keys.A)) {
            newIndex = p1Index - 1;
        } else if (Gdx.input.isKeyJustPressed(Keys.D)) {
            newIndex = p1Index + 1;
        }

        // Giới hạn index trong khoảng hợp lệ [0, size-1], không cho vòng qua đầu/cuối
        if (newIndex < 0) newIndex = 0;
        if (newIndex >= slots.size()) newIndex = slots.size() - 1;

        // Nếu lựa chọn thay đổi: cập nhật trạng thái slot cũ/mới và load lại preview
        if (newIndex != p1Index) {
            current.setState(CharacterSlot.State.NORMAL);
            p1Index = newIndex;
            slots.get(p1Index).setState(CharacterSlot.State.SELECTED);

            refreshPreviews();
        }

        // J = xác nhận lựa chọn hiện tại, chuyển slot sang trạng thái CONFIRMING
        if (Gdx.input.isKeyJustPressed(Keys.J)) {
            slots.get(p1Index).setState(CharacterSlot.State.CONFIRMING);
        }
    }

    @Override
    public void render(float delta) {

        // Xử lý input trước, có thể làm thay đổi p1Index/preview
        handleInput();

        // Cập nhật trạng thái/animation theo thời gian cho từng slot và preview
        for (CharacterSlot s : slots) s.update(delta);
        p1Preview.update(delta);
        p2Preview.update(delta);

        // Xóa màn hình về màu đen trước khi vẽ frame mới
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Lớp 1: vẽ nền phủ toàn màn hình
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // Lớp 2: vẽ hiệu ứng glow (phát sáng) và viền cho các slot,
        // dùng ShapeRenderer riêng vì batch (SpriteBatch) không vẽ được shape thuần
        shapeRenderer.begin(ShapeType.Filled);
        for (CharacterSlot s : slots) s.renderGlow(shapeRenderer);
        for (CharacterSlot s : slots) s.renderBorder(shapeRenderer);
        shapeRenderer.end();

        // Lớp 3: vẽ avatar của từng slot + 2 nhân vật preview (P1/P2) đè lên trên cùng
        batch.begin();
        for (CharacterSlot s : slots) s.renderAvatar(batch);
        p1Preview.draw(batch);
        p2Preview.draw(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Màn hình đổi kích thước -> tính lại vị trí slot và preview
        layoutSlots();
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}

    @Override
    public void dispose() {
        // Giải phóng toàn bộ tài nguyên native khi rời khỏi màn hình này,
        // tránh leak bộ nhớ GPU (texture) và tài nguyên OpenGL (batch, shapeRenderer)
        backgroundTexture.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        animManager.dispose(); // giải phóng toàn bộ texture animation đã cache cho P1/P2 preview
    }
}
