package com.HUY.JumpUltimateStars.ui;

import com.badlogic.gdx.math.Rectangle;

/**
 * 1 ô nhập chữ trên màn hình đăng nhập: chỉ giữ nội dung và vị trí,
 * việc vẽ do AuthScreen làm.
 *
 * Chỉ nhận ký tự ASCII in được (32..126) để font luôn vẽ được
 * và khớp với luật tên đăng nhập của backend.
 */
public class InputField {

    public final String label;
    public final boolean password;   // true: hiện dấu * thay cho chữ
    public final int maxLength;
    public final Rectangle bounds = new Rectangle();

    private final StringBuilder text = new StringBuilder();

    public InputField(String label, boolean password, int maxLength) {
        this.label = label;
        this.password = password;
        this.maxLength = maxLength;
    }

    /** Xử lý 1 ký tự từ keyTyped: '\b' là xoá lùi, còn lại thêm vào cuối. */
    public void type(char c) {
        if (c == '\b') {
            if (text.length() > 0) text.setLength(text.length() - 1);
        } else if (c >= 32 && c <= 126 && text.length() < maxLength) {
            text.append(c);
        }
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String value) {
        text.setLength(0);
        if (value != null) text.append(value);
    }

    public void clear() {
        text.setLength(0);
    }

    /** Chuỗi để vẽ lên màn hình: mật khẩu thì thay bằng dấu *. */
    public String displayText() {
        if (!password) return text.toString();
        StringBuilder stars = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) stars.append('*');
        return stars.toString();
    }
}
