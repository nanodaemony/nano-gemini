package com.naon.grid.backend.repo.grammarcomparison;

import com.naon.grid.backend.domain.grammarcomparison.GrammarComparisonChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrammarComparisonChatRepository extends JpaRepository<GrammarComparisonChat, Long>,
        JpaSpecificationExecutor<GrammarComparisonChat> {

    List<GrammarComparisonChat> findByGroupIdAndStatus(Long groupId, Integer status);

    List<GrammarComparisonChat> findByGroupIdInAndStatus(List<Long> groupIds, Integer status);
}
