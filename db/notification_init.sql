-- Thêm vào db/init.sql (hoặc chạy riêng trong MySQL Workbench bằng tài khoản root)
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'notification_user'@'%' IDENTIFIED BY 'notification_pass';
GRANT ALL PRIVILEGES ON notification_db.* TO 'notification_user'@'%';
FLUSH PRIVILEGES;
-- Bảng `notifications` do Hibernate tự tạo (ddl-auto: update) lần chạy đầu.
