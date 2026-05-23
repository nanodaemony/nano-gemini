package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridUserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GridUserAuthRepository extends JpaRepository<GridUserAuth, Long>, JpaSpecificationExecutor<GridUserAuth> {

    Optional<GridUserAuth> findByProviderAndProviderId(String provider, String providerId);

    List<GridUserAuth> findByUserId(Long userId);
}
