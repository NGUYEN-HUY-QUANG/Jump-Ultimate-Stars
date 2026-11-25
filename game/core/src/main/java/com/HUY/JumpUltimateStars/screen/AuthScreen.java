package com.HUY.JumpUltimateStars.screen;

import java.util.ArrayList;
import java.util.List;

import com.HUY.JumpUltimateStars.account.AuthClient;
import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.HUY.JumpUltimateStars.ui.InputField;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Màn hình tài khoản, 3 chế độ dùng chung 1 khung:
 *  - LOGIN: màn hình đầu tiên khi mở game.
 *  - REGISTER: tạo tài khoản mới, xong quay về LOGIN.
 *  - CHANGE_PASSWORD: mở từ menu chính khi đã đăng nhập.
 *
 * Khác các màn hình khác (đọc phím bằng isKeyJustPressed), màn hình này cần
 * gõ chữ nên nhận phím qua InputProcessor (keyTyped / keyDown).
 * Điều khiển: TAB hoặc mũi tên lên/xuống để chuyển ô, ENTER để xác nhận,
 * ESC để quay lại; cũng có thể bấm chuột vào ô hoặc nút.
 */
public class AuthScreen extends InputAdapter implements Screen {

    public enum Mode { LOGIN, REGISTER, CHANGE_PASSWORD }

    // ===== Bố cục (cửa sổ cố định 1000x600) =====
    private static final float PANEL_X = 130f, PANEL_Y = 115f, PANEL_W = 740f, PANEL_H = 465f;
    private static final float TITLE_Y = 545f;
    private static final float SUBTITLE_Y = 500f;
    private static final float LABEL_RIGHT = 430f;   // nhãn căn phải, kết thúc tại đây
    private static final float FIELD_X = 450f, FIELD_W = 380f, FIELD_H = 44f;
    private static final float FIRST_ROW_Y = 450f, ROW_GAP = 60f;
    private static final float BUTTON_W = 340f, BUTTON_H = 44f, BUTTON_GAP = 55f;
    private static final float HINT_Y = 75f;

    private static final Color ORANGE = new Color(1f, 0.5f, 0f, 1f);
    private static final Color ERROR_COLOR = new Color(1f, 0.4f, 0.4f, 1f);
    private static final Color SUCCESS_COLOR = new Color(0.45f, 1f, 0.45f, 1f);
    private static final Color LABEL_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color HINT_COLOR = new Color(0.8f, 0.8f, 0.8f, 1f);

    // Đủ chữ tiếng Việt để hiện câu báo lỗi từ backend
    private static final String VIETNAMESE =
        "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
            + "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ"
            + "–";

    /** 1 nút bấm: chữ + việc cần làm + vùng bấm chuột. */
    private static class Button {
        final String label;
        final Runnable action;
        final Rectangle bounds = new Rectangle();

        Button(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    private final MainGame game;
    private final AuthClient client = new AuthClient();

    private Mode mode;
    private final List<InputField> fields = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    // Ô đang được chọn: 0..fields-1 là ô nhập, từ fields.size() trở đi là nút
    private int focus;
    private float messageY;

    private String message = "";
    private Color messageColor = Color.WHITE;
    private boolean busy;        // đang chờ backend trả lời -> khoá bàn phím, chuột
    private boolean disposed;
    private float stateTime;     // dùng cho con trỏ nhấp nháy

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private Texture background;
    private BitmapFont titleFont, font, smallFont;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector3 touch = new Vector3();

    public AuthScreen(MainGame game, Mode mode) {
        this.game = game;
        this.mode = mode;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        background = new Texture("background/main_screen.jpg");

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font_chinh.ttf"));
        String characters = fontCharacters();
        titleFont = makeFont(generator, characters, 40, 3);
        font = makeFont(generator, characters, 22, 2);
        smallFont = makeFont(generator, characters, 17, 2);
        generator.dispose();

        setMode(mode);
        Gdx.input.setInputProcessor(this);
    }

    /* ========================= CHẾ ĐỘ ========================= */

    /** Dựng lại ô nhập và nút cho chế độ mới. Tên đăng nhập được giữ khi chuyển LOGIN <-> REGISTER. */
    private void setMode(Mode newMode) {
        String keptUsername = (!fields.isEmpty() && mode != Mode.CHANGE_PASSWORD) ? fields.get(0).getText() : "";
        mode = newMode;
        fields.clear();
        buttons.clear();
        focus = 0;
        message = "";

        switch (newMode) {
            case LOGIN:
                fields.add(new InputField("Tên đăng nhập", false, 30));
                fields.add(new InputField("Mật khẩu", true, 72));
                buttons.add(new Button("ĐĂNG NHẬP", this::submit));
                buttons.add(new Button("TẠO TÀI KHOẢN MỚI", () -> setMode(Mode.REGISTER)));
                buttons.add(new Button("THOÁT GAME", () -> Gdx.app.exit()));
                break;
            case REGISTER:
                fields.add(new InputField("Tên đăng nhập", false, 30));
                fields.add(new InputField("Mật khẩu", true, 72));
                fields.add(new InputField("Nhập lại mật khẩu", true, 72));
                buttons.add(new Button("ĐĂNG KÝ", this::submit));
                buttons.add(new Button("QUAY LẠI ĐĂNG NHẬP", () -> setMode(Mode.LOGIN)));
                break;
            case CHANGE_PASSWORD:
                fields.add(new InputField("Mật khẩu hiện tại", true, 72));
                fields.add(new InputField("Mật khẩu mới", true, 72));
                fields.add(new InputField("Nhập lại mật khẩu mới", true, 72));
                buttons.add(new Button("ĐỔI MẬT KHẨU", this::submit));
                buttons.add(new Button("QUAY LẠI MENU", () -> goTo(new MainScreen(game))));
                break;
        }

        if (newMode != Mode.CHANGE_PASSWORD && !keptUsername.isEmpty()) {
            fields.get(0).setText(keptUsername);
            focus = 1;
        }
        layoutWidgets();
    }

    /** Xếp ô nhập từ trên xuống, rồi tới các nút, cuối cùng là dòng thông báo. */
    private void layoutWidgets() {
        float centerX = Gdx.graphics.getWidth() / 2f;
        float y = FIRST_ROW_Y;
        for (InputField field : fields) {
            field.bounds.set(FIELD_X, y - FIELD_H / 2f, FIELD_W, FIELD_H);
            y -= ROW_GAP;
        }
        y -= 15f;
        for (Button button : buttons) {
            button.bounds.set(centerX - BUTTON_W / 2f, y - BUTTON_H / 2f, BUTTON_W, BUTTON_H);
            y -= BUTTON_GAP;
        }
        messageY = y;
    }

    /* ========================= GỬI LÊN BACKEND ========================= */

    private void submit() {
        switch (mode) {
            case LOGIN:
                submitLogin();
                break;
            case REGISTER:
                submitRegister();
                break;
            case CHANGE_PASSWORD:
                submitChangePassword();
                break;
        }
    }

    private void submitLogin() {
        final String username = fields.get(0).getText().trim();
        String password = fields.get(1).getText();
        if (username.isEmpty() || password.isEmpty()) {
            showError("Hãy nhập tên đăng nhập và mật khẩu.");
            return;
        }
        startRequest("Đang đăng nhập...");
        client.login(username, password, new Reply() {
            @Override
            void ok(JsonValue body) {
                game.setLoggedInUser(body != null ? body.getString("username", username) : username);
                goTo(new MainScreen(game));
            }
        });
    }

    private void submitRegister() {
        final String username = fields.get(0).getText().trim();
        String password = fields.get(1).getText();
        String again = fields.get(2).getText();
        if (username.isEmpty() || password.isEmpty() || again.isEmpty()) {
            showError("Hãy điền đủ cả 3 ô.");
            return;
        }
        if (!password.equals(again)) {
            showError("Mật khẩu nhập lại không khớp.");
            focus = 2;
            return;
        }
        startRequest("Đang tạo tài khoản...");
        client.register(username, password, new Reply() {
            @Override
            void ok(JsonValue body) {
                // Backend trả tên đã chuẩn hoá (chữ thường), điền sẵn để đăng nhập luôn
                String created = body != null ? body.getString("username", username) : username;
                setMode(Mode.LOGIN);
                fields.get(0).setText(created);
                focus = 1;
                showSuccess("Đăng ký thành công! Hãy nhập mật khẩu để đăng nhập.");
            }
        });
    }

    private void submitChangePassword() {
        String username = game.getLoggedInUser();
        String current = fields.get(0).getText();
        String newPassword = fields.get(1).getText();
        String again = fields.get(2).getText();
        if (username == null) {
            showError("Bạn chưa đăng nhập.");
            return;
        }
        if (current.isEmpty() || newPassword.isEmpty() || again.isEmpty()) {
            showError("Hãy điền đủ cả 3 ô.");
            return;
        }
        if (!newPassword.equals(again)) {
            showError("Mật khẩu mới nhập lại không khớp.");
            focus = 2;
            return;
        }
        startRequest("Đang đổi mật khẩu...");
        client.changePassword(username, current, newPassword, new Reply() {
            @Override
            void ok(JsonValue body) {
                for (InputField field : fields) field.clear();
                focus = 0;
                showSuccess(body != null
                    ? body.getString("message", "Đổi mật khẩu thành công.")
                    : "Đổi mật khẩu thành công.");
            }
        });
    }

    /**
     * Callback dùng chung: bỏ qua nếu màn hình đã đóng, mở khoá phím,
     * lỗi thì hiện chữ đỏ. Chỉ cần viết phần xử lý khi thành công.
     */
    private abstract class Reply implements AuthClient.Callback {
        @Override
        public final void onSuccess(JsonValue body) {
            if (disposed) return;
            busy = false;
            ok(body);
        }

        @Override
        public final void onError(String errorMessage) {
            if (disposed) return;
            busy = false;
            showError(errorMessage);
        }

        abstract void ok(JsonValue body);
    }

    private void startRequest(String text) {
        busy = true;
        message = text;
        messageColor = Color.WHITE;
    }

    private void showError(String text) {
        message = text;
        messageColor = ERROR_COLOR;
    }

    private void showSuccess(String text) {
        message = text;
        messageColor = SUCCESS_COLOR;
    }

    /* ========================= INPUT ========================= */

    @Override
    public boolean keyDown(int keycode) {
        if (busy) return true;
        int count = fields.size() + buttons.size();
        switch (keycode) {
            case Keys.TAB:
                boolean shift = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
                focus = shift ? (focus - 1 + count) % count : (focus + 1) % count;
                return true;
            case Keys.DOWN:
                focus = (focus + 1) % count;
                return true;
            case Keys.UP:
                focus = (focus - 1 + count) % count;
                return true;
            case Keys.ENTER:
            case Keys.NUMPAD_ENTER:
                // Đang ở nút thì bấm nút đó, đang ở ô nhập thì gửi luôn form
                if (focus >= fields.size()) buttons.get(focus - fields.size()).action.run();
                else submit();
                return true;
            case Keys.ESCAPE:
                back();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyTyped(char character) {
        if (busy || focus >= fields.size()) return false;
        // LWJGL3 gửi Backspace thành '\b' (kể cả khi giữ phím); TAB/ENTER đã xử lý ở keyDown
        if (character == '\b' || (character >= 32 && character <= 126)) {
            fields.get(focus).type(character);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (busy) return true;
        camera.unproject(touch.set(screenX, screenY, 0));
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).bounds.contains(touch.x, touch.y)) {
                focus = i;
                return true;
            }
        }
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).bounds.contains(touch.x, touch.y)) {
                focus = fields.size() + i;
                buttons.get(i).action.run();
                return true;
            }
        }
        return false;
    }

    private void back() {
        if (mode == Mode.REGISTER) setMode(Mode.LOGIN);
        else if (mode == Mode.CHANGE_PASSWORD) goTo(new MainScreen(game));
        // LOGIN: không làm gì, tránh lỡ tay thoát game
    }

    private void goTo(Screen next) {
        game.setScreen(next);
        dispose();
    }

    /* ========================= VẼ ========================= */

    @Override
    public void render(float delta) {
        stateTime += delta;
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);

        // Lớp 1: ảnh nền
        batch.begin();
        batch.draw(background, 0, 0, width, height);
        batch.end();

        // Lớp 2: khung mờ, ô nhập, nút (cần bật blend để màu có độ trong suốt)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        for (int i = 0; i < fields.size(); i++) {
            Rectangle r = fields.get(i).bounds;
            if (i == focus) shapes.setColor(ORANGE);
            else shapes.setColor(0.5f, 0.5f, 0.5f, 1f);
            shapes.rect(r.x - 2f, r.y - 2f, r.width + 4f, r.height + 4f);   // viền
            shapes.setColor(0.08f, 0.08f, 0.08f, 0.95f);
            shapes.rect(r.x, r.y, r.width, r.height);
        }
        for (int i = 0; i < buttons.size(); i++) {
            Rectangle r = buttons.get(i).bounds;
            if (fields.size() + i == focus) shapes.setColor(ORANGE);
            else shapes.setColor(0.22f, 0.22f, 0.22f, 0.95f);
            shapes.rect(r.x, r.y, r.width, r.height);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Lớp 3: chữ
        batch.begin();
        drawCentered(titleFont, title(), ORANGE, width / 2f, TITLE_Y);
        if (mode == Mode.CHANGE_PASSWORD && game.getLoggedInUser() != null) {
            drawCentered(smallFont, "Tài khoản: " + game.getLoggedInUser(), LABEL_COLOR, width / 2f, SUBTITLE_Y);
        }
        for (int i = 0; i < fields.size(); i++) {
            drawField(fields.get(i), i == focus);
        }
        for (Button button : buttons) {
            Rectangle r = button.bounds;
            drawCentered(font, button.label, Color.WHITE, r.x + r.width / 2f, r.y + r.height / 2f);
        }
        if (!message.isEmpty()) {
            layout.setText(smallFont, message, messageColor, PANEL_W - 40f, Align.center, true);
            smallFont.draw(batch, layout, PANEL_X + 20f, messageY + layout.height / 2f);
        }
        drawCentered(smallFont, hint(), HINT_COLOR, width / 2f, HINT_Y);
        batch.end();
    }

    private void drawField(InputField field, boolean focused) {
        Rectangle r = field.bounds;
        float centerY = r.y + r.height / 2f;

        // Nhãn bên trái, căn phải
        layout.setText(font, field.label, LABEL_COLOR, 0f, Align.left, false);
        font.draw(batch, layout, LABEL_RIGHT - layout.width, centerY + layout.height / 2f);

        // Nội dung: dài quá ô thì chỉ hiện phần cuối (phần đang gõ)
        String shown = field.displayText();
        float maxWidth = r.width - 24f;
        layout.setText(font, shown, Color.WHITE, 0f, Align.left, false);
        while (layout.width > maxWidth && shown.length() > 0) {
            shown = shown.substring(1);
            layout.setText(font, shown, Color.WHITE, 0f, Align.left, false);
        }
        float textX = r.x + 12f;
        float textY = centerY + font.getCapHeight() / 2f;
        float shownWidth = layout.width;
        font.draw(batch, layout, textX, textY);

        // Con trỏ nhấp nháy ở ô đang chọn
        if (focused && !busy && (stateTime % 1f) < 0.5f) {
            layout.setText(font, "|", ORANGE, 0f, Align.left, false);
            font.draw(batch, layout, textX + shownWidth + 2f, textY);
        }
    }

    private void drawCentered(BitmapFont f, String text, Color color, float centerX, float centerY) {
        layout.setText(f, text, color, 0f, Align.left, false);
        f.draw(batch, layout, centerX - layout.width / 2f, centerY + layout.height / 2f);
    }

    private String title() {
        switch (mode) {
            case REGISTER: return "ĐĂNG KÝ";
            case CHANGE_PASSWORD: return "ĐỔI MẬT KHẨU";
            default: return "ĐĂNG NHẬP";
        }
    }

    private String hint() {
        String base = "TAB / mũi tên: chuyển ô     ENTER: xác nhận";
        return mode == Mode.LOGIN ? base : base + "     ESC: quay lại";
    }

    /* ========================= FONT ========================= */

    /** Bộ ký tự mặc định của FreeType + toàn bộ chữ tiếng Việt, bỏ trùng. */
    private static String fontCharacters() {
        String all = FreeTypeFontGenerator.DEFAULT_CHARS + VIETNAMESE;
        StringBuilder unique = new StringBuilder();
        for (int i = 0; i < all.length(); i++) {
            char c = all.charAt(i);
            if (unique.indexOf(String.valueOf(c)) < 0) unique.append(c);
        }
        return unique.toString();
    }

    private static BitmapFont makeFont(FreeTypeFontGenerator generator, String characters, int size, int border) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = characters;
        parameter.size = size;
        parameter.color = Color.WHITE;
        parameter.borderWidth = border;
        parameter.borderColor = Color.BLACK;
        return generator.generateFont(parameter);
    }

    /* ========================= VÒNG ĐỜI SCREEN ========================= */

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == this) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (background != null) background.dispose();
        if (titleFont != null) titleFont.dispose();
        if (font != null) font.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
