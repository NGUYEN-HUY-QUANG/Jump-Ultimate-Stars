package com.HUY.JumpUltimateStars.screen;

import com.HUY.JumpUltimateStars.hethong.MainGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Man hinh chinh (Main Menu) cua game.
 *
 * Gom 2 phan:
 *  1. Background: 1 tam anh phu kin man hinh.
 *  2. Menu: danh sach lua chon dang cuon vong tron (infinite scroll),
 *     dieu khien bang phim UP / DOWN / ENTER.
 */
public class MainScreen implements Screen {

    // Tham chieu nguoc ve game chinh, dung de chuyen man hinh (setScreen)
    private final MainGame game;

    // ===== Background =====
    private Texture backgroundTexture;
    private SpriteBatch batch;       // dung chung de ve ca background lan text menu
    private OrthographicCamera camera;

    // ===== Menu =====
    private BitmapFont font;
    private GlyphLayout layout; // dung de do kich thuoc text -> canh giua chinh xac

    // Danh sach cac lua chon trong menu, thu tu 0..3
    private final String[] menuItems = {
        "PVP",
        "PVE",
        "VƯỢT ẢI",
        "LUYỆN TẬP",
        "THOÁT"
    };

    /*
     * === CO CHE CUON VO HAN (infinite scroll) ===
     *
     * targetPosition: vi tri LOGIC ma menu can cuon toi.
     *   - Khong gioi han trong khoang [0, menuItems.length).
     *   - Moi lan bam UP/DOWN thi +1/-1, cu the ma tang/giam mai,
     *     khong bao gio bi "kep" lai trong khoang 0..3.
     *   - Vi du bam DOWN lien tuc 5 lan -> targetPosition = 5.
     *
     * scrollPosition: vi tri dang THUC SU hien thi tren man hinh,
     *   duoc lam muot bang lerp() moi frame, duoi theo targetPosition.
     *
     * index thuc su cua item dang duoc CHON (khi bam ENTER):
     *   getCircularIndex(Math.round(targetPosition))
     *
     * Tach rieng "vi tri logic" (targetPosition) va "vi tri hien thi"
     * (scrollPosition) giup animation luon muot, knavigate.
     */
    private float scrollPosition = 0f;
    private float targetPosition = 0f;

    // Toa do tam man hinh, noi item dang chon se duoc "keo" ve
    private float centerX;
    private float centerY;

    // Khoang cach (px) giua 2 item lien tiep theo truc Y
    private final float spacing = 80f;

    // Tai su dung 1 object Color duy nhat, tranh "new Color(...)" moi frame
    // trong vong lap renderMenu() gay rac (garbage) khong can thiet
    private final Color tmpColor = new Color();

    public MainScreen(MainGame game) {
        this.game = game;
    }

    /**
     * Duoc goi 1 lan duy nhat khi Screen nay bat dau hien thi.
     * Noi khoi tao tat ca tai nguyen (texture, font, camera...).
     */
    @Override
    public void show() {
        backgroundTexture = new Texture("background/main_screen.jpg");
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font_chinh.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.characters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyz"
                + "0123456789"
                + " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
                + "ÀÁÂÃÈÉÊẾÌÍÒÓÔÕÙÚĂĐĨŨƠƯ"
                + "àáâãèéêìíòóôõùúăđĩũơư"
                + "ẠẢẤẦẨẪẬẮẰẲẴẶ"
                + "ạảấầẩẫậắằẳẵặ"
                + "ẸẺẼỀỂỄỆ"
                + "ẹẻẽềểễệ"
                + "ỈỊỌỎỐỒỔỖỘỚỜỞỠỢ"
                + "ỉịọỏốồổỗộớờởỡợ"
                + "ỤỦỨỪỬỮỰỲỶỸỴ"
                + "ụủứừửữựỳỷỹỵ";

        // Kích thước chữ
        parameter.size = 25;

        //  Màu chữ bên trong
        parameter.color = Color.WHITE;

        // Viền đen
        parameter.borderWidth = 3;
        parameter.borderColor = Color.BLACK;

        font = generator.generateFont(parameter);

        generator.dispose();
        layout = new GlyphLayout();

        centerX = Gdx.graphics.getWidth() / 2f;
        // Menu dat lech xuong duoi tam man hinh 100px cho thoang,
        centerY = Gdx.graphics.getHeight() / 2f - 100f;
    }

    /**
     * Duoc goi moi frame. Thu tu xu ly:
     *  1. Doc input (UP/DOWN/ENTER)
     *  2. Cap nhat animation cuon menu
     *  3. Ve background + menu len man hinh
     */
    @Override
    public void render(float delta) {
        handleInput();
        updateAnimation(delta);

        // Xoa man hinh voi mau den truoc khi ve frame moi
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // Ve background phu kin toan bo man hinh
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Ve menu de len tren background
        renderMenu();

        batch.end();
    }

    /* ========================= INPUT ========================= */

    /**
     * Doc phim bam trong frame nay.
     * isKeyJustPressed = chi bat 1 lan duy nhat luc vua nhan xuong,
     * khong lap lai lien tuc neu giu phim (khac voi isKeyPressed).
     */
    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            moveDown();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            moveUp();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            selectItem();
        }
    }

    private void moveDown() {
        // Cu tang lien tuc, KHONG gioi han/wrap tai day.
        // Viec "vong tron" duoc xu ly rieng luc render (shortestCircularDistance)
        // va luc chon item (getCircularIndex).
        targetPosition++;
    }

    private void moveUp() {
        targetPosition--;
    }

    /* ========================= ANIMATION ========================= */

    /**
     * Lam muot scrollPosition duoi theo targetPosition moi frame.
     * He so 10f la toc do "bat kip" - so cang lon thi cuon cang nhanh.
     */
    private void updateAnimation(float delta) {
        scrollPosition = MathUtils.lerp(scrollPosition, targetPosition, 5f * delta);
    }

    /* ========================= RENDER MENU ========================= */

    /**
     * Ve tat ca item cua menu, item nao gan tam man hinh (center)
     * thi hien ro va to trang, cang xa thi cang mo va nga sang cam.
     */
    private void renderMenu() {
        /*
         * Vi chi co vai item (4 item) nen duyet qua TAT CA item
         * moi frame, khong can kieu "cua so 3 item" bam theo target/scroll
         * (cach cu de bi loi trong man hinh khi bam phim lien tuc nhanh).
         *
         * Voi moi item, tinh khoang cach VONG TRON NGAN NHAT
         * toi scrollPosition (vi tri dang thuc su hien thi).
         *
         * Cach nay tu dong dung trong moi truong hop, ke ca khi nguoi
         * dung bam lien tuc rat nhanh, vi khong phu thuoc vao viec
         * "cua so" co bam kip target hay khong nua.
         */
        int size = menuItems.length;

        for (int index = 0; index < size; index++) {

            // distance < 0: item nam phia TREN center
            // distance = 0: item dang o CHINH GIUA center
            // distance > 0: item nam phia DUOI center
            float distance = shortestCircularDistance(index, scrollPosition, size);

            // Item nao qua xa center (ngoai vung nhin thay) thi bo qua,
            // khong ton cong ve
            if (Math.abs(distance) > 1.5f) {
                continue;
            }

            String text = menuItems[index];

            // distance am -> y lon hon centerY (ve len tren)
            // distance duong -> y nho hon centerY (ve xuong duoi)
            float y = centerY - distance * spacing;

            // Alpha (do trong suot): o giua = 1 (net hoan toan),
            // cang xa center thi cang mo dan
            float alpha = MathUtils.clamp(1f - Math.abs(distance) * 0.6f, 0f, 1f);

            // whiteAmount: o giua = 1 (trang hoan toan),
            // cang xa center thi cang nga ve mau cam goc
            float whiteAmount = MathUtils.clamp(1f - Math.abs(distance), 0f, 1f);

            // Mau goc la CAM, sau do pha dan sang TRANG khi gan center
            tmpColor.set(1f, 0.5f, 0f, alpha);
            tmpColor.lerp(Color.WHITE, whiteAmount);
            // lerp() voi Color.WHITE co the lam sai lech kenh alpha,
            // nen phai set lai alpha cho dung sau khi lerp
            tmpColor.a = alpha;

            font.setColor(tmpColor);

            drawCenteredText(text, centerX, y);
        }
    }

    /**
     * Tinh khoang cach vong tron NGAN NHAT tu "index" (0..size-1)
     * toi "position" (so thuc bat ky, co the am hoac rat lon vi
     * scrollPosition duoc cong don vo han theo so lan bam phim).
     *
     * Vi du size = 4:
     *
     *   index = 0, position = 3.8
     *   -> item 0 tuong duong voi cac vi tri 4, 8, -4, ...
     *   -> instance gan position nhat la 4 -> distance = 4 - 3.8 = 0.2
     *
     *   index = 3, position = 0.2
     *   -> item 3 tuong duong voi cac vi tri -1, 7, ...
     *   -> instance gan position nhat la -1 -> distance = -1 - 0.2 = -1.2
     *
     * Nho vay item luon duoc "keo" ve center theo huong gan nhat,
     * khong bao gio phai di vong xa hon can thiet.
     */
    private float shortestCircularDistance(int index, float position, int size) {
        // % cho ra gia tri trong khoang (-size, size)
        float raw = (index - position) % size;

        // Ep ve khoang (-size/2, size/2] de luon la duong di NGAN NHAT
        if (raw > size / 2f) {
            raw -= size;
        }

        if (raw < -size / 2f) {
            raw += size;
        }

        return raw;
    }

    /**
     * Chuyen 1 vi tri logic vo han (co the am, co the > size)
     * thanh index thuc trong mang menuItems (luon nam trong 0..size-1).
     *
     * Vi du size = 4: 4 -> 0, 5 -> 1, -1 -> 3, -2 -> 2
     *
     * Dung cho selectItem() de biet chinh xac item nao dang duoc chon.
     */
    private int getCircularIndex(int index) {
        int size = menuItems.length;
        int result = index % size;

        // % trong Java co the tra ve so am (vd -1 % 4 = -1, khong phai 3)
        // nen phai cong them size neu bi am
        if (result < 0) {
            result += size;
        }

        return result;
    }

    /**
     * Ve 1 doan text sao cho tam cua no nam dung tai (x, y).
     * Dung GlyphLayout de do kich thuoc chinh xac (khong dung uoc luong
     * theo so ky tu, vi moi chu co do rong khac nhau).
     */
    private void drawCenteredText(String text, float x, float y) {
        layout.setText(font, text);
        font.draw(batch, layout, x - layout.width / 2f, y + layout.height / 2f);
    }

    /* ========================= CHON MENU ========================= */

    /**
     * Duoc goi khi nguoi choi bam J.
     * Lay dung index cua item dang duoc chon (dua theo targetPosition,
     * TUC LA y dinh cua nguoi choi, khong phai scrollPosition dang
     * chay animation do dang).
     */
    private void selectItem() {
        int index = getCircularIndex(Math.round(targetPosition));

        switch (index) {
            case 0:
                System.out.println("CHON PVP");
                // TODO: game.setScreen(new ManHinhPVP(game));
                break;

            case 1:
                System.out.println("CHON PVE");
                // TODO: game.setScreen(new ManHinhPVE(game));
                break;

            case 2:
                System.out.println("CHON VƯỢT ẢI");
                // TODO: game.setScreen(new ManHinhVuotAi(game));
                break;

            case 3:
                System.out.println("CHON LUYỆN TẬP");
                game.setScreen(new CharacterSelectScreen(game));
                break;

            case 4:
                System.out.println("CHON THOAT");
                Gdx.app.exit();
                break;
        }
    }

    // ========================= VONG DOI SCREEN =========================
    // Cac method duoi day thuoc interface Screen cua LibGDX, hien tai
    // khong can xu ly gi them nen de trong.

    // Goi khi kich thuoc cua so thay doi (khong dung vi game khong cho resize)
    @Override
    public void resize(int width, int height) {}

    // Goi khi game bi tam dung (vd nguoi dung chuyen sang app khac tren mobile)
    @Override
    public void pause() {}

    // Goi khi Screen nay bi thay the boi 1 Screen khac (setScreen sang man hinh khac)
    @Override
    public void hide() {}

    // Goi khi game duoc tiep tuc sau khi bi pause
    @Override
    public void resume() {}

    /**
     * Giai phong tat ca tai nguyen GPU/native (texture, batch, font)
     * khi khong con dung Screen nay nua, tranh memory leak.
     */
    @Override
    public void dispose() {
        backgroundTexture.dispose();
        batch.dispose();
        font.dispose();
    }
}
