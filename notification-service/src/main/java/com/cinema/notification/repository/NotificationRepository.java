package com.cinema.notification.repository;

import com.cinema.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByDedupeKey(String dedupeKey);

    @Query("select n from Notification n where n.userId = :uid "
            + "and (n.expiresAt is null or n.expiresAt > :now) order by n.createdAt desc")
    List<Notification> findVisible(@Param("uid") Long uid, @Param("now") LocalDateTime now, Pageable page);

    @Query("select count(n) from Notification n where n.userId = :uid and n.seen = false "
            + "and (n.expiresAt is null or n.expiresAt > :now)")
    long countUnread(@Param("uid") Long uid, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("update Notification n set n.seen = true where n.userId = :uid and n.seen = false")
    int markAllRead(@Param("uid") Long uid);
}
