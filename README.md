# Jump Ultimate Stars

Repo gồm hai phần tách riêng:

| Thư mục | Nội dung | Công cụ build |
|---|---|---|
| [`game/`](game/) | Game libGDX (desktop LWJGL3) | Gradle (`gradlew`) |
| [`backend/`](backend/) | API tài khoản: đăng ký, đăng nhập, đổi mật khẩu | Maven (`mvnw`), Spring Boot + MySQL |

Mở từng thư mục như một project riêng trong IDE (IntelliJ: *File → Open* rồi chọn `game` hoặc `backend`).

## Chạy game

```powershell
cd game
.\gradlew.bat lwjgl3:run
```

## Chạy backend

Chạy thử không cần MySQL:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

Chạy với MySQL và danh sách API: xem [`backend/README.md`](backend/README.md).
