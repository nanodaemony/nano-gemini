package com.naon.grid.backend.repo.topic;

import com.naon.grid.backend.domain.topic.TopicPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicPatternRepository extends JpaRepository<TopicPattern, Long> {

    List<TopicPattern> findByTopicIdAndStatus(Long topicId, Integer status);

    List<TopicPattern> findByTopicIdInAndStatus(List<Long> topicIds, Integer status);
}
