# Data seed (dev profile)

Seeder được đặt tại `src/main/java/demo/project/seeder/DevDataSeeder.java`.

## Cách hoạt động

- Chỉ chạy khi profile `dev` được active (`@Profile("dev")`).
- Chỉ seed khi các bảng chính chưa có dữ liệu (`users`, `courts`, `bookings`).
- Mỗi tài khoản seed có mật khẩu riêng, không dùng chung `123456`.

## Dữ liệu được tạo

- 12 users:
  - 2 admin: `admin.root`, `admin.ops`
  - 4 manager: `manager.a`, `manager.b`, `manager.c`, `manager.d`
  - 6 customer: `customer.a` -> `customer.f`
- 4 cụm sân: `SkyBird Arena`, `Phoenix Smash Center`, `Dragon Shuttle Hub`, `Riverside Badminton House`
- 12 sân (mỗi cụm 3 sân): `A1-A3`, `B1-B3`, `C1-C3`, `D1-D3`
- 12 booking mẫu với đủ trạng thái: `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`

## Tài khoản seed và mật khẩu

| Username | Role | Mật khẩu |
|---|---|---|
| `admin.root` | `ROLE_ADMIN` | `Adm1nRoot@2026` |
| `admin.ops` | `ROLE_ADMIN` | `Adm1nOps@2026` |
| `manager.a` | `ROLE_MANAGER` | `MngA@2026!` |
| `manager.b` | `ROLE_MANAGER` | `MngB@2026!` |
| `manager.c` | `ROLE_MANAGER` | `MngC@2026!` |
| `manager.d` | `ROLE_MANAGER` | `MngD@2026!` |
| `customer.a` | `ROLE_CUSTOMER` | `CusA@2026#` |
| `customer.b` | `ROLE_CUSTOMER` | `CusB@2026#` |
| `customer.c` | `ROLE_CUSTOMER` | `CusC@2026#` |
| `customer.d` | `ROLE_CUSTOMER` | `CusD@2026#` |
| `customer.e` | `ROLE_CUSTOMER` | `CusE@2026#` |
| `customer.f` | `ROLE_CUSTOMER` | `CusF@2026#` |

## Gợi ý kiểm tra nhanh

- Đăng nhập nhanh bằng `admin.root` / `Adm1nRoot@2026`
- Query DB để kiểm tra các bảng `users`, `badminton_clusters`, `courts`, `bookings`

