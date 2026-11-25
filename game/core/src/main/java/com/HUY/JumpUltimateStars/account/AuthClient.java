package com.HUY.JumpUltimateStars.account;

import java.io.IOException;
import java.io.StringWriter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Gọi API tài khoản của backend (thư mục backend/ trong repo).
 *
 * Mọi request đều là POST + JSON. Gdx.net trả kết quả ở một luồng khác,
 * nên callback được đẩy về luồng vẽ bằng Gdx.app.postRunnable: trong callback
 * có thể đổi màn hình, sửa giao diện thoải mái.
 */
public class AuthClient {

    /** Địa chỉ backend. Đổi ở đây nếu backend chạy ở máy hoặc cổng khác. */
    public static final String SERVER_URL = "http://127.0.0.1:8080";

    private static final int TIMEOUT_MS = 5000;

    public interface Callback {
        /** HTTP 2xx. body là JSON backend trả về. */
        void onSuccess(JsonValue body);

        /** Sai thông tin, trùng tên, mất kết nối... message đã là câu tiếng Việt để hiện lên màn hình. */
        void onError(String message);
    }

    /** Thành công: body có id, username, createdAt. */
    public void login(String username, String password, Callback callback) {
        post("/api/auth/login", json("username", username, "password", password), callback);
    }

    /** Thành công (HTTP 201): body có id, username, createdAt. */
    public void register(String username, String password, Callback callback) {
        post("/api/auth/register", json("username", username, "password", password), callback);
    }

    /** Thành công: body có message. */
    public void changePassword(String username, String currentPassword, String newPassword, Callback callback) {
        post("/api/auth/change-password",
            json("username", username, "currentPassword", currentPassword, "newPassword", newPassword),
            callback);
    }

    private void post(String path, String body, final Callback callback) {
        Net.HttpRequest request = new HttpRequestBuilder().newRequest()
            .method(Net.HttpMethods.POST)
            .url(SERVER_URL + path)
            .header("Content-Type", "application/json")
            .content(body)
            .timeout(TIMEOUT_MS)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                // Phải đọc body ngay trong luồng mạng này, sau đó mới chuyển về luồng vẽ
                final int status = response.getStatus().getStatusCode();
                final String text = response.getResultAsString();
                Gdx.app.postRunnable(() -> deliver(status, text, callback));
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(
                    "Không kết nối được máy chủ. Hãy bật backend rồi thử lại."));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Yêu cầu đã bị huỷ."));
            }
        });
    }

    private static void deliver(int status, String text, Callback callback) {
        JsonValue body = parse(text);
        if (status >= 200 && status < 300) {
            callback.onSuccess(body);
            return;
        }
        // Lỗi nghiệp vụ backend trả HTTP 400 kèm {"message": "..."}
        String message = body != null ? body.getString("message", null) : null;
        if (message == null || message.isEmpty()) {
            message = "Máy chủ báo lỗi (HTTP " + status + ").";
        }
        callback.onError(message);
    }

    private static JsonValue parse(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return new JsonReader().parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Ghép JSON từ các cặp key, value. JsonWriter tự escape dấu " và \ trong mật khẩu. */
    private static String json(String... keyValues) {
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(out);
        writer.setOutputType(JsonWriter.OutputType.json);
        try {
            writer.object();
            for (int i = 0; i < keyValues.length; i += 2) {
                writer.set(keyValues[i], keyValues[i + 1]);
            }
            writer.pop();
        } catch (IOException e) {
            throw new GdxRuntimeException(e);
        }
        return out.toString();
    }
}
