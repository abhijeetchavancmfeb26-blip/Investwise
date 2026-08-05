package com.investwise.user.repository.jpa;

import com.investwise.user.model.Enums;
import com.investwise.user.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    /** Fetch join so the caller can read the user without a second query. */
    @Query("SELECT t FROM Token t JOIN FETCH t.user WHERE t.token = :token AND t.purpose = :purpose")
    Optional<Token> find(@Param("token") String token, @Param("purpose") Enums.TokenPurpose purpose);

    /** Invalidate outstanding tokens before issuing a replacement. */
    @Modifying
    @Query("UPDATE Token t SET t.used = true WHERE t.user.id = :userId AND t.purpose = :purpose AND t.used = false")
    void invalidateExisting(@Param("userId") Long userId, @Param("purpose") Enums.TokenPurpose purpose);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :cutoff OR t.used = true")
    int purgeStale(@Param("cutoff") LocalDateTime cutoff);
}
