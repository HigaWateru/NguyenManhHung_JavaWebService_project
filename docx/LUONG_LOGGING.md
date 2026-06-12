# LUỒNG LOGGING

## 1) Hiện trạng trong dự án

Dự án hiện đang có logging ở tầng AOP thông qua file `src/main/java/demo/project/common/aspect/LoggingAspect.java`.

- Theo dõi method: `BookingServiceImpl.createBooking(..)`
- Khi thành công: ghi log `INFO` với thông tin đặt sân
- Khi thất bại: ghi log `WARN` với lý do lỗi

Hiện tại các file cấu hình:

- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-test.properties`

Từ bản cập nhật này, dự án đã có cấu hình ghi ra file log riêng:

```properties
logging.file.name=logs/project.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n
```

=> Log vẫn hiển thị ở console và đồng thời ghi vào `logs/project.log`.

---

## 2) Luồng logging hiện tại (booking)

```text
Client gọi API đặt sân
        |
        v
Controller -> Service: BookingServiceImpl.createBooking(..)
        |
        +-- Nếu thành công
        |      |
        |      v
        |   AOP @AfterReturning (LoggingAspect.logBookingSuccess)
        |      |
        |      v
        |   ghi INFO: [AUDIT - SUCCESS] ...
        |
        +-- Nếu có exception
               |
               v
            AOP @AfterThrowing (LoggingAspect.logBookingFailure)
               |
               v
            ghi WARN: [AUDIT - FAILED] ...

Sau đó log được output ra console và file log.
```

---

## 3) Gợi ý mở rộng

- Tách audit log booking ra file riêng bằng `logback-spring.xml`
- Thêm rolling policy theo ngày/dung lượng để tránh file log quá lớn
- Bật MDC (`requestId`, `userId`) để trace request dễ hơn

