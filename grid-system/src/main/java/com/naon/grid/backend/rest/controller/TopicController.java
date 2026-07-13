package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.TopicCreateRequest;
import com.naon.grid.backend.rest.request.TopicQueryRequest;
import com.naon.grid.backend.rest.vo.TopicBaseVO;
import com.naon.grid.backend.rest.vo.TopicCreateVO;
import com.naon.grid.backend.rest.vo.TopicVO;
import com.naon.grid.backend.rest.wrapper.TopicWrapper;
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：话题管理")
@RequestMapping("/api/topic")
public class TopicController {

    private final TopicService topicService;
    private final AiContentMarkerService aiContentMarkerService;

    @Log("新增话题")
    @ApiOperation("新增话题")
    @AnonymousPostMapping
    public ResponseEntity<TopicCreateVO> create(@Valid @RequestBody TopicCreateRequest request) {
        TopicCreateVO vo = new TopicCreateVO();
        vo.setId(topicService.create(TopicWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新话题")
    @ApiOperation("更新话题")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody TopicCreateRequest request) {
        topicService.update(id, TopicWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("审核话题")
    @ApiOperation("话题草稿通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        topicService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布话题")
    @ApiOperation("发布话题（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        topicService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询话题详情")
    @ApiOperation("根据ID查询话题详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<TopicVO> findById(@PathVariable Long id) {
        TopicDto dto = topicService.findById(id);
        List<String> entityKeys = collectTopicEntityKeys(dto);
        Map<String, MarkerFields> aiMarkers = aiContentMarkerService.batchQuery(entityKeys);
        return new ResponseEntity<>(TopicWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }

    private List<String> collectTopicEntityKeys(TopicDto dto) {
        List<String> keys = new ArrayList<>();
        keys.addAll(AiContentMarkerHelper.collectOne("topic", dto.getId()));
        if (dto.getPatterns() != null) {
            for (TopicPatternDto pattern : dto.getPatterns()) {
                keys.addAll(AiContentMarkerHelper.collectOne("topic_pattern", pattern.getId()));
                if (pattern.getChats() != null) {
                    for (TopicChatDto chat : pattern.getChats()) {
                        keys.addAll(AiContentMarkerHelper.collectOne("topic_chat", chat.getId()));
                    }
                }
            }
        }
        return keys;
    }

    @Log("查询话题列表")
    @ApiOperation("分页查询话题列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<TopicBaseVO>> queryAll(TopicQueryRequest request, Pageable pageable) {
        PageResult<TopicDto> pageResult = topicService.queryAll(TopicWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(TopicWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除话题")
    @ApiOperation("删除话题")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        topicService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线话题")
    @ApiOperation("下线话题")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        topicService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
