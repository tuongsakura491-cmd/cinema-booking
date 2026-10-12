# Cinema Booking (SOA)

React + API Gateway + 4 Spring Boot service (database per service).

## Cổng
Gateway 8080 | Auth 8081 | Movie 8082 | Showtime 8083 | Booking 8084 | Frontend 5173

## Cách chạy
1. Chạy `db/init.sql` trong MySQL Workbench.
2. Mở project bằng IntelliJ (JDK 21), Reload Maven.
3. Chạy lần lượt: AuthServiceApplication, MovieServiceApplication,
   ShowtimeServiceApplication, BookingServiceApplication, GatewayApplication.
4. Frontend: `cd frontend && npm install && npm run dev`, mở http://localhost:5173
5. Tài khoản mặc định: admin / admin123

## Phân công

| Thành viên | Backend | Frontend | Hạ tầng / tài liệu | Kiểm thử |
|---|---|---|---|---|
| **TV1: Nền tảng & Bảo mật** | `auth-service`, `api-gateway`, `payment-service` | `Login.jsx`, `App.jsx` (route guard, navbar), `api.js` | `docker-compose`, `db/*.sql`, đưa secret/mật khẩu ra biến môi trường, chuyển `ddl-auto` sang Flyway | JWT filter, đăng ký/đăng nhập, payment idempotent |
| **TV2: Phim & Suất chiếu** | `movie-service`, `showtime_db` (catalog rạp/phòng/ghế, giữ ghế) | `Movies.jsx`, `MovieDetail.jsx`, `Admin.jsx` | Dữ liệu mẫu (`DataInitializer`), upload poster, Postman cho Movie/Showtime | Giữ ghế đồng thời (all-or-nothing, hết hạn) |
| **TV3: Đặt vé & Thông báo** | `booking-service` (saga, hoàn tiền, job hết hạn), `notification-service` | `SeatPicker.jsx`, `PayPanel.jsx`, `MyBookings.jsx`, `NotificationBell.jsx` | Cập nhật README và báo cáo HTML | Luồng create → confirm → cancel, hoàn tiền, nhắc giờ chiếu |

### Điểm phối hợp giữa các thành viên
- **TV3 ↔ TV2:** hợp đồng API `hold / confirm / release` (`HoldRequest`, `HoldResponse`). Ai đổi DTO phải báo bên còn lại.
- **TV3 ↔ TV1:** `PaymentClient.pay / refund` và header `X-User-Id` do Gateway gắn.
- **Cả nhóm:** dùng chung `JWT_SECRET` và `INTERNAL_TOKEN` qua biến môi trường (sau khi TV1 hoàn tất phần cấu hình).

### Lộ trình gợi ý
1. **Tuần 1:** TV1 hoàn thiện docker-compose và biến môi trường để cả nhóm chạy cùng một môi trường.
2. **Tuần 2:** mỗi thành viên hoàn thiện backend và frontend phần mình phụ trách.
3. **Tuần 3:** viết test, chạy thử toàn luồng và cập nhật báo cáo.

## Additional services

The notification and payment services are Maven modules at:

- `notification-service` (port 8086)
- `payment-service` (port 8085)

Run `db/init.sql` again as a MySQL administrator to create their databases and users. Then reload Maven and start `NotificationServiceApplication` and `PaymentServiceApplication`. The API Gateway routes authenticated requests under `/api/notifications/**` and `/api/payments/**`.

Booking confirmation now uses Payment Service; confirmed bookings receive a reminder through Notification Service up to `REMINDER_MINUTES` before showtime (default 60 minutes).