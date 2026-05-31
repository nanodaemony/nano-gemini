package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabOutlineRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VocabOutlineRecordRepository extends JpaRepository<VocabOutlineRecord, Integer>, JpaSpecificationExecutor<VocabOutlineRecord> {

    Optional<VocabOutlineRecord> findByWord(String word);
}
