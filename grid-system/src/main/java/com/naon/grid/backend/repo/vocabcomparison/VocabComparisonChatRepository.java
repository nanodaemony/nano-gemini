package com.naon.grid.backend.repo.vocabcomparison;

import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabComparisonChatRepository extends JpaRepository<VocabComparisonChat, Long>,
        JpaSpecificationExecutor<VocabComparisonChat> {

    List<VocabComparisonChat> findByGroupIdAndStatus(Long groupId, Integer status);

    List<VocabComparisonChat> findByGroupIdInAndStatus(List<Long> groupIds, Integer status);
}
