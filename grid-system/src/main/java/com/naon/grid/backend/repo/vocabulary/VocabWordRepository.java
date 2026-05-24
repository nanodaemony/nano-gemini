package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabWordRepository extends JpaRepository<VocabWord, Integer>, JpaSpecificationExecutor<VocabWord> {
}
