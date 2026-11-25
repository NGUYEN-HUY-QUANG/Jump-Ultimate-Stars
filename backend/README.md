# Jump Ultimate Stars – backend tài khoản

Backend dùng Spring Boot và MySQL. Tài khoản được lưu trong bảng `accounts`; mật khẩu chỉ lưu dưới dạng BCrypt hash.

Mọi lệnh bên dưới chạy trong thư mục `backend`.

## Chạy thử nhanh (không cần MySQL)

Profile `h2` dùng database H2 trong RAM. Tắt ứng dụng là mất dữ liệu, chỉ dùng để thử API.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

## Chạy với MySQL

Cần Java 21 trở lên và MySQL. Mặc định ứng dụng kết nối tới MySQL tại `localhost:3306`, database `jump_ultimate_stars`, user `root` và mật khẩu rỗng. Nếu máy bạn dùng cấu hình khác, đặt các biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` rồi chạy:

```powershell
$env:DB_PASSWORD = "mat_khau_mysql_cua_ban"
.\mvnw.cmd spring-boot:run
```

Nếu báo `Access denied for user 'root'@'localhost' (using password: NO)` thì nghĩa là chưa đặt `DB_PASSWORD`. Database `jump_ultimate_stars` và bảng `accounts` được tạo tự động khi chạy lần đầu.

## Test

Test dùng H2 trong bộ nhớ, không cần MySQL:

```powershell
.\mvnw.cmd test
```

## API

Server chạy ở `http://localhost:8080`. Các endpoint nhận và trả JSON. Không có OTP hoặc chức năng quên mật khẩu.

### Đăng ký — `POST /api/auth/register`

```json
{"username":"player_01","password":"matkhau123"}
```

Tên đăng nhập dài 3–30 ký tự, gồm chữ thường, số và `_`. Mật khẩu dài 8–72 ký tự. Thành công trả về `id`, `username`, `createdAt` (HTTP 201).

### Đăng nhập — `POST /api/auth/login`

```json
{"username":"player_01","password":"matkhau123"}
```

Thông tin đúng trả về hồ sơ tài khoản, không bao gồm mật khẩu hay hash.

### Đổi mật khẩu — `POST /api/auth/change-password`

```json
{"username":"player_01","currentPassword":"matkhau123","newPassword":"matkhau456"}
```

Backend kiểm tra mật khẩu hiện tại trước khi lưu mật khẩu mới. Lỗi nghiệp vụ trả HTTP 400 với `{ "message": "..." }`.
