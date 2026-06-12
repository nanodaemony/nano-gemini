package com.naon.grid.backend.repo.common;

import com.naon.grid.backend.domain.common.ExampleSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExampleSentenceRepository extends JpaRepository<ExampleSentence, Long> {

    List<ExampleSentence> findByBizTypeAndBizIdAndStatus(String bizType, Long bizId, Integer status);

    List<ExampleSentence> findByBizTypeAndBizIdInAndStatus(String bizType, Collection<Long> bizIds, Integer status);
}