package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharComparisonRepository extends JpaRepository<CharComparison, Integer> {

    List<CharComparison> findByCharId(Integer charId);

    List<CharComparison> findByCharIdAndStatus(Integer charId, Integer status);
}
