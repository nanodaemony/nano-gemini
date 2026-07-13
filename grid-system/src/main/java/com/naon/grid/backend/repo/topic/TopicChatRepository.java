package com.naon.grid.backend.repo.topic;

import com.naon.grid.backend.domain.topic.TopicChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicChatRepository extends JpaRepository<TopicChat, Long> {

    List<TopicChat> findByPatternIdAndStatus(Long patternId, Integer status);

    List<TopicChat> findByPatternIdInAndStatus(List<Long> patternIds, Integer status);
}
