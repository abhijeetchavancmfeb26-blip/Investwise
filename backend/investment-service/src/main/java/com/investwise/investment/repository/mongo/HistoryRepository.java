package com.investwise.investment.repository.mongo;

import com.investwise.investment.model.RecommendationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends MongoRepository<RecommendationHistory, String> {

    Page<RecommendationHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
