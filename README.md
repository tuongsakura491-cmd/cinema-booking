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
- TV1: Auth, Gateway
- TV2: Movie, Showtime
- TV3: Booking, Frontend

## Additional services

The notification and payment services are Maven modules at:

- `notification-service` (port 8086)
- `payment-service` (port 8085)

Run `db/init.sql` again as a MySQL administrator to create their databases and users. Then reload Maven and start `NotificationServiceApplication` and `PaymentServiceApplication`. The API Gateway routes authenticated requests under `/api/notifications/**` and `/api/payments/**`.

Booking confirmation now uses Payment Service; confirmed bookings receive a reminder through Notification Service up to `REMINDER_MINUTES` before showtime (default 60 minutes).
