package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabWord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabWordRepository extends JpaRepository<VocabWord, Integer>, JpaSpecificationExecutor<VocabWord> {

    List<VocabWord> findByWordAndStatus(String word, Integer status);
}
