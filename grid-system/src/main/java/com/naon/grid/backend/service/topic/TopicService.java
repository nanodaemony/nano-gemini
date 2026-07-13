package com.naon.grid.backend.service.topic;

import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TopicService {

    PageResult<TopicDto> queryAll(TopicQueryCriteria criteria, Pageable pageable);

    TopicDto findById(Long id);

    TopicDto findPublishedById(Long id);

    Long create(TopicDto resources);

    void update(Long id, TopicDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);

    List<TopicDto> searchPublished(String blurry);
}
