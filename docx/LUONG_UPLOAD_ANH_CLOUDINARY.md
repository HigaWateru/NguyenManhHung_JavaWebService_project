# Luong upload anh san voi Cloudinary

Tai lieu nay mo ta luong upload anh san dang co trong code hien tai.

## 1) Thanh phan lien quan

- Config: `src/main/java/demo/project/config/CloudinaryConfig.java`
- Controller: `src/main/java/demo/project/controller/FileController.java`
- Service interface: `src/main/java/demo/project/service/FileStorageService.java`
- Service impl: `src/main/java/demo/project/service/impl/FileStorageServiceImpl.java`
- DTO response: `src/main/java/demo/project/dto/response/FileUploadResponse.java`
- Error handling: `src/main/java/demo/project/exception/GlobalExceptionHandler.java`
- Security rule: `src/main/java/demo/project/config/SecurityConfig.java`

## 2) Endpoint va authentication

- Method: `POST`
- URL: `/api/v1/files/upload/court/{courtId}`
- Content-Type: `multipart/form-data`
- Form field bat buoc: `file`
- Authentication: bat buoc dang nhap (`Authorization: Bearer <access_token>`)

> Theo `SecurityConfig`, route `POST /api/v1/files/**` yeu cau authenticated.

## 3) Cau hinh Cloudinary

Ung dung doc 3 property trong profile dang chay:

- `cloudinary.cloud-name`
- `cloudinary.api-key`
- `cloudinary.api-secret`

`CloudinaryConfig` tao bean `Cloudinary` tu 3 gia tri tren.

## 4) Luong xu ly chi tiet

Khi client goi API upload anh, he thong xu ly theo thu tu:

1. Security filter xac thuc JWT bearer token.
2. `FileController#uploadCourtImage(...)` nhan `courtId` va `MultipartFile file`.
3. Service kiem tra `file` khac null va khong rong.
4. Service tim `Court` theo `courtId`.
5. Service goi Cloudinary uploader de upload bytes cua file.
6. Lay `secure_url` tu ket qua Cloudinary.
7. Gan URL vao truong `image` cua `Court`.
8. Luu lai `Court` va tra `secureUrl` trong `ApiResponse<FileUploadResponse>`.

## 5) Dau vao / dau ra mau

### 5.1 Request (multipart)

- Path variable: `courtId` (vd: `1`)
- Form-data:
  - key: `file`
  - type: File
  - value: chon anh (`.jpg`, `.png`, ...)

### 5.2 Success response

- HTTP status: `200 OK`
- Body mau:

```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "secureUrl": "https://res.cloudinary.com/.../image/upload/...jpg"
  }
}
```

## 6) Cac loi chinh

- `400 Bad Request`:
  - file null/rong -> `File is required`
- `401 Unauthorized`:
  - thieu bearer token / token khong hop le
- `404 Not Found`:
  - `courtId` khong ton tai -> `Court not found`
- `503 Service Unavailable`:
  - loi IO tu cloud storage (duoc map boi `GlobalExceptionHandler`)

## 7) Ghi chu nghiep vu hien tai

- Upload thanh cong se ghi de URL anh moi vao `Court.image`.
- Hien tai chua validate loai file, dung luong, kich thuoc anh, hoac quyen role cu the.
- Hien tai endpoint cho phep moi user da dang nhap goi upload (khong gioi han theo manager/admin).

