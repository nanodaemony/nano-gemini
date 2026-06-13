package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharStroke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CharStrokeRepository extends JpaRepository<CharStroke, Long> {

    Optional<CharStroke> findByCharacterAndStatus(String character, Integer status);
}
