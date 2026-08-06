package com.investwise.user.repository.mongo;

import com.investwise.user.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByActionAndCreatedAtAfter(String action, LocalDateTime since);
}
