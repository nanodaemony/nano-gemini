package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CharCharacterRepository extends JpaRepository<CharCharacter, Integer>, JpaSpecificationExecutor<CharCharacter> {
}
