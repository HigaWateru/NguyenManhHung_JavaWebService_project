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
