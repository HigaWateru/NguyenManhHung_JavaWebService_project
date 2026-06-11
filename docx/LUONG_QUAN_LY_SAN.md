# Luồng quản lý đặt sân (Booking)

Tài liệu này mô tả **toàn bộ luồng quản lý đặt sân** hiện có trong code base, tập trung vào quy tắc kiểm tra trùng giờ.

## 1) Quyền truy cập

Theo `src/main/java/demo/project/config/SecurityConfig.java`:

- `/api/v1/customer/**` -> role `CUSTOMER`
- `/api/v1/manager/**` -> role `MANAGER`
- `/api/v1/admin/**` -> role `ADMIN`

Với phần đặt sân:

- Khách hàng thao tác qua `CustomerBookingController`
- Người quản lý thao tác qua `ManagerBookingController`

## 2) Các API chính

### 2.1 Khách hàng tạo đặt sân

- **Endpoint**: `POST /api/v1/customer/bookings`
- **Controller**: `src/main/java/demo/project/controller/CustomerBookingController.java`
- **Service**: `createBooking(...)` trong `src/main/java/demo/project/service/impl/BookingServiceImpl.java`

Request body (`BookingCreateRequest`):

```json
{
  "courtId": 1,
  "timeSlot": "07:00-09:00",
  "bookingDate": "2026-06-12",
  "totalPrice": 200000
}
```

Validate đầu vào:

- `courtId`: bắt buộc
- `timeSlot`: đúng định dạng `HH:mm-HH:mm`
- `bookingDate`: ngày hiện tại hoặc tương lai

### 2.2 Khách hàng xem danh sách đặt sân của mình

- **Endpoint**: `GET /api/v1/customer/bookings`
- **Controller**: `CustomerBookingController#getMyBookings`

### 2.3 Quản lý duyệt/từ chối đặt sân (phạm vi sân mình quản lý)

- **Endpoint**: `PATCH /api/v1/manager/bookings/{bookingId}/status`
- **Controller**: `ManagerBookingController#updateStatus`
- **Status hợp lệ để cập nhật**: `CONFIRMED` (duyệt), `CANCELLED` (từ chối)
- **Rule phân quyền nghiệp vụ**: manager chỉ được xử lý booking thuộc cụm sân có `manager.username` trùng với tài khoản đăng nhập.

### 2.4 Admin duyệt/từ chối đặt sân (toàn hệ thống)

- **Endpoint**: `PATCH /api/v1/admin/bookings/{bookingId}/status`
- **Controller**: `AdminBookingController#updateStatus`
- **Status hợp lệ để cập nhật**: `CONFIRMED` (duyệt), `CANCELLED` (từ chối)
- **Rule nghiệp vụ**: chỉ booking đang `PENDING` mới được xử lý.

### 2.5 Quản lý lọc đặt sân theo ngày + trạng thái

- **Endpoint**: `GET /api/v1/manager/bookings?date=2026-06-12&status=PENDING`
- **Controller**: `ManagerBookingController#byDateAndStatus`
- **Phạm vi dữ liệu**: chỉ trả về booking của sân thuộc manager hiện tại.

### 2.6 Admin lọc đặt sân theo ngày + trạng thái

- **Endpoint**: `GET /api/v1/admin/bookings?date=2026-06-12&status=PENDING`
- **Controller**: `AdminBookingController#byDateAndStatus`
- **Phạm vi dữ liệu**: toàn bộ booking trong hệ thống.

## 3) Luồng tạo đặt sân (chi tiết)

Khi gọi `POST /api/v1/customer/bookings`, hệ thống xử lý theo thứ tự:

1. Xác thực user theo JWT và lấy `username` từ `Authentication`.
2. Tìm user đang hoạt động (`enabled = true`).
3. Tìm sân (`Court`) theo `courtId`.
4. Parse `timeSlot` thành thời gian bắt đầu/kết thúc.
5. Kiểm tra `start < end` (nếu sai, trả `400`).
6. Lấy tất cả booking cùng sân + cùng ngày, có status `PENDING` hoặc `CONFIRMED`.
7. So từng booking hiện có để kiểm tra giao nhau khung giờ.
8. Nếu có giao nhau -> trả `409 Booking already exists`.
9. Nếu không giao nhau -> tạo booking mới với status mặc định `PENDING`.
10. Trả về `BookingResponse`.

## 4) Cách kiểm tra trùng giờ (chi tiết)

### 4.1 Vì sao logic cũ chưa đủ

Logic cũ dùng `existsByCourtIdAndBookingDateAndTimeSlotAndStatusIn(...)`, tức là chỉ kiểm tra **trùng chuỗi `timeSlot` tuyệt đối**.

Ví dụ:

- Đã có: `07:00-09:00`
- Đặt mới: `08:00-10:00`

Hai chuỗi khác nhau nên có thể lọt qua, dù thực tế bị chồng giờ.

### 4.2 Logic mới đang áp dụng

Code hiện tại trong `BookingServiceImpl#createBooking` chạy theo 3 bước:

1. **Chuẩn hóa khung giờ đầu vào**
   - Tách `timeSlot` theo dấu `-`.
   - Parse từng vế sang `LocalTime`.
   - Validate `start.isBefore(end)`.

2. **Lấy danh sách booking cần so sánh**
   - Query theo `courtId + bookingDate + status in (PENDING, CONFIRMED)`.
   - Mục tiêu: chỉ chặn trùng với booking còn hiệu lực giữ sân.

3. **Kiểm tra overlap theo khoảng thời gian**
   - Công thức giao nhau:
   - `requested.start < existing.end && requested.end > existing.start`

Nếu đúng công thức ở bất kỳ booking nào -> xem là trùng giờ, trả lỗi `409`.

### 4.3 Diễn giải công thức overlap

Hai khoảng thời gian **không giao nhau** khi một khoảng kết thúc trước hoặc đúng lúc khoảng kia bắt đầu.
Ngược lại, sẽ giao nhau nếu:

- Bắt đầu của khoảng mới nhỏ hơn kết thúc của khoảng cũ, **và**
- Kết thúc của khoảng mới lớn hơn bắt đầu của khoảng cũ.

Nhờ đó:

- `07:00-09:00` và `08:00-10:00` -> giao nhau -> **chặn**
- `07:00-09:00` và `09:00-11:00` -> chạm biên tại `09:00` -> **không chặn**
- `07:00-09:00` và `06:00-07:00` -> chạm biên tại `07:00` -> **không chặn**
- `07:00-09:00` và `07:30-08:00` -> nằm trong khoảng cũ -> **chặn**
- `07:00-09:00` và `06:00-10:00` -> bao trùm khoảng cũ -> **chặn**

### 4.4 Các case nên test

- Trùng hoàn toàn: `07:00-09:00` vs `07:00-09:00` -> chặn
- Trùng một phần bên trái: `07:00-09:00` vs `06:00-08:00` -> chặn
- Trùng một phần bên phải: `07:00-09:00` vs `08:00-10:00` -> chặn
- Nằm trong: `07:00-09:00` vs `07:30-08:00` -> chặn
- Bao ngoài: `07:00-09:00` vs `06:00-10:00` -> chặn
- Chạm biên trái/phải: `09:00-11:00`, `05:00-07:00` -> cho phép
- Khung giờ sai: `09:00-07:00` -> `400`

## 5) Trạng thái booking

Theo `BookingStatus`:

- `PENDING`: vừa tạo, chờ quản lý xử lý
- `CONFIRMED`: đã duyệt
- `CANCELLED`: đã hủy
- `COMPLETED`: trạng thái hoàn tất (hiện chưa thấy flow set trạng thái này trong service)

## 6) Luồng phê duyệt/từ chối cho admin và manager

### 6.1 Luồng manager xử lý booking

1. Manager gọi `PATCH /api/v1/manager/bookings/{bookingId}/status` với body `{ "status": "CONFIRMED" | "CANCELLED" }`.
2. Hệ thống validate status chỉ cho phép `CONFIRMED` hoặc `CANCELLED`.
3. Tải booking theo `bookingId`, kiểm tra booking tồn tại.
4. So khớp quyền sở hữu: booking phải thuộc sân nằm trong cụm có manager là user hiện tại.
5. Kiểm tra trạng thái booking đang là `PENDING`.
6. Cập nhật trạng thái và trả về `BookingResponse`.

### 6.2 Luồng admin xử lý booking

1. Admin gọi `PATCH /api/v1/admin/bookings/{bookingId}/status` với body `{ "status": "CONFIRMED" | "CANCELLED" }`.
2. Hệ thống validate status chỉ cho phép `CONFIRMED` hoặc `CANCELLED`.
3. Tải booking theo `bookingId`, kiểm tra booking tồn tại.
4. Kiểm tra trạng thái booking đang là `PENDING`.
5. Cập nhật trạng thái và trả về `BookingResponse`.

### 6.3 Các tình huống lỗi chính

- `400 Invalid status`: status khác `CONFIRMED`/`CANCELLED`.
- `403 You are not allowed to process this booking`: manager xử lý booking không thuộc sân mình quản lý.
- `404 Booking not found`: booking không tồn tại.
- `409 Only pending booking can be approved or rejected`: booking đã được xử lý trước đó.

## 7) Các file liên quan

- `src/main/java/demo/project/controller/CustomerBookingController.java`
- `src/main/java/demo/project/controller/ManagerBookingController.java`
- `src/main/java/demo/project/controller/AdminBookingController.java`
- `src/main/java/demo/project/service/impl/BookingServiceImpl.java`
- `src/main/java/demo/project/repository/BookingRepository.java`
- `src/main/java/demo/project/dto/request/BookingCreateRequest.java`
- `src/main/java/demo/project/common/enums/BookingStatus.java`
- `src/main/java/demo/project/config/SecurityConfig.java`

## 8) Ghi chú mở rộng

Nếu bạn muốn đầy đủ hơn cho “quản lý sân” theo nghĩa CRUD sân (tạo/sửa/xóa sân, bật/tắt khả dụng), hiện tại repo có `Court` và `CourtRepository` nhưng chưa có `CourtController/CourtService` riêng. Có thể bổ sung ở sprint tiếp theo.

