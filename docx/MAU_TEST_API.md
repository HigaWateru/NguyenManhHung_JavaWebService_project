# Mau test API (method, body, authentication, token)

Tai lieu nay tong hop cac mau test nhanh cho nhung chuc nang hien co trong du an.

## 1) Chuan bi bien moi truong

```bash
BASE_URL=http://localhost:8080
ADMIN_USERNAME=admin
MANAGER_USERNAME=manager.a
CUSTOMER_USERNAME=customer.a
PASSWORD=123456
```

> Luu y:
> - Password seed theo `DATA_SEED.md` la `123456`.
> - API duoi `/api/v1/auth/**` khong can bearer token (tru `change-password`, `logout`).
> - API duoi `/api/v1/admin/**`, `/api/v1/manager/**`, `/api/v1/customer/**` can JWT access token.

## 2) Luong lay token (Auth)

### 2.1 Login admin de lay access token + refresh token

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

### 2.8 Forgot password (ban hien tai chi check email ton tai)

- Method: `POST`
- URL: `/api/v1/auth/forgot-password`
- Authentication: `None`

```json
{
  "email": "customer.a@example.com"
}
```

## 3) Test nhom Admin User API

Su dung token cua admin (`Authorization: Bearer <ADMIN_ACCESS_TOKEN>`).

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

## 4) Test nhom Customer Booking API

Su dung token cua customer (`Authorization: Bearer <CUSTOMER_ACCESS_TOKEN>`).

### 4.1 Tao booking

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

### 4.2 Lay booking cua toi

- Method: `GET`
- URL: `/api/v1/customer/bookings`
- Body: `None`

## 5) Test nhom Manager Booking API (duyet/tu choi san thuoc manager)

Su dung token manager (`Authorization: Bearer <MANAGER_ACCESS_TOKEN>`).

### 5.1 Loc booking theo ngay + trang thai trong pham vi manager

- Method: `GET`
- URL: `/api/v1/manager/bookings?date=2026-06-12&status=PENDING`
- Body: `None`

### 5.2 Manager duyet booking

- Method: `PATCH`
- URL: `/api/v1/manager/bookings/{bookingId}/status`

```json
{
  "status": "CONFIRMED"
}
```

### 5.3 Manager tu choi booking

- Method: `PATCH`
- URL: `/api/v1/manager/bookings/{bookingId}/status`

```json
{
  "status": "CANCELLED"
}
```

## 6) Test nhom Admin Booking API (duyet/tu choi toan he thong)

Su dung token admin (`Authorization: Bearer <ADMIN_ACCESS_TOKEN>`).

### 6.1 Loc booking theo ngay + trang thai toan he thong

- Method: `GET`
- URL: `/api/v1/admin/bookings?date=2026-06-12&status=PENDING`
- Body: `None`

### 6.2 Admin duyet booking

- Method: `PATCH`
- URL: `/api/v1/admin/bookings/{bookingId}/status`

```json
{
  "status": "CONFIRMED"
}
```

### 6.3 Admin tu choi booking

- Method: `PATCH`
- URL: `/api/v1/admin/bookings/{bookingId}/status`

```json
{
  "status": "CANCELLED"
}
```

## 7) Mau negative test nen co

1. Login sai password -> mong doi `401`.
2. Goi API admin bang token customer -> mong doi `403`.
3. Duyet booking voi `status = COMPLETED` -> mong doi `400 Invalid status`.
4. Duyet booking khong ton tai -> mong doi `404 Booking not found`.
5. Manager duyet booking khong thuoc cum san minh quan ly -> mong doi `403`.
6. Duyet lai booking da `CONFIRMED`/`CANCELLED` -> mong doi `409`.
7. Tao booking trung khung gio -> mong doi `409 Booking already exists`.

## 8) Huong dan trich token khi test Postman

1. Goi `POST /api/v1/auth/login`.
2. Copy `data.accessToken` gan vao header `Authorization: Bearer <token>`.
3. Copy `data.refreshToken` de test endpoint refresh.
4. Sau khi goi logout, token do bi blacklist va khong dung lai duoc.

