# Luồng quản lý ảnh sân với Cloudinary (1 sân nhiều ảnh)

Tài liệu này mô tả luồng CRUD ảnh sau khi chuyển sang hướng entity riêng `CourtImage`.

## 1) Thành phần liên quan

- Entity: `src/main/java/demo/project/entity/CourtImage.java`
- Entity cập nhật: `src/main/java/demo/project/entity/Court.java`
- Repository: `src/main/java/demo/project/repository/CourtImageRepository.java`
- Controller: `src/main/java/demo/project/controller/FileController.java`
- Service interface: `src/main/java/demo/project/service/FileStorageService.java`
- Service impl: `src/main/java/demo/project/service/impl/FileStorageServiceImpl.java`
- DTO response: `src/main/java/demo/project/dto/response/CourtImageResponse.java`
- Security rule: `src/main/java/demo/project/config/SecurityConfig.java`

## 2) Endpoint và xác thực

Tất cả endpoint dưới đây đều cần `Authorization: Bearer <access_token>` và role `MANAGER`:

- `POST /api/v1/files/courts/{courtId}/images` -> Thêm ảnh mới cho sân
- `GET /api/v1/files/courts/{courtId}/images` -> Lấy danh sách ảnh của sân
- `PUT /api/v1/files/courts/{courtId}/images/{imageId}` -> Cập nhật ảnh theo `imageId`
- `DELETE /api/v1/files/courts/{courtId}/images/{imageId}` -> Xóa ảnh theo `imageId`

## 3) Rule validate và phân quyền

### 3.1 Validate file

- Chỉ chấp nhận content type: `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
- Dung lượng tối đa: `50MB`.
- Kích thước tối đa: `5000x5000` pixels.
- File null/rỗng bị từ chối.

### 3.2 Rule quyền quản lý ảnh

- Chỉ manager mới được CRUD ảnh sân.
- Manager chỉ được thao tác trên sân thuộc cụm sân mình quản lý.
- Sai quyền trả `403` với message: `You are not allowed to manage images for this court`.

## 4) Luồng xử lý

### 4.1 Thêm ảnh

1. Xác thực JWT và role manager.
2. Validate file upload.
3. Tìm `Court` theo `courtId` và check manager sở hữu.
4. Upload lên Cloudinary, lấy `secure_url` + `public_id`.
5. Lưu bản ghi mới `CourtImage` (`court_id`, `image_url`, `public_id`).
6. Trả về `CourtImageResponse`.

### 4.2 Cập nhật ảnh

1. Validate file mới.
2. Check manager có quyền trên `courtId`.
3. Tìm ảnh theo `imageId` và `courtId`.
4. Upload ảnh mới lên Cloudinary.
5. Cập nhật `image_url/public_id` trong `CourtImage`.
6. Thử xóa ảnh cũ trên Cloudinary theo `public_id` cũ (best effort).

### 4.3 Xóa ảnh

1. Check manager có quyền trên `courtId`.
2. Tìm ảnh theo `imageId` + `courtId`.
3. Xóa bản ghi `CourtImage` trong DB.
4. Thử xóa file trên Cloudinary theo `public_id` (best effort).

## 5) Các lỗi chính

- `400 Bad Request`: file invalid/quá dung lượng/quá kích thước.
- `401 Unauthorized`: token thiếu/hết hạn/sai.
- `403 Forbidden`: manager không sở hữu sân.
- `404 Not Found`: `courtId` không tồn tại hoặc `imageId` không thuộc sân.
- `503 Service Unavailable`: lỗi IO cloud khi upload ảnh.

## 6) Ghi chú nghiệp vụ

- Đã bỏ hướng lưu 1 URL duy nhất trong `Court.image`; thay bằng quan hệ `Court` 1-n `CourtImage`.
- Mỗi ảnh có `id` riêng để update/xóa chính xác theo `courtId` + `imageId`.
- Giới hạn multipart vẫn giữ nguyên: `50MB`.

