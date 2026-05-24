package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharDiscrimination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharDiscriminationRepository extends JpaRepository<CharDiscrimination, Integer> {
    List<CharDiscrimination> findByCharId(Integer charId);
}
