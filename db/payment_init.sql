-- Chạy trong MySQL Workbench bằng tài khoản root (và thêm vào db/init.sql)
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'payment_pass';
GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_user'@'%';
FLUSH PRIVILEGES;
-- Bảng `payments` do Hibernate tự tạo (ddl-auto: update) lần chạy đầu.
