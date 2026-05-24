package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharWordRepository extends JpaRepository<CharWord, Integer> {
    List<CharWord> findByCharId(Integer charId);
}
