# Mô tả toàn bộ luồng hoạt động của dự án

Tài liệu này mô tả các luồng đang có trong mã nguồn hiện tại của dự án Spring Boot tại `src/main/java/demo/project`.

## 1) Tổng quan kiến trúc

Dự án đang đi theo kiến trúc nhiều lớp:

- **Controller**: nhận HTTP request, validate request body, gọi service.
- **Service**: xử lý nghiệp vụ chính, transaction, kiểm tra điều kiện.
- **Repository (Spring Data JPA)**: truy vấn và ghi dữ liệu.
- **Entity**: ánh xạ bảng DB.
- **Security/JWT**: xác thực và phân quyền bằng access token.
- **Exception Handler**: chuẩn hóa lỗi trả về.

Các module chính đang có endpoint:

- `AuthController` (`/api/v1/auth/**`)
- `AdminUserController` (`/api/v1/admin/users`)

## 2) Luồng khởi động ứng dụng

1. Chạy `ProjectApplication.main()`.
2. Spring Boot khởi tạo context, quét bean (`@SpringBootApplication`).
3. Nạp cấu hình từ `application.properties` và profile active (`dev` mặc định).
4. Khởi tạo Security (`SecurityConfig`), bao gồm:
   - `SecurityFilterChain`
   - `AuthenticationProvider` (DAO + `CustomUserDetailsService`)
   - `PasswordEncoder` (`BCryptPasswordEncoder`)
   - `JwtAuthenticationFilter`
5. Kết nối datasource theo profile:
   - `dev`: MySQL (`application-dev.properties`)
   - `test`: H2 in-memory (`application-test.properties`)
6. Nếu profile là `dev`, `DevDataSeeder` chạy khi startup để seed dữ liệu mẫu (chỉ seed khi DB gần như trống).

## 3) Luồng xử lý request tổng quát

Mọi request API đi theo luồng cơ bản:

1. **HTTP request đến Spring Security filter chain**.
2. `JwtAuthenticationFilter` chạy trước `UsernamePasswordAuthenticationFilter`:
   - Nếu không có `Authorization: Bearer ...` -> cho đi tiếp (chưa authenticate).
   - Nếu có token:
      - Kiểm tra token có nằm trong Redis blacklist không (`bl:access:<jti-hoặc-token-hash>`).
     - Parse JWT, lấy `username`, kiểm tra hạn token.
     - Nạp user qua `CustomUserDetailsService`.
     - Nếu hợp lệ, set `Authentication` vào `SecurityContextHolder`.
3. **Phân quyền endpoint** trong `SecurityConfig`:
   - `/api/v1/auth/**` -> `permitAll`.
   - Endpoint còn lại -> bắt buộc authenticated.
4. Controller nhận request, validate DTO (`@Valid`).
5. Service xử lý nghiệp vụ, thao tác repository.
6. Trả về `ApiResponse<T>` nếu thành công.
7. Nếu có lỗi, `GlobalExceptionHandler` chuyển thành `ErrorResponse` chuẩn.

## 4) Luồng xác thực và tài khoản (`/api/v1/auth`)

### 4.1) Login - `POST /api/v1/auth/login`

**Input**: `username`, `password` (`LoginRequest`).

Luồng:

1. `AuthController.login()` gọi `AuthService.login()`.
2. `AuthenticationManager.authenticate(...)` xác thực thông tin đăng nhập.
3. Tìm user theo username và yêu cầu `enabled = true`.
4. Sinh:
   - Access token (chứa claim `role`)
   - Refresh token
5. Xóa refresh token cũ của user (`deleteByUserId`) để mỗi user chỉ giữ 1 refresh token active.
6. Lưu refresh token mới vào bảng `refresh_tokens`.
7. Trả `AuthResponse` gồm access token, refresh token, token type, expiresIn.

### 4.2) Refresh token - `POST /api/v1/auth/refresh`

**Input**: `refreshToken` (`RefreshTokenRequest`).

Luồng:

1. Tìm refresh token trong DB.
2. Kiểm tra:
   - Không `revoked`
   - `expiresAt` chưa quá hạn
3. Nếu hợp lệ, sinh access token mới từ user trong refresh token.
4. Trả lại access token mới + refresh token cũ.

### 4.3) Logout - `POST /api/v1/auth/logout`

**Input**: header `Authorization: Bearer <access_token>`.

Luồng:

1. Tách token từ header, nếu thiếu/không đúng format -> lỗi 401.
2. Tính thời gian sống còn lại (`TTL`) của access token.
3. Ghi key blacklist vào Redis với TTL tương ứng:
   - Key: `bl:access:<jti-hoặc-token-hash>`
   - Value: `1`
   - TTL: bằng thời gian còn lại của token.
4. Nếu token đã hết hạn thì bỏ qua bước ghi blacklist.
5. Trích `username` từ access token.
6. Nếu tìm thấy user, xóa refresh token của user đó.
7. Xóa `SecurityContextHolder`.

Kết quả: access token hiện tại bị vô hiệu hóa ngay qua Redis blacklist, refresh token cũng bị thu hồi.

### 4.4) Register - `POST /api/v1/auth/register`

**Input**: `username`, `email`, `password`, `role` (`RegisterRequest`).

Luồng:

1. Kiểm tra trùng username.
2. Kiểm tra trùng email.
3. Mã hóa password bằng BCrypt.
4. Tạo user mới (`enabled = true`).
5. Trả `UserResponse`.

### 4.5) Change password - `POST /api/v1/auth/change-password`

**Yêu cầu**: phải đăng nhập (dùng `Authentication` hiện tại).

Luồng:

1. Lấy username từ `authentication.getName()`.
2. Tìm user theo username.
3. So khớp `oldPassword` với password hash hiện tại.
4. Nếu đúng, mã hóa `newPassword` và lưu lại.

### 4.6) Forgot password - `POST /api/v1/auth/forgot-password`

Luồng hiện tại:

1. Kiểm tra email có tồn tại hay không.
2. Xóa OTP cũ của user và dọn OTP đã hết hạn.
3. Sinh OTP 6 chữ số, hash OTP rồi lưu DB với thời hạn 5 phút.
4. Gửi OTP qua email bằng Gmail SMTP.
5. Khi gọi `POST /api/v1/auth/forgot-password/verify-otp`, hệ thống xác thực OTP, đặt mật khẩu tạm mặc định `123456`, rồi trả mật khẩu này cho client.

## 5) Luồng quản lý user cho admin (`/api/v1/admin/users`)

> Lưu ý: trong `SecurityConfig`, nhóm endpoint `/api/v1/admin/**` đã được cấu hình `hasRole("ADMIN")`. Vì vậy các API `/api/v1/admin/users` bắt buộc người dùng có `ROLE_ADMIN` mới truy cập được.

### 5.1) Search users - `GET /api/v1/admin/users`

Query params: `keyword`, `page`, `size`.

Luồng:

1. Controller nhận tham số phân trang.
2. `UserService.searchUsers()` gọi repository tìm theo username/email chứa keyword (ignore case).
3. Mapping entity -> `UserResponse`.
4. Gói vào `PageResponse<UserResponse>` và trả về.

### 5.2) Create user - `POST /api/v1/admin/users`

Luồng:

1. Validate `UserUpsertRequest`.
2. Kiểm tra trùng username/email.
3. Nếu request không có password -> dùng mặc định `123456`.
4. Mã hóa password, tạo user, lưu DB.
5. Trả `UserResponse`.

### 5.3) Update user - `PUT /api/v1/admin/users/{id}`

Luồng:

1. Tìm user theo id.
2. Cập nhật username, fullName (đang set bằng username), email, role, enabled.
3. Nếu password mới có giá trị -> mã hóa và cập nhật.
4. Lưu DB, trả `UserResponse`.

### 5.4) Delete user - `DELETE /api/v1/admin/users/{id}`

Luồng:

1. Kiểm tra user tồn tại.
2. Xóa theo id.
3. Trả HTTP 204 No Content.

## 6) Luồng xử lý lỗi

`GlobalExceptionHandler` chuẩn hóa các nhóm lỗi:

- `AppException` -> dùng HTTP status và message do nghiệp vụ ném ra.
- `MethodArgumentNotValidException` -> 400, lấy message của field lỗi đầu tiên.
- `BadCredentialsException` -> 401 `Invalid credentials`.
- `AccessDeniedException` -> 403 `Access denied`.
- `IOException` -> 503 (thông điệp liên quan cloud storage).
- `Exception` tổng quát -> 500.

Định dạng trả lỗi: `ErrorResponse` gồm `timestamp`, `status`, `error`, `message`, `path`.

## 7) Luồng dữ liệu bảo mật token

### Access token (JWT)

- Sinh bởi `JwtService.generateAccessToken(username, role)`.
- Hạn dùng lấy từ `jwt.access-token-minutes`.
- Chứa claim `role`.
- Được kiểm tra ở `JwtAuthenticationFilter` cho mỗi request có bearer token.

### Refresh token

- Sinh bởi `JwtService.generateRefreshToken(username)`.
- Hạn dùng lấy từ `jwt.refresh-token-days`.
- Lưu trong bảng `refresh_tokens` để quản lý vòng đời token.

### Blacklist token

- Khi logout, access token được ghi vào Redis với key `bl:access:<jti-hoặc-token-hash>`.
- Redis tự xóa key khi hết TTL, không cần dọn thủ công như blacklist trong DB.
- Filter từ chối request nếu key blacklist còn tồn tại.

### Luồng Redis blacklist chi tiết (access token)

#### Thành phần tham gia

- `AuthServiceImpl.logout(...)`: nhận access token từ header Bearer khi user logout.
- `RedisTokenBlacklistService`: ghi/check key blacklist trong Redis.
- `JwtService`:
  - `resolveTokenId(token)`: lấy `jti` trong JWT; nếu không có thì fallback sang `SHA-256(token)`.
  - `getRemainingSeconds(token)`: tính TTL còn lại của token tại thời điểm logout.
- `JwtAuthenticationFilter`: chạy mỗi request có Bearer token để chặn token đã revoke.

#### Luồng 1 - User logout

```text
Client gọi POST /api/v1/auth/logout (Bearer access_token)
        |
        v
AuthServiceImpl.logout()
        |
        +-- JwtService.getRemainingSeconds(token) -> remainingSeconds
        |      |
        |      +-- nếu <= 0: token đã hết hạn, bỏ qua ghi Redis
        |
        +-- JwtService.resolveTokenId(token) -> tokenId (jti hoặc hash)
        |
        +-- Redis SET bl:access:<tokenId> = "1" EX <remainingSeconds>
        |
        +-- xóa refresh token trong DB + clear SecurityContext
```

#### Luồng 2 - Request API có Bearer token

```text
Client gọi API bất kỳ có Bearer access_token
        |
        v
JwtAuthenticationFilter
        |
        +-- RedisTokenBlacklistService.isAccessTokenBlacklisted(token)
               |
               +-- nếu key tồn tại -> trả 403 "Token has been revoked"
               +-- nếu key không tồn tại -> tiếp tục parse/validate JWT
```

#### Cơ chế "tự động xóa" trong Redis

- Hệ thống **không tự chạy job xóa blacklist**.
- Key blacklist được set kèm TTL = thời gian sống còn lại của access token tại lúc logout.
- Khi TTL về `0`, Redis tự evict key.
- Ý nghĩa: token đã hết hạn thì key blacklist cũng tự biến mất, tránh phình dữ liệu.

Ví dụ timeline:

- Access token có hạn 30 phút.
- User logout ở phút thứ 10.
- Hệ thống set key blacklist với TTL khoảng 20 phút.
- Sau 20 phút, Redis tự xóa key, vì token đó cũng không còn hợp lệ nữa.

#### Trạng thái suy giảm khi Redis không sẵn sàng

- Theo code hiện tại trong `RedisTokenBlacklistService`:
  - Nếu Redis lỗi khi ghi blacklist lúc logout -> log `WARN`, không làm fail logout.
  - Nếu Redis lỗi khi check blacklist lúc request -> log `WARN` và fallback `false` (coi như chưa blacklist).
- Mục tiêu: giữ API không bị `500` khi Redis tạm thời down.
- Đánh đổi: trong khoảng Redis down, khả năng chặn token đã revoke sẽ giảm tạm thời.

## 8) Luồng seed dữ liệu dev

`DevDataSeeder` chạy khi:

- profile active là `dev`.
- các bảng chính chưa có dữ liệu (`users`, `courts`, `bookings`).

Seeder tạo:

- 5 users (admin, manager, customer)
- 2 badminton clusters
- 4 courts
- 4 bookings mẫu với trạng thái khác nhau

Password mặc định dữ liệu seed: `123456`.

## 9) Luồng persistence và quan hệ dữ liệu

Quan hệ chính giữa các entity:

- `User` 1-n `Booking`
- `Court` 1-n `Booking`
- `BadmintonCluster` 1-n `Court`
- `User` 1-1 `BadmintonCluster` (manager)
- `User` n-1 `TokenBlacklist`
- `RefreshToken` n-1 `User`

Dữ liệu booking có `@PrePersist` để tự gán `createdAt` khi insert.

## 10) Luồng đặt sân đã được mở endpoint

Luồng booking hiện đã được triển khai đầy đủ từ controller -> service -> repository:

- Controller:
  - `CustomerBookingController` (`/api/v1/customer/bookings`)
  - `ManagerBookingController` (`/api/v1/manager/bookings`)
  - `AdminBookingController` (`/api/v1/admin/bookings`)
- Service: `BookingService`, `BookingServiceImpl`
- Repository: `BookingRepository`

Nghiệp vụ chính đang có:

1. Customer tạo booking theo sân/ngày/khung giờ.
2. Hệ thống kiểm tra trùng khung giờ với các booking đang `PENDING` hoặc `CONFIRMED`.
3. Booking mới được tạo ở trạng thái `PENDING`.
4. Manager của cụm sân hoặc Admin có thể cập nhật trạng thái booking (`CONFIRMED`/`CANCELLED`).
5. Có API lọc booking theo ngày + trạng thái cho Manager/Admin.

## 11) Tóm tắt ngắn các entrypoint API hiện tại

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/change-password`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/forgot-password/verify-otp`
- `POST /api/v1/customer/bookings`
- `GET /api/v1/customer/bookings`
- `PATCH /api/v1/manager/bookings/{bookingId}/status`
- `GET /api/v1/manager/bookings`
- `PATCH /api/v1/admin/bookings/{bookingId}/status`
- `GET /api/v1/admin/bookings`
- `GET /api/v1/admin/users`
- `POST /api/v1/admin/users`
- `PUT /api/v1/admin/users/{id}`
- `DELETE /api/v1/admin/users/{id}`

---

Nếu cần, có thể tách tài liệu này thành 3 file riêng: **luồng auth**, **luồng admin user**, **luồng dữ liệu booking/cluster** để dễ bảo trì theo từng module.
