# Mẫu test API trên Postman + kiểm tra report unit test

Tài liệu này bám theo dữ liệu seed mới tại `src/main/java/demo/project/seeder/DevDataSeeder.java` và các controller hiện có trong dự án.

## 1) Chuẩn bị Postman environment

Tạo environment và thêm các biến sau:

- `BASE_URL = http://localhost:8080`
- `ADMIN_USERNAME = admin.root`
- `ADMIN_PASSWORD = adminroot123`
- `MANAGER_USERNAME = manager.a`
- `MANAGER_PASSWORD = managera123`
- `CUSTOMER_USERNAME = customer.a`
- `CUSTOMER_PASSWORD = customera123`
- `ADMIN_ACCESS_TOKEN =` (để trống ban đầu)
- `MANAGER_ACCESS_TOKEN =` (để trống ban đầu)
- `CUSTOMER_ACCESS_TOKEN =` (để trống ban đầu)
- `REFRESH_TOKEN =` (để trống ban đầu)

> Lưu ý:
> - Password seed hiện dùng theo bộ dễ nhớ trong `DevDataSeeder` (ví dụ: `adminroot123`, `managera123`, `customera123`).
> - API dưới `/api/v1/admin/**`, `/api/v1/manager/**`, `/api/v1/customer/**`, `/api/v1/files/**` cần bearer token đúng role.

## 2) Bộ tài khoản seed để test nhanh

| Role | Username | Password |
|---|---|---|
| Admin | `admin.root` | `adminroot123` |
| Manager | `manager.a` | `managera123` |
| Customer | `customer.a` | `customera123` |

Tham khảo đầy đủ danh sách account tại `docx/DATA_SEED.md`.

## 3) Luồng Auth cơ bản (lấy token)

### 3.1 Login (dùng cho admin/manager/customer)

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/auth/login`
- Authentication: `None`

```json
{
  "username": "{{ADMIN_USERNAME}}",
  "password": "{{ADMIN_PASSWORD}}"
}
```

Sau khi gọi thành công, copy `data.accessToken` và `data.refreshToken` vào biến môi trường tương ứng.

### 3.2 Refresh token

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/auth/refresh`

```json
{
  "refreshToken": "{{REFRESH_TOKEN}}"
}
```

### 3.3 Logout (đang dùng Redis blacklist)

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/auth/logout`
- Header: `Authorization: Bearer {{ADMIN_ACCESS_TOKEN}}`

Kỳ vọng: logout thành công, token cũ bị thu hồi ngay.

### 3.4 Forgot password + verify OTP

1) Gửi OTP:

- `POST {{BASE_URL}}/api/v1/auth/forgot-password`

```json
{
  "email": "customer.a@badminton.local"
}
```

2) Verify OTP:

- `POST {{BASE_URL}}/api/v1/auth/forgot-password/verify-otp`

```json
{
  "email": "customer.a@badminton.local",
  "otp": "123456"
}
```

Kỳ vọng: `data` trả về mật khẩu tạm `123456`.

## 4) Test Admin User API

Header chung: `Authorization: Bearer {{ADMIN_ACCESS_TOKEN}}`

### 4.1 Search users

- Method: `GET`
- URL: `{{BASE_URL}}/api/v1/admin/users?keyword=manager&page=0&size=10`

### 4.2 Create user

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/admin/users`

```json
{
  "username": "staff.test",
  "email": "staff.test@example.com",
  "password": "StaffTest@2026",
  "role": "ROLE_MANAGER",
  "enabled": true
}
```

### 4.3 Update user

- Method: `PUT`
- URL: `{{BASE_URL}}/api/v1/admin/users/{id}`

```json
{
  "username": "staff.test.updated",
  "email": "staff.updated@example.com",
  "password": "StaffUpdated@2026",
  "role": "ROLE_MANAGER",
  "enabled": true
}
```

### 4.4 Delete user

- Method: `DELETE`
- URL: `{{BASE_URL}}/api/v1/admin/users/{id}`

## 5) Test Booking API theo role

### 5.1 Customer tạo booking

- Header: `Authorization: Bearer {{CUSTOMER_ACCESS_TOKEN}}`
- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/customer/bookings`

```json
{
  "courtId": 1,
  "timeSlot": "07:00-08:30",
  "bookingDate": "2026-06-20",
  "totalPrice": 200000
}
```

### 5.2 Customer xem booking của mình

- Header: `Authorization: Bearer {{CUSTOMER_ACCESS_TOKEN}}`
- Method: `GET`
- URL: `{{BASE_URL}}/api/v1/customer/bookings`

### 5.3 Manager lọc booking trong phạm vi cụm sân quản lý

- Header: `Authorization: Bearer {{MANAGER_ACCESS_TOKEN}}`
- Method: `GET`
- URL: `{{BASE_URL}}/api/v1/manager/bookings?date=2026-06-20&status=PENDING`

### 5.4 Manager duyệt/hủy booking

- Header: `Authorization: Bearer {{MANAGER_ACCESS_TOKEN}}`
- Method: `PATCH`
- URL: `{{BASE_URL}}/api/v1/manager/bookings/{bookingId}/status`

```json
{
  "status": "CONFIRMED"
}
```

### 5.5 Admin duyệt/hủy booking toàn hệ thống

- Header: `Authorization: Bearer {{ADMIN_ACCESS_TOKEN}}`
- Method: `PATCH`
- URL: `{{BASE_URL}}/api/v1/admin/bookings/{bookingId}/status`

```json
{
  "status": "CANCELLED"
}
```

## 6) Test File API (manager)

Header chung: `Authorization: Bearer {{MANAGER_ACCESS_TOKEN}}`

- `POST {{BASE_URL}}/api/v1/files/courts/{courtId}/images` (form-data: `file`)
- `GET {{BASE_URL}}/api/v1/files/courts/{courtId}/images`
- `PUT {{BASE_URL}}/api/v1/files/courts/{courtId}/images/{imageId}` (form-data: `file`)
- `DELETE {{BASE_URL}}/api/v1/files/courts/{courtId}/images/{imageId}`

### 6.1 Chuẩn bị ảnh để test

- Tạo sẵn thư mục ảnh test, ví dụ: `D:\code\Rest API\project\docx\test-images`
- Đặt 2 file ảnh để test nhanh:
  - `court-ok.png` (ảnh nhỏ, ví dụ 500x500)
  - `court-update.jpg` (ảnh khác để test update)
- Định dạng nên dùng: `png`, `jpg`, `jpeg`, `webp`, `gif`

### 6.2 Cách chọn ảnh upload trong Postman (rất chi tiết)

Áp dụng cho API `POST /api/v1/files/courts/{courtId}/images`:

1. Tạo request mới trong Postman.
2. Chọn method `POST`.
3. URL ví dụ: `{{BASE_URL}}/api/v1/files/courts/1/images`
4. Vào tab `Authorization`:
   - Type: `Bearer Token`
   - Token: `{{MANAGER_ACCESS_TOKEN}}`
5. Vào tab `Body` -> chọn `form-data`.
6. Thêm 1 dòng key:
   - Cột **Key**: nhập `file`
   - Cột bên phải key (mặc định là `Text`): đổi từ `Text` sang **`File`**
7. Sau khi chọn `File`, cột Value sẽ có nút **Select Files**.
8. Bấm **Select Files** -> chọn ảnh từ máy (ví dụ `court-ok.png`) -> Open.
9. Bấm `Send`.

> Quan trọng:
> - Key bắt buộc phải đúng tên `file`.
> - Nếu để `Text` thay vì `File`, backend sẽ không nhận được ảnh.
> - Không cần tự set `Content-Type: multipart/form-data`, Postman sẽ tự set đúng boundary.

### 6.3 Luồng test đầy đủ cho File API

#### Bước A - Upload ảnh mới

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/files/courts/1/images`
- Body: `form-data` với `file` (kiểu `File`)
- Kỳ vọng: `200/201` (tùy controller), trả về thông tin ảnh đã upload.

#### Bước B - Lấy danh sách ảnh của court

- Method: `GET`
- URL: `{{BASE_URL}}/api/v1/files/courts/1/images`
- Kỳ vọng: danh sách có ảnh vừa upload, lấy `imageId` để dùng cho bước C/D.

#### Bước C - Update ảnh theo `imageId`

- Method: `PUT`
- URL: `{{BASE_URL}}/api/v1/files/courts/1/images/{imageId}`
- Body: `form-data` với `file` (chọn `court-update.jpg`)
- Kỳ vọng: ảnh được cập nhật thành URL/public id mới.

#### Bước D - Xóa ảnh

- Method: `DELETE`
- URL: `{{BASE_URL}}/api/v1/files/courts/1/images/{imageId}`
- Kỳ vọng: xóa thành công, gọi lại GET sẽ không còn ảnh đó.

### 6.4 Lỗi thường gặp khi chọn ảnh upload

1. **Không thấy nút Select Files**
   - Nguyên nhân: key đang để `Text`.
   - Cách xử lý: đổi kiểu của key sang `File`.

2. **Backend báo thiếu file / invalid request**
   - Nguyên nhân: key không phải `file` hoặc body không phải `form-data`.
   - Cách xử lý: dùng đúng key `file`, tab Body là `form-data`.

3. **403 Forbidden**
   - Nguyên nhân: dùng token sai role hoặc manager không sở hữu court.
   - Cách xử lý: dùng `{{MANAGER_ACCESS_TOKEN}}` đúng manager của cụm sân.

4. **400 Only image file types are allowed**
   - Nguyên nhân: upload file không phải ảnh (txt/pdf...).
   - Cách xử lý: chọn file đuôi ảnh hợp lệ (`png/jpg/jpeg/webp/gif`).

5. **400 File size must be less than or equal to 50MB**
   - Nguyên nhân: ảnh quá lớn.
   - Cách xử lý: giảm dung lượng ảnh trước khi upload.

6. **400 Image dimensions exceed allowed limit**
   - Nguyên nhân: kích thước ảnh vượt ngưỡng hệ thống cho phép.
   - Cách xử lý: resize ảnh xuống nhỏ hơn giới hạn.

## 7) Negative test gợi ý

1. Login sai password -> `401`.
2. Dùng token customer gọi `/api/v1/admin/users` -> `403`.
3. Duyệt booking với `status=COMPLETED` -> `400`.
4. Duyệt booking không tồn tại -> `404`.
5. Tạo booking trùng khung giờ -> `409`.
6. Gọi file API bằng token customer -> `403`.

## 8) Cách kiểm tra report unit test trong dự án

### 8.1 Chạy test + tạo report

```powershell
Set-Location "D:\code\Rest API\project"
.\gradlew.bat clean test jacocoTestReport
```

### 8.2 Xem kết quả test JUnit

- Mở file: `build/reports/tests/test/index.html`

### 8.3 Xem độ bao phủ JaCoCo

- Mở file: `build/reports/jacoco/test/html/index.html`

### 8.4 Bộ test hiện có liên quan controller/service

- `src/test/java/demo/project/controller/AdminUserControllerTest.java` (5 test)
- `src/test/java/demo/project/service/impl/UserServiceImplTest.java` (5 test)
- `src/test/java/demo/project/service/impl/AuthServiceImplForgotPasswordTest.java`
- `src/test/java/demo/project/service/impl/FileStorageServiceImplTest.java`

