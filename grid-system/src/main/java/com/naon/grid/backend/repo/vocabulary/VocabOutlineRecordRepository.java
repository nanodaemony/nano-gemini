package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabOutlineRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VocabOutlineRecordRepository extends JpaRepository<VocabOutlineRecord, Integer>, JpaSpecificationExecutor<VocabOutlineRecord> {

    Optional<VocabOutlineRecord> findByWord(String word);

    @Modifying
    @Query("UPDATE VocabOutlineRecord r SET r.searchCount = r.searchCount + 1, r.updateTime = CURRENT_TIMESTAMP WHERE r.word = :word")
    int incrementSearchCount(@Param("word") String word);
}
