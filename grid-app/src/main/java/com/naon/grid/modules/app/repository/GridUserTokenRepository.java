package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridUserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridUserTokenRepository extends JpaRepository<GridUserToken, Long>, JpaSpecificationExecutor<GridUserToken> {

    Optional<GridUserToken> findByRefreshToken(String refreshToken);

    Optional<GridUserToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<GridUserToken> findByUserId(Long userId);

    List<GridUserToken> findByExpireTimeBefore(Date now);

    void deleteByUserIdAndDeviceId(Long userId, String deviceId);
}
