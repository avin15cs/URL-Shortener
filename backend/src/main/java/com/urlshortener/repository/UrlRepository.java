package com.urlshortener.repository;

import com.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findByCustomAlias(String customAlias);

    boolean existsByShortCode(String shortCode);

    boolean existsByCustomAlias(String customAlias);

    List<Url> findAllByIsActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT u FROM Url u WHERE u.ownerUid = :ownerUid AND u.isActive = true "
            + "AND (u.expiresAt IS NULL OR u.expiresAt > CURRENT_TIMESTAMP) ORDER BY u.createdAt DESC")
    List<Url> findAllActiveAndNonExpiredByOwnerUid(@Param("ownerUid") UUID ownerUid);

    @Query("SELECT u FROM Url u WHERE u.ownerClientId = :clientId AND u.isActive = true "
            + "AND (u.expiresAt IS NULL OR u.expiresAt > CURRENT_TIMESTAMP) ORDER BY u.createdAt DESC")
    List<Url> findAllActiveAndNonExpiredByOwnerClientId(@Param("clientId") String clientId);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Query("SELECT u FROM Url u WHERE u.isActive = true AND (u.expiresAt IS NULL OR u.expiresAt > CURRENT_TIMESTAMP) ORDER BY u.createdAt DESC")
    List<Url> findAllActiveAndNonExpired();
}
