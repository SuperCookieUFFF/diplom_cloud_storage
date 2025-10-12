package spingcloud.diplom.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import spingcloud.diplom.entity.AuthToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM AuthToken a WHERE a.token = ?1")
    void deleteByToken(String token);

    @Modifying
    @Query("DELETE FROM AuthToken a WHERE a.expiresAt < ?1")
    void deleteExpiredTokens(LocalDateTime now);

    boolean existsByToken(String token);
}
