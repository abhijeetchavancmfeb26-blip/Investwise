package com.investwise.user.repository.jpa;

import com.investwise.user.model.Enums;
import com.investwise.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByPanNumberIgnoreCase(String panNumber);

    Optional<User> findByPhone(String phone);

    /** One query covers the admin console's keyword search and both filters. */
    @Query("""
           SELECT u FROM User u
           WHERE (:keyword IS NULL OR :keyword = ''
                  OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR u.phone            LIKE CONCAT('%', :keyword, '%'))
             AND (:status IS NULL OR u.status = :status)
             AND (:tier   IS NULL OR u.tier = :tier)
           """)
    Page<User> search(@Param("keyword") String keyword,
                      @Param("status") Enums.Status status,
                      @Param("tier") Enums.Tier tier,
                      Pageable pageable);

    long countByStatus(Enums.Status status);

    long countByTier(Enums.Tier tier);

    long countByEmailVerifiedTrue();

    long countByCreatedAtAfter(LocalDateTime since);

    List<User> findTop5ByOrderByCreatedAtDesc();

    /** Monthly registration counts for the admin growth chart. */
    @Query("""
           SELECT FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m'), COUNT(u)
           FROM User u WHERE u.createdAt >= :since
           GROUP BY FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m')
           ORDER BY 1
           """)
    List<Object[]> countByMonth(@Param("since") LocalDateTime since);
}
