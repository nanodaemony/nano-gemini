package com.naon.grid.backend.repo.culture;

import com.naon.grid.backend.domain.culture.Culture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CultureRepository extends JpaRepository<Culture, Long>, JpaSpecificationExecutor<Culture> {
}
