package com.naon.grid.backend.repo.topic;

import com.naon.grid.backend.domain.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    List<Topic> findByNameContainingAndStatus(String name, Integer status);

    List<Topic> findByNameContainingAndStatusAndPublishStatus(String name, Integer status, String publishStatus);
}
