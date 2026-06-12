# Luồng upload ảnh sân với Cloudinary

Tài liệu này mô tả luồng upload ảnh sau khi bổ sung validate và phân quyền.

## 1) Thành phần liên quan

- Config: `src/main/java/demo/project/config/CloudinaryConfig.java`
- Controller: `src/main/java/demo/project/controller/FileController.java`
- Service interface: `src/main/java/demo/project/service/FileStorageService.java`
- Service impl: `src/main/java/demo/project/service/impl/FileStorageServiceImpl.java`
- DTO response: `src/main/java/demo/project/dto/response/FileUploadResponse.java`
- Error handling: `src/main/java/demo/project/exception/GlobalExceptionHandler.java`
- Security rule: `src/main/java/demo/project/config/SecurityConfig.java`

## 2) Endpoint và authentication

- Method: `POST`
- URL: `/api/v1/files/upload/court/{courtId}`
- Content-Type: `multipart/form-data`
- Form field bắt buộc: `file`
- Authentication: bắt buộc đăng nhập (`Authorization: Bearer <access_token>`)

> Theo `SecurityConfig`, route `POST /api/v1/files/**` chỉ cho role `MANAGER`.

## 3) Cấu hình Cloudinary

Ứng dụng đọc 3 property trong profile đang chạy:

- `cloudinary.cloud-name`
- `cloudinary.api-key`
- `cloudinary.api-secret`

`CloudinaryConfig` tạo bean `Cloudinary` từ 3 giá trị trên.

## 4) Rule validate và phân quyền

### 4.1 Validate file

- Chỉ chấp nhận content type: `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
- Dung lượng tối đa: `50MB`.
- Kích thước tối đa: `5000x5000` pixels.
- File rỗng/null sẽ bị từ chối.

### 4.2 Rule quyền upload

- Chỉ `MANAGER` được gọi API upload.
- `MANAGER` chỉ được upload ảnh cho sân thuộc cụm sân mà manager đang quản lý.
- Nếu manager upload sân không thuộc quyền -> trả `403`.

## 5) Luồng xử lý chi tiết

Khi client gọi API upload ảnh, hệ thống xử lý theo thứ tự:

1. Security filter xác thực JWT và check role `MANAGER`.
2. `FileController#uploadCourtImage(...)` nhận `courtId`, `MultipartFile file`, `Authentication`.
3. Controller truyền `username` hiện tại xuống service.
4. Service validate file (type, size <= 50MB, image dimensions <= 5000x5000).
5. Service tìm `Court` theo `courtId`.
6. Service check `username` phải là manager của cluster chứa court.
7. Service upload bytes lên Cloudinary.
8. Lấy `secure_url` từ kết quả Cloudinary.
9. Gán URL vào trường `image` của `Court` và save.
10. Trả `ApiResponse<FileUploadResponse>`.

## 6) Đầu vào / đầu ra mẫu

### 6.1 Request (multipart)

- Path variable: `courtId` (vd: `1`)
- Form-data:
  - key: `file`
  - type: File
  - value: chọn ảnh (`.jpg`, `.png`, ...)

### 6.2 Success response

- HTTP status: `200 OK`
- Body mẫu:

```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "secureUrl": "https://res.cloudinary.com/.../image/upload/...jpg"
  }
}
```

## 7) Các lỗi chính

- `400 Bad Request`:
  - file null/rỗng -> `File is required`
- `400 Bad Request`:
  - sai loại file -> `Only image file types are allowed (jpeg, png, webp, gif)`
- `400 Bad Request`:
  - vượt 50MB -> `File size must be less than or equal to 50MB`
- `400 Bad Request`:
  - file ảnh không hợp lệ -> `Invalid image file`
- `400 Bad Request`:
  - kích thước ảnh vượt ngưỡng -> `Image dimensions exceed allowed limit: max 5000x5000 pixels`
- `401 Unauthorized`:
  - thiếu bearer token / token không hợp lệ
- `403 Forbidden`:
  - role không được phép hoặc manager không sở hữu sân -> `You are not allowed to upload image for this court`
- `404 Not Found`:
  - `courtId` không tồn tại -> `Court not found`
- `503 Service Unavailable`:
  - lỗi IO từ cloud storage (được map bởi `GlobalExceptionHandler`)

## 8) Ghi chú nghiệp vụ hiện tại

- Upload thành công sẽ ghi đè URL ảnh mới vào `Court.image`.
- Giới hạn multipart trong app đã đặt: `spring.servlet.multipart.max-file-size=50MB` và `spring.servlet.multipart.max-request-size=50MB`.

