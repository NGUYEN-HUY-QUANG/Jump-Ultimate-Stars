package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.character.CharacterData;
import com.HUY.JumpUltimateStars.character.CharacterRegistry;
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
 * Hiện tại chỉ mới xử lý phần danh sách ở giữa và input của P1; phần preview 2 bên
 * (LEFT_RATIO / RIGHT_RATIO) đang được dùng để tính layout nhưng chưa có nội dung vẽ riêng.
 */
public class CharacterSelectScreen implements Screen {

    private final MainGame game;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture backgroundTexture;

    private ShapeRenderer shapeRenderer;

    // Danh sách các ô nhân vật hiển thị trên màn hình, được tạo từ CharacterRegistry.ALL.
    private List<CharacterSlot> slots = new ArrayList<>();

    // Tỉ lệ chia màn hình theo chiều ngang: trái (preview P1) - giữa (danh sách) - phải (preview P2).
    // Tổng 3 giá trị này = 1.0 (100% chiều rộng màn hình).
    private static final float LEFT_RATIO  = 0.25f;
    private static final float MID_RATIO   = 0.5f;
    private static final float RIGHT_RATIO = 0.25f;

    // Kích thước (cạnh vuông) và khoảng cách giữa các ô nhân vật.
    private static final float SLOT_SIZE = 60f;
    private static final float SLOT_GAP  = 15f;

    // Chỉ số (index trong danh sách slots) mà Player 1 đang trỏ tới / đã chọn.
    private int p1Index = 0;

    public CharacterSelectScreen(MainGame game) {
        this.game = game;
    }

    /**
     * Được gọi khi màn hình này được chuyển tới (trở thành màn hình đang active).
     * Khởi tạo các tài nguyên vẽ, load danh sách nhân vật từ registry thành các
     * CharacterSlot, sắp xếp vị trí cho chúng, và tự động chọn sẵn nhân vật đầu tiên.
     */
    @Override
    public void show() {

        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        backgroundTexture = new Texture("background/character_select_screen.png");

        shapeRenderer = new ShapeRenderer();

        // Tạo 1 CharacterSlot cho mỗi nhân vật có trong registry.
        for (CharacterData data : CharacterRegistry.ALL) {
            slots.add(new CharacterSlot(data));
        }
        layoutSlots();

        // tự động chọn sẵn nhân vật đầu tiên khi vào màn hình
        p1Index = 0;
        slots.get(p1Index).setState(CharacterSlot.State.SELECTED);
    }

    /**
     * Tính toán lại vị trí (x, y) của từng slot dựa theo kích thước màn hình hiện tại,
     * sao cho toàn bộ danh sách nhân vật được canh giữa theo chiều ngang trong vùng
     * "giữa" (MID_RATIO) và canh giữa theo chiều dọc màn hình.
     * Được gọi lúc show() và mỗi khi màn hình bị resize.
     */
    private void layoutSlots() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Điểm bắt đầu và bề rộng của vùng "giữa" (nơi đặt danh sách nhân vật).
        float midX = screenW * LEFT_RATIO;
        float midW = screenW * MID_RATIO;

        int count = slots.size();
        // Tổng bề rộng của toàn bộ danh sách slot (kể cả khoảng cách giữa các ô).
        float totalW = count * SLOT_SIZE + (count - 1) * SLOT_GAP;
        // Canh giữa danh sách theo chiều ngang trong vùng giữa.
        float startX = midX + (midW - totalW) / 2f;
        // Canh giữa theo chiều dọc màn hình.
        float y = screenH / 2f - SLOT_SIZE / 2f;

        for (int i = 0; i < count; i++) {
            float x = startX + i * (SLOT_SIZE + SLOT_GAP);
            slots.get(i).setPosition(x, y, SLOT_SIZE);
        }
    }

    /**
     * Xử lý input của Player 1: di chuyển qua lại giữa các ô bằng phím A/D,
     * và xác nhận chọn nhân vật bằng phím J.
     * Nếu ô hiện tại đang CONFIRMING hoặc đã CONFIRMED thì khóa input,
     * không cho di chuyển hay xác nhận lại nữa (tránh đổi nhân vật sau khi đã chốt).
     */
    private void handleInput() {
        CharacterSlot current = slots.get(p1Index);

        // đã xác nhận rồi thì khóa, không cho di chuyển/xác nhận lại nữa
        if (current.getState() == CharacterSlot.State.CONFIRMING
            || current.getState() == CharacterSlot.State.CONFIRMED) {
            return;
        }

        int newIndex = p1Index;

        if (Gdx.input.isKeyJustPressed(Keys.A)) {
            newIndex = p1Index - 1;
        } else if (Gdx.input.isKeyJustPressed(Keys.D)) {
            newIndex = p1Index + 1;
        }

        // giới hạn trong khoảng danh sách, không cho lố ra ngoài
        if (newIndex < 0) newIndex = 0;
        if (newIndex >= slots.size()) newIndex = slots.size() - 1;

        // Nếu có di chuyển sang ô khác: trả ô cũ về NORMAL, đánh dấu ô mới là SELECTED.
        if (newIndex != p1Index) {
            current.setState(CharacterSlot.State.NORMAL);
            p1Index = newIndex;
            slots.get(p1Index).setState(CharacterSlot.State.SELECTED);
        }

        // Nhấn J để bắt đầu xác nhận chọn nhân vật hiện tại (chuyển sang trạng thái CONFIRMING,
        // sau đó CharacterSlot.update() sẽ tự chuyển tiếp sang CONFIRMED sau FLASH_DURATION giây).
        if (Gdx.input.isKeyJustPressed(Keys.J)) {
            slots.get(p1Index).setState(CharacterSlot.State.CONFIRMING);
        }
    }

    /**
     * Vòng lặp vẽ chính, gọi mỗi frame.
     * Thứ tự xử lý:
     *  1. Đọc input và cập nhật trạng thái các slot theo thời gian (delta).
     *  2. Xóa màn hình và cập nhật camera/ma trận chiếu.
     *  3. Vẽ background.
     *  4. Vẽ glow rồi vẽ viền cho tất cả slot (glow phải vẽ trước để border đè lên trên).
     *  5. Vẽ avatar nhân vật lên trên cùng.
     */
    @Override
    public void render(float delta) {

        handleInput();

        for (CharacterSlot s : slots) s.update(delta);

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Vẽ nền màn hình, phủ kín toàn bộ viewport.
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // Vẽ glow trước, viền sau, để border luôn hiển thị rõ nét đè lên trên lớp glow mờ.
        shapeRenderer.begin(ShapeType.Filled);
        for (CharacterSlot s : slots) s.renderGlow(shapeRenderer);
        for (CharacterSlot s : slots) s.renderBorder(shapeRenderer);
        shapeRenderer.end();

        // Vẽ avatar nhân vật sau cùng, nằm trên cùng của viền/glow.
        batch.begin();
        for (CharacterSlot s : slots) s.renderAvatar(batch);
        batch.end();
    }

    /** Được gọi khi kích thước cửa sổ/màn hình thay đổi; tính lại layout cho các slot. */
    @Override
    public void resize(int width, int height) {
        layoutSlots();
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}

    /** Giải phóng tài nguyên GPU (texture, batch, shape renderer) khi màn hình bị hủy. */
    @Override
    public void dispose() {
        backgroundTexture.dispose();
        batch.dispose();
        shapeRenderer.dispose();
    }
}
