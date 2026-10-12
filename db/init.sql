CREATE DATABASE IF NOT EXISTS auth_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS movie_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS showtime_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS booking_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'auth_user'@'%'     IDENTIFIED BY 'auth_pass';
CREATE USER IF NOT EXISTS 'movie_user'@'%'    IDENTIFIED BY 'movie_pass';
CREATE USER IF NOT EXISTS 'showtime_user'@'%' IDENTIFIED BY 'showtime_pass';
CREATE USER IF NOT EXISTS 'booking_user'@'%'  IDENTIFIED BY 'booking_pass';

GRANT ALL PRIVILEGES ON auth_db.*     TO 'auth_user'@'%';
GRANT ALL PRIVILEGES ON movie_db.*    TO 'movie_user'@'%';
GRANT ALL PRIVILEGES ON showtime_db.* TO 'showtime_user'@'%';
GRANT ALL PRIVILEGES ON booking_db.*  TO 'booking_user'@'%';
FLUSH PRIVILEGES;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'notification_user'@'%' IDENTIFIED BY 'notification_pass';
GRANT ALL PRIVILEGES ON notification_db.* TO 'notification_user'@'%';

CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'payment_pass';
GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_user'@'%';

FLUSH PRIVILEGES;
