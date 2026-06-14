package com.naon.grid.backend.repo.common;

import com.naon.grid.backend.domain.common.ExampleSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExampleSentenceRepository extends JpaRepository<ExampleSentence, Long> {

    /**
     * 查询某结构的所有启用例句（1:N vocab_structure 场景）
     */
    List<ExampleSentence> findByStructureIdAndStatus(Long structureId, Integer status);

    /**
     * 批量查询多个结构的所有启用例句
     */
    List<ExampleSentence> findByStructureIdInAndStatus(Collection<Long> structureIds, Integer status);
}