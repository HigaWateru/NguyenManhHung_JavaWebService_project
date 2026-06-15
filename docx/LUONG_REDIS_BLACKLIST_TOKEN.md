# Redis blacklist token - ghi chú để hiểu nhanh

Tài liệu này giải thích đúng theo use case trong dự án: revoke access token khi logout bằng Redis blacklist.

## 1) Redis là gì trong bài toán này

- Redis là kho key-value, đọc/ghi rất nhanh vì dữ liệu nằm trong RAM.
- Dự án không lưu access token đã revoke vào MySQL.
- Thay vào đó, dự án lưu một key blacklist trong Redis, kèm TTL (thời gian sống còn lại).

Ý nghĩa:
- Token vừa logout -> bị chặn ngay lập tức.
- Hết hạn token -> key blacklist tự mất, không cần job dọn dẹp.

## 2) Key đang lưu trong Redis

Dạng key hiện tại:
- `bl:access:<tokenId>`

Trong đó:
- `tokenId` = `jti` của JWT nếu có.
- Nếu JWT không có `jti` thì fallback sang hash SHA-256 của token.

Value:
- Giá trị nhẹ, thường là `"1"`.

TTL:
- Bằng số giây còn lại của access token tại lúc logout.

## 3) Luồng logout và luồng request

### Luồng A - Logout

1. Client gọi `POST /api/v1/auth/logout` với Bearer access token.
2. Server tính `remainingSeconds` của token.
3. Nếu `remainingSeconds <= 0` -> bỏ qua, vì token đã hết hạn.
4. Nếu token còn hạn -> Redis `SET key value EX remainingSeconds`.
5. Access token đó bị đánh dấu revoke ngay.

### Luồng B - Mỗi request có Bearer token

1. Filter tách token từ header `Authorization`.
2. Tạo lại `tokenId` từ token.
3. Kiểm tra `EXISTS bl:access:<tokenId>`.
4. Nếu có key -> trả `403 Token has been revoked`.
5. Nếu không có key -> tiếp tục parse/validate JWT như bình thường.

## 4) Cách hình dung để nhớ

Hình dung Redis blacklist như "bảng đen tạm thời":

- Mỗi token logout = dán 1 tấm thẻ vào bảng đen.
- Tấm thẻ có hẹn giờ tự hủy (TTL).
- Đến giờ, tấm thẻ tự rơi xuống.

Vì vậy:
- Không cần lưu blacklist vĩnh viễn.
- Không cần cron job xóa bảng đen.

## 5) Ví dụ timeline để dễ hiểu

- Access token có hạn 30 phút.
- User logout ở phút thứ 10.
- Redis lưu key blacklist với TTL ~20 phút.
- Trong 20 phút đó: token này bị chặn.
- Sau 20 phút: key tự xóa, vì token cũng đã hết hạn.

## 6) Lệnh Redis để tự kiểm chứng

Khi vừa logout:

```text
SET bl:access:abc123 1 EX 1200
TTL bl:access:abc123
EXISTS bl:access:abc123
```

Sau khi hết TTL:

```text
EXISTS bl:access:abc123   -> 0
TTL bl:access:abc123      -> -2
```

Ghi chú:
- `TTL = -2` nghĩa là key không tồn tại.
- `TTL = -1` nghĩa là key tồn tại nhưng không có thời hạn.

## 7) Dữ liệu nằm ở RAM hay DB?

- Runtime: nằm trong RAM (tốc độ cao).
- Có thể có persistence (RDB/AOF) nếu bật trong Redis config, để khôi phục khi restart.
- Đối với blacklist token, thường chấp nhận dữ liệu tạm thời + TTL.

## 8) Ưu/nhược điểm cách blacklist bằng Redis

Ưu điểm:
- Nhanh.
- Đơn giản.
- Tự động dọn dẹp nhờ TTL.

Nhược điểm:
- Cần Redis sẵn sàng.
- Nếu Redis down, khả năng chặn token đã revoke có thể giảm tạm thời (tùy theo fallback).

## 9) Mapping với code trong dự án

- `AuthServiceImpl.logout(...)`: gọi blacklist token.
- `RedisTokenBlacklistService.blacklistAccessToken(...)`: set key + TTL.
- `JwtAuthenticationFilter`: check blacklist trước khi cho qua security context.
- Prefix key: `bl:access:`.

## 10) Những nhầm lẫn hay gặp

- "Lưu token vào Redis" không phải lưu cả bảng user.
  - Ta chỉ lưu một key đánh dấu revoke.
- Không cần xóa thủ công.
  - TTL sẽ tự xóa.
- Không cần DB quan hệ cho blacklist access token.
  - Redis giải quyết bài toán revoke ngắn hạn rất tốt.

