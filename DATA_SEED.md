# Data seed (dev profile)

Seeder được đặt tại `src/main/java/demo/project/seeder/DevDataSeeder.java`.

## Cách hoạt động

- Chỉ chạy khi profile `dev` được active (`@Profile("dev")`).
- Chỉ seed khi các bảng chính chưa có dữ liệu (`users`, `courts`, `bookings`).
- Password mặc định cho các tài khoản seed: `123456`.

## Dữ liệu được tạo

- 5 users: `admin`, `manager.a`, `manager.b`, `customer.a`, `customer.b`
- 2 cụm sân: `SkyBird Arena`, `Phoenix Smash Center`
- 4 sân: `Court A1`, `Court A2`, `Court B1`, `Court B2`
- 4 booking mẫu với trạng thái: `CONFIRMED`, `PENDING`, `COMPLETED`, `CANCELLED`

## Gợi ý kiểm tra nhanh

- Đăng nhập bằng user `admin` / `123456`
- Query DB để kiểm tra các bảng `users`, `badminton_clusters`, `courts`, `bookings`

