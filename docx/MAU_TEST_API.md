# Mẫu test API (method, body, authentication, token)

Tài liệu này tổng hợp các mẫu test nhanh cho những chức năng hiện có trong dự án.

## 1) Chuẩn bị biến môi trường

```bash
BASE_URL=http://localhost:8080
ADMIN_USERNAME=admin
MANAGER_USERNAME=manager.a
CUSTOMER_USERNAME=customer.a
PASSWORD=123456
```

> Lưu ý:
> - Password seed theo `DATA_SEED.md` là `123456`.
> - API dưới `/api/v1/auth/**` không cần bearer token (trừ `change-password`, `logout`).
> - API dưới `/api/v1/admin/**`, `/api/v1/manager/**`, `/api/v1/customer/**` cần JWT access token.

## 2) Luồng lấy token (Auth)

### 2.1 Login admin để lấy access token + refresh token

- Method: `POST`
- URL: `/api/v1/auth/login`
- Authentication: `None`

```json
{
  "username": "admin",
  "password": "123456"
}
```

### 2.2 Login manager

- Method: `POST`
- URL: `/api/v1/auth/login`
- Authentication: `None`

```json
{
  "username": "manager.a",
  "password": "123456"
}
```

### 2.3 Login customer

- Method: `POST`
- URL: `/api/v1/auth/login`
- Authentication: `None`

```json
{
  "username": "customer.a",
  "password": "123456"
}
```

### 2.4 Refresh token

- Method: `POST`
- URL: `/api/v1/auth/refresh`
- Authentication: `None`

```json
{
  "refreshToken": "<REFRESH_TOKEN>"
}
```

### 2.5 Logout

- Method: `POST`
- URL: `/api/v1/auth/logout`
- Authentication header: `Authorization: Bearer <ACCESS_TOKEN>`
- Body: `None`

### 2.6 Change password

- Method: `POST`
- URL: `/api/v1/auth/change-password`
- Authentication header: `Authorization: Bearer <ACCESS_TOKEN>`

```json
{
  "oldPassword": "123456",
  "newPassword": "1234567"
}
```

### 2.7 Register

- Method: `POST`
- URL: `/api/v1/auth/register`
- Authentication: `None`

```json
{
  "username": "new.customer",
  "email": "new.customer@example.com",
  "password": "123456",
  "role": "ROLE_CUSTOMER"
}
```

### 2.8 Forgot password (gửi OTP về email)

- Method: `POST`
- URL: `/api/v1/auth/forgot-password`
- Authentication: `None`

```json
{
  "email": "customer.a@badminton.local"
}
```

### 2.9 Verify OTP để nhận mật khẩu mới

- Method: `POST`
- URL: `/api/v1/auth/forgot-password/verify-otp`
- Authentication: `None`

```json
{
  "email": "customer.a@badminton.local",
  "otp": "123456"
}
```

> Kết quả thành công trả về `data` là mật khẩu mới tạm thời. Nên đăng nhập và đổi mật khẩu ngay.

## 3) Test nhóm Admin User API

Sử dụng token của admin (`Authorization: Bearer <ADMIN_ACCESS_TOKEN>`).

### 3.1 Search users

- Method: `GET`
- URL: `/api/v1/admin/users?keyword=manager&page=0&size=10`
- Body: `None`

### 3.2 Create user

- Method: `POST`
- URL: `/api/v1/admin/users`

```json
{
  "username": "staff.test",
  "email": "staff.test@example.com",
  "password": "123456",
  "role": "ROLE_MANAGER",
  "enabled": true
}
```

### 3.3 Update user

- Method: `PUT`
- URL: `/api/v1/admin/users/{id}`

```json
{
  "username": "staff.test.updated",
  "email": "staff.updated@example.com",
  "password": "654321",
  "role": "ROLE_MANAGER",
  "enabled": true
}
```

### 3.4 Delete user

- Method: `DELETE`
- URL: `/api/v1/admin/users/{id}`
- Body: `None`

## 4) Test nhóm Customer Booking API

Sử dụng token của customer (`Authorization: Bearer <CUSTOMER_ACCESS_TOKEN>`).

### 4.1 Tạo booking

- Method: `POST`
- URL: `/api/v1/customer/bookings`

```json
{
  "courtId": 1,
  "timeSlot": "07:00-09:00",
  "bookingDate": "2026-06-12",
  "totalPrice": 200000
}
```

### 4.2 Lấy booking của tôi

- Method: `GET`
- URL: `/api/v1/customer/bookings`
- Body: `None`

## 5) Test nhóm Manager Booking API (duyệt/từ chối sân thuộc manager)

Sử dụng token manager (`Authorization: Bearer <MANAGER_ACCESS_TOKEN>`).

### 5.1 Lọc booking theo ngày + trạng thái trong phạm vi manager

- Method: `GET`
- URL: `/api/v1/manager/bookings?date=2026-06-12&status=PENDING`
- Body: `None`

### 5.2 Manager duyệt booking

- Method: `PATCH`
- URL: `/api/v1/manager/bookings/{bookingId}/status`

```json
{
  "status": "CONFIRMED"
}
```

### 5.3 Manager từ chối booking

- Method: `PATCH`
- URL: `/api/v1/manager/bookings/{bookingId}/status`

```json
{
  "status": "CANCELLED"
}
```

## 6) Test nhóm Admin Booking API (duyệt/từ chối toàn hệ thống)

Sử dụng token admin (`Authorization: Bearer <ADMIN_ACCESS_TOKEN>`).

### 6.1 Lọc booking theo ngày + trạng thái toàn hệ thống

- Method: `GET`
- URL: `/api/v1/admin/bookings?date=2026-06-12&status=PENDING`
- Body: `None`

### 6.2 Admin duyệt booking

- Method: `PATCH`
- URL: `/api/v1/admin/bookings/{bookingId}/status`

```json
{
  "status": "CONFIRMED"
}
```

### 6.3 Admin từ chối booking

- Method: `PATCH`
- URL: `/api/v1/admin/bookings/{bookingId}/status`

```json
{
  "status": "CANCELLED"
}
```

## 7) Test nhóm Manager Court Image API (1 sân nhiều ảnh)

Sử dụng token manager (`Authorization: Bearer <MANAGER_ACCESS_TOKEN>`).

### 7.1 Thêm ảnh mới cho sân

- Method: `POST`
- URL: `/api/v1/files/courts/{courtId}/images`
- Body type: `form-data`
- Field:
  - `file` (type `File`)

### 7.2 Lấy danh sách ảnh của sân

- Method: `GET`
- URL: `/api/v1/files/courts/{courtId}/images`
- Body: `None`

### 7.3 Cập nhật ảnh theo imageId

- Method: `PUT`
- URL: `/api/v1/files/courts/{courtId}/images/{imageId}`
- Body type: `form-data`
- Field:
  - `file` (type `File`)

### 7.4 Xóa ảnh theo imageId

- Method: `DELETE`
- URL: `/api/v1/files/courts/{courtId}/images/{imageId}`
- Body: `None`

## 8) Mẫu negative test nên có

1. Login sai password -> mong đợi `401`.
2. Gọi API admin bằng token customer -> mong đợi `403`.
3. Duyệt booking với `status = COMPLETED` -> mong đợi `400 Invalid status`.
4. Duyệt booking không tồn tại -> mong đợi `404 Booking not found`.
5. Manager duyệt booking không thuộc cụm sân mình quản lý -> mong đợi `403`.
6. Duyệt lại booking đã `CONFIRMED`/`CANCELLED` -> mong đợi `409`.
7. Tạo booking trùng khung giờ -> mong đợi `409 Booking already exists`.
8. Upload ảnh với token customer -> mong đợi `403`.
9. Manager A sửa/xóa ảnh của sân thuộc manager B -> mong đợi `403`.
10. Sửa/xóa `imageId` không thuộc `courtId` -> mong đợi `404 Court image not found`.

## 9) Hướng dẫn trích token khi test Postman

1. Gọi `POST /api/v1/auth/login`.
2. Copy `data.accessToken` gắn vào header `Authorization: Bearer <token>`.
3. Copy `data.refreshToken` để test endpoint refresh.
4. Sau khi gọi logout, token đó bị blacklist và không dùng lại được.

