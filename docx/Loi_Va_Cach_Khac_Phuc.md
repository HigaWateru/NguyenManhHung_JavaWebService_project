### Báo cáo lỗi và Giải pháp khắc phục (Service isEnabled)

#### 1. Mô tả lỗi (Issue)
- **Vấn đề chính**: Hệ thống gặp lỗi khi truy cập và thiết lập trạng thái `isEnabled` của đối tượng `User`.
- **Nguyên nhân kỹ thuật**: 
    - Trong thực thể `User.java`, trường trạng thái được định nghĩa là `private Boolean isEnabled;`.
    - Khi sử dụng **Lombok** (`@Getter`, `@Setter`, `@Builder`), đối với kiểu dữ liệu `Boolean` (wrapper class) có tiền tố `is`, Lombok sẽ tạo ra:
        - Getter: `getIsEnabled()`
        - Setter: `setIsEnabled()`
        - Builder method: `isEnabled(Boolean isEnabled)`
    - Tuy nhiên, trong mã nguồn ở các lớp Service, Mapper và Security, lập trình viên đang gọi phương thức `user.isEnabled()`. Phương thức này không tồn tại theo quy tắc đặt tên mặc định của Lombok cho kiểu `Boolean` có tiền tố `is`, dẫn đến lỗi biên dịch (Compilation Error).

#### 2. Giải pháp khắc phục
- **Thay đổi kiểu dữ liệu và tên trường**:
    - Chuyển `Boolean isEnabled` thành `boolean enabled` (kiểu nguyên thủy - primitive).
    - **Tại sao?**: Với kiểu `boolean` nguyên thủy, Lombok sẽ tự động tạo getter là `isEnabled()` và setter là `setEnabled()`. Điều này giúp mã nguồn đồng nhất và tuân thủ đúng chuẩn JavaBeans.
- **Cập nhật các vị trí liên quan**:
    - **Thực thể (Entity)**: Cập nhật `User.java`.
    - **Repository**: Đổi tên phương thức truy vấn `findByIdAndIsEnabledTrue` thành `findByIdAndEnabledTrue` trong `UserRepository.java` để khớp với tên trường mới.
    - **Dịch vụ (Service)**: Cập nhật `AuthServiceImpl.java` và `UserServiceImpl.java` để sử dụng đúng tên phương thức builder (`enabled()`) và setter (`setEnabled()`).
    - **Bảo mật (Security)**: `CustomUserDetails.java` và `DtoMapper.java` hiện tại đã có thể gọi `user.isEnabled()` một cách chính xác.

#### 3. Công nghệ sử dụng để Fix lỗi
- **Java Persistence API (JPA)**: Cấu hình lại `@Column(name = "is_enabled")` để ánh xạ đúng vào cột trong cơ sở dữ liệu mặc dù tên trường trong code đã thay đổi.
- **Lombok**: Sử dụng cơ chế tạo mã tự động của Lombok một cách chính xác theo chuẩn quy tắc đặt tên của Java cho kiểu dữ liệu boolean.
- **Spring Data JPA**: Sử dụng Query Methods để cập nhật lại các truy vấn theo tên trường mới.

#### 4. Ghi chú bổ sung
- Khi làm việc với Lombok và các trường Boolean, tốt nhất nên đặt tên trường là `enabled` (kiểu `boolean`). Lombok sẽ tự tạo `isEnabled()` (getter) và `setEnabled()` (setter), giúp code sạch sẽ và tránh nhầm lẫn.

---

### Báo cáo lỗi và Giải pháp khắc phục (JWT - Illegal base64 character)

#### 1. Mô tả lỗi (Issue)
- **API bị lỗi**: `POST /api/v1/auth/login`
- **Thông báo lỗi**:
  - `Internal Server Error`
  - `Illegal base64 character: '-'`
- **Ảnh hưởng**: Người dùng không thể đăng nhập vì hệ thống lỗi khi tạo/đọc JWT.

#### 2. Nguyên nhân kỹ thuật
- Trong `JwtService.java`, secret JWT được decode bằng `Decoders.BASE64.decode(...)`.
- Giá trị `jwt.secret` hiện tại chứa ký tự `-` (thuộc định dạng URL-safe Base64 hoặc plain text), nên Base64 chuẩn có thể ném lỗi `IllegalArgumentException`.
- Lỗi này xảy ra trong quá trình xử lý JWT và bị đẩy lên thành HTTP `500`.

#### 3. Giải pháp đã áp dụng
- Cập nhật `src/main/java/demo/project/security/jwt/JwtService.java` để xử lý secret theo thứ tự:
  1. Thử decode bằng `BASE64`
  2. Nếu fail, thử `BASE64URL`
  3. Nếu vẫn fail, fallback sang bytes của plain text (`UTF-8`)
- Mục tiêu: hệ thống chấp nhận linh hoạt secret hiện có, tránh crash khi login.

#### 4. Kết quả xác nhận
- Đã chạy test project bằng Gradle và không phát sinh lỗi regression sau khi sửa.

#### 5. Khuyến nghị
- Nên chuẩn hóa `jwt.secret` theo một định dạng thống nhất (khuyến nghị Base64/Base64URL hợp lệ và đủ độ dài cho thuật toán HMAC).
- Có thể bổ sung validate lúc startup để fail-fast với thông báo rõ ràng khi secret không hợp lệ.

---

### Báo cáo lỗi và Giải pháp khắc phục (Manager Booking - thiếu query param `date`/`status`)

#### 1. Mô tả lỗi (Issue)
- **API bị lỗi**: `GET /api/v1/manager/bookings`
- **Request thực tế**: gọi endpoint với Bearer token manager nhưng **không truyền query param**.
- **Thông báo nhận được**:
  - `Required request parameter 'date' for method parameter type LocalDate is not present`
  - Response đang trả `status: 500`.

#### 2. Nguyên nhân kỹ thuật
- Trong `ManagerBookingController.java`, method `byDateAndStatus(...)` khai báo:
  - `@RequestParam LocalDate date`
  - `@RequestParam BookingStatus status`
- Hai tham số này là **bắt buộc**. Khi không truyền, Spring ném `MissingServletRequestParameterException`.
- `GlobalExceptionHandler.java` hiện chưa có handler riêng cho exception này, nên rơi vào `@ExceptionHandler(Exception.class)` và bị map thành HTTP `500`.

#### 3. Kết luận lỗi bạn đang gặp
- **Lỗi gốc**: thiếu query param bắt buộc (`date`, `status`) khi gọi endpoint manager booking.
- **Vì sao thấy 500**: do cơ chế bắt lỗi tổng quát hiện tại, không phải lỗi server thực sự.
- **HTTP đúng theo semantics nên là**: `400 Bad Request`.

#### 4. Cách gọi đúng endpoint
- Ví dụ hợp lệ:
  - `GET /api/v1/manager/bookings?date=2026-06-11&status=CONFIRMED`
- `status` phải thuộc enum `BookingStatus`: `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`.

#### 5. Khuyến nghị cải thiện
- Bổ sung handler cho `MissingServletRequestParameterException` trong `GlobalExceptionHandler` để trả `400` và message rõ ràng hơn.
- Có thể thêm ví dụ query param vào tài liệu API để tránh gọi thiếu tham số.

---

### Báo cáo lỗi và Giải pháp khắc phục (Search User 500 do Redis không kết nối được)

#### 1. Mô tả lỗi (Issue)
- **API bị ảnh hưởng**: `GET /api/v1/admin/users?keyword=...&page=...&size=...`
- **Thông báo lỗi**:
  - `status: 500 Internal Server Error`
  - `RedisConnectionFailureException: Unable to connect to Redis`
- **Bối cảnh**: API có Bearer token hợp lệ nhưng request vẫn fail khi Redis đang down/chưa chạy.

#### 2. Nguyên nhân kỹ thuật
- Trong `JwtAuthenticationFilter.java`, mỗi request có token sẽ gọi `RedisTokenBlacklistService.isAccessTokenBlacklisted(...)` để kiểm tra token đã revoke chưa.
- Khi Redis không truy cập được, `StringRedisTemplate` ném `RedisConnectionFailureException`.
- Exception này không được xử lý tại service blacklist nên bị đẩy lên thành HTTP `500`.

#### 3. Giải pháp đã áp dụng
- Cập nhật `src/main/java/demo/project/security/jwt/RedisTokenBlacklistService.java`:
  - Bọc thao tác Redis bằng `try/catch` cho `RedisConnectionFailureException` và `DataAccessException`.
  - Với `isAccessTokenBlacklisted(...)`: fallback trả `false` (không chặn request) khi Redis unavailable để giữ API hoạt động.
  - Với `blacklistAccessToken(...)`: không ném lỗi ra ngoài khi Redis unavailable, chỉ log cảnh báo.
- Bổ sung test mới tại `src/test/java/demo/project/security/jwt/RedisTokenBlacklistServiceTest.java` để xác nhận fallback behavior khi Redis lỗi kết nối.

#### 4. Kết quả mong đợi sau fix
- API search user không còn văng `500` chỉ vì Redis tạm thời không kết nối được.
- Chức năng xác thực JWT vẫn chạy bình thường (trừ khả năng revoke-check bị degrade tạm thời trong lúc Redis down).

#### 5. Khuyến nghị vận hành
- Trong môi trường dev/local: đảm bảo Redis chạy trước khi test logout/revoke token.
- Trong production: nên dùng Redis HA hoặc cơ chế healthcheck + alert để giảm thời gian mất kết nối.

---

### Báo cáo lỗi và Giải pháp khắc phục (Update User báo `Request method 'PUT' is not supported`)

#### 1. Mô tả lỗi (Issue)
- **API bị ảnh hưởng**: update user role trong module admin.
- **Thông báo nhận được**:
  - `Request method 'PUT' is not supported`
  - Response hiển thị `status: 500`.
- **Request gây lỗi phổ biến**: `PUT /api/v1/admin/users` (không có `/{id}`).

#### 2. Nguyên nhân kỹ thuật
- Trong `AdminUserController.java`, endpoint update được khai báo là `@PutMapping("/{id}")` dưới base path `/api/v1/admin/users`.
- URL đúng bắt buộc phải có path variable `id`: `/api/v1/admin/users/{id}`.
- Khi gọi `PUT /api/v1/admin/users`, Spring ném `HttpRequestMethodNotSupportedException`.
- `GlobalExceptionHandler.java` trước đó chưa handle exception này, nên bị bắt bởi handler tổng quát `Exception.class` và trả nhầm `500`.

#### 3. Giải pháp đã áp dụng
- Cập nhật `src/main/java/demo/project/exception/GlobalExceptionHandler.java`:
  - Thêm `@ExceptionHandler(HttpRequestMethodNotSupportedException.class)`.
  - Trả về HTTP `405 Method Not Allowed` với message gốc từ Spring.

#### 4. Cách gọi đúng endpoint update
- URL: `PUT /api/v1/admin/users/{id}`
- Ví dụ: `PUT /api/v1/admin/users/5`
- Payload mẫu:

```json
{
  "username": "manager.updated",
  "email": "manager.updated@badminton.local",
  "password": "managerupdated123",
  "role": "ROLE_ADMIN",
  "enabled": true
}
```

#### 5. Kết quả mong đợi
- Gọi sai URL (`PUT /api/v1/admin/users`) -> trả `405` (thay vì `500`).
- Gọi đúng URL (`PUT /api/v1/admin/users/{id}`) -> xử lý update bình thường theo business logic.

---

### Báo cáo lỗi và Giải pháp khắc phục (Upload ảnh báo `Required part 'file' is not present`)

#### 1. Mô tả lỗi (Issue)
- **API bị ảnh hưởng**: `POST /api/v1/files/courts/{courtId}/images`
- **Thông báo nhận được**:
  - `Required part 'file' is not present.`
  - Response trước khi fix hiển thị `status: 500`.

#### 2. Nguyên nhân kỹ thuật
- Endpoint upload yêu cầu multipart field tên `file`.
- Khi request không gửi đúng part `file` (hoặc body không phải `form-data`), Spring ném `MissingServletRequestPartException`.
- `GlobalExceptionHandler` trước đó chưa có handler riêng cho exception này nên rơi vào handler tổng quát `Exception.class` và trả `500`.

#### 3. Giải pháp đã áp dụng
- Cập nhật `src/main/java/demo/project/exception/GlobalExceptionHandler.java`:
  - Thêm `@ExceptionHandler(MissingServletRequestPartException.class)` -> trả `400 Bad Request`.
  - Thêm `@ExceptionHandler(MissingServletRequestParameterException.class)` -> trả `400 Bad Request`.
- Cập nhật `src/main/java/demo/project/controller/FileController.java`:
  - Endpoint upload/update nhận linh hoạt cả key `file` và `image` trong `form-data`.
  - Nếu không có file hợp lệ, trả message rõ ràng hơn: `File is required. Send multipart/form-data with key 'file' (or 'image').`
- Kết quả: lỗi phía client (thiếu part/thiếu param) không còn bị map sai thành lỗi server `500`.

#### 4. Cách gọi đúng trên Postman (để không thiếu part `file`)
1. Chọn method `POST` và URL `{{BASE_URL}}/api/v1/files/courts/1/images`.
2. Vào tab `Authorization` -> `Bearer Token` -> dán `{{MANAGER_ACCESS_TOKEN}}`.
3. Vào tab `Body` -> chọn `form-data`.
4. Tạo key đúng chính tả: `file`.
5. Ở cột type của key `file`, đổi từ `Text` sang `File`.
6. Bấm `Select Files` và chọn ảnh từ máy.
7. Không tự set cứng header `Content-Type`; để Postman tự set `multipart/form-data`.

#### 5. Kết quả mong đợi sau fix
- Nếu thiếu part `file` -> trả `400` với message rõ ràng.
- Nếu gửi đúng multipart (`file` hợp lệ) -> upload chạy bình thường theo nghiệp vụ/permission hiện có.

#### 6. Trường hợp vẫn gặp `Required part 'file' is not present`
- Kiểm tra nhanh theo thứ tự:
  1. Body đang là `form-data` (không phải `raw`, không phải `binary`).
  2. Key tên `file` hoặc `image`.
  3. Type của key là `File` (không phải `Text`).
  4. Không set cứng header `Content-Type: application/json`.
  5. Trong tab `Headers`, nếu có `Content-Type` tự thêm tay thì xóa đi để Postman tự set `multipart/form-data; boundary=...`.
- Sau khi sửa, gửi lại request mới (không dùng tab cũ bị cache header).

#### 7. Trường hợp báo `No static resource .../api/v1/files/courts/{id}/images`
- Dấu hiệu: response trước đây có thể hiện `status: 500`, message kiểu `No static resource ...`.
- Nguyên nhân thực tế: request không match endpoint API (sai path hoặc sai HTTP method), rồi rơi sang static resource handler.
- Cập nhật đã áp dụng trong `GlobalExceptionHandler`:
  - Handle `NoResourceFoundException` -> trả `404 Not Found` với message rõ hơn.
- Checklist xử lý:
  1. Method đúng là `POST` cho upload mới.
  2. URL đúng: `/api/v1/files/courts/{courtId}/images`.
  3. Không gõ dư ký tự cuối URL (khoảng trắng, xuống dòng).
  4. Nếu vừa đổi code, restart lại app để nạp mapping mới.

