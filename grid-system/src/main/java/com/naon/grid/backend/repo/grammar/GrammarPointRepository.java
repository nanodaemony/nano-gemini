package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GrammarPointRepository extends JpaRepository<GrammarPoint, Long>, JpaSpecificationExecutor<GrammarPoint> {
}
