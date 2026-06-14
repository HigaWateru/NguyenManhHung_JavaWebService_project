# Luồng quên mật khẩu bằng OTP qua Gmail

Tài liệu này mô tả luồng quên mật khẩu theo 2 bước:
1) Nhập email để nhận OTP.
2) Nhập lại email + OTP để nhận mật khẩu mới.

## 1) Thành phần liên quan

- Controller: `src/main/java/demo/project/controller/AuthController.java`
- Service interface: `src/main/java/demo/project/service/AuthService.java`
- Service impl: `src/main/java/demo/project/service/impl/AuthServiceImpl.java`
- DTO request:
  - `src/main/java/demo/project/dto/request/ForgotPasswordRequest.java`
  - `src/main/java/demo/project/dto/request/VerifyForgotPasswordOtpRequest.java`
- Entity OTP: `src/main/java/demo/project/entity/PasswordResetOtp.java`
- Repository OTP: `src/main/java/demo/project/repository/PasswordResetOtpRepository.java`

## 2) Cấu hình email sử dụng

Hệ thống gửi OTP bằng `JavaMailSender` với cấu hình trong `application.properties`:

- `spring.mail.host`
- `spring.mail.port`
- `spring.mail.username`
- `spring.mail.password`
- `spring.mail.properties.mail.smtp.auth=true`
- `spring.mail.properties.mail.smtp.starttls.enable=true`

## 3) Endpoint sử dụng

### 3.1 Gửi OTP

- Method: `POST`
- URL: `/api/v1/auth/forgot-password`
- Authentication: `None`
- Body:

```json
{
  "email": "customer.a@badminton.local"
}
```

### 3.2 Xác thực OTP và trả mật khẩu mới

- Method: `POST`
- URL: `/api/v1/auth/forgot-password/verify-otp`
- Authentication: `None`
- Body:

```json
{
  "email": "customer.a@badminton.local",
  "otp": "123456"
}
```

## 4) Luồng xử lý chi tiết

### Bước A - Gửi OTP

1. Client gửi email đến `POST /forgot-password`.
2. Hệ thống kiểm tra email có tồn tại hay không.
3. Hệ thống xóa OTP cũ của user và dọn bản ghi OTP đã hết hạn.
4. Sinh OTP 6 chữ số ngẫu nhiên.
5. Hash OTP rồi lưu vào bảng `password_reset_otps` với thời gian hết hạn.
6. Gửi OTP về email bằng Gmail SMTP.

### Bước B - Verify OTP

1. Client gửi `email + otp` đến `POST /forgot-password/verify-otp`.
2. Hệ thống lấy OTP mới nhất chưa xác thực của user.
3. Kiểm tra OTP còn hạn và chưa vượt số lần nhập sai.
4. So khớp OTP nhập vào với `otp_hash`.
5. Nếu đúng, hệ thống đặt mật khẩu mới mặc định là `123456`.
6. Mật khẩu mới được mã hóa bằng `PasswordEncoder` và cập nhật vào tài khoản user.
7. Đánh dấu OTP đã xác thực và trả về mật khẩu mới trong response.

## 5) Rule bảo mật hiện tại

- OTP gồm 6 chữ số.
- OTP hết hạn sau 5 phút.
- Giới hạn tối đa 5 lần nhập sai OTP cho 1 mã.
- OTP lưu dưới dạng hash (`otp_hash`), không lưu plain text trong DB.
- Mật khẩu trả về là mật khẩu tạm mặc định `123456`, người dùng nên đổi ngay sau khi đăng nhập.

## 6) Các lỗi thường gặp

- `404 Not Found`: email không tồn tại.
- `400 Bad Request`: OTP không tồn tại/hết hạn/sai hoặc bị khóa do nhập sai quá nhiều.
- `503 Service Unavailable`: lỗi gửi mail (SMTP/Gmail tạm thời không khả dụng).

## 7) Ví dụ response thành công

### 7.1 Gửi OTP thành công

```json
{
  "success": true,
  "message": "Password reset request accepted",
  "data": null
}
```

### 7.2 Verify OTP thành công

```json
{
  "success": true,
  "message": "OTP verified, temporary password has been set",
  "data": "123456"
}
```

