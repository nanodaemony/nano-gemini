package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonChatDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO.AppChatVO;
import com.naon.grid.modules.app.rest.wrapper.AppVocabComparisonWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/vocab/comparison")
@Api(tags = "用户：词汇辨析")
public class AppVocabComparisonController {

    private final VocabComparisonGroupService vocabComparisonGroupService;
    private final AudioResourceService audioResourceService;

    @ApiOperation("根据词汇查询辨析组列表")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppVocabComparisonGroupVO>> search(@RequestParam String word) {
        List<VocabComparisonGroupDto> dtos = vocabComparisonGroupService.searchByWord(word);
        return new ResponseEntity<>(AppVocabComparisonWrapper.toAppVOList(dtos), HttpStatus.OK);
    }

    @ApiOperation("根据辨析组ID查询详情")
    @AnonymousGetMapping("/{groupId}")
    public ResponseEntity<AppVocabComparisonGroupVO> getDetail(@PathVariable Long groupId) {
        VocabComparisonGroupDto dto = vocabComparisonGroupService.findById(groupId);

        // Only return published groups to end users
        if (!"published".equals(dto.getPublishStatus())) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        AppVocabComparisonGroupVO vo = AppVocabComparisonWrapper.toAppVO(dto);

        // Batch load audio resources for chats
        if (dto.getChats() != null) {
            List<Long> audioIds = dto.getChats().stream()
                    .map(VocabComparisonChatDto::getAudioId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            if (!audioIds.isEmpty()) {
                Map<Long, AudioResourceDto> audioMap = audioResourceService.findByIds(audioIds).stream()
                        .collect(Collectors.toMap(AudioResourceDto::getId, a -> a));
                List<AppChatVO> chatVos = vo.getChats();
                List<VocabComparisonChatDto> chatDtos = dto.getChats();
                for (int i = 0; i < chatDtos.size() && i < chatVos.size(); i++) {
                    Long audioId = chatDtos.get(i).getAudioId();
                    if (audioId != null && audioMap.containsKey(audioId)) {
                        chatVos.get(i).setAudioUrl(audioMap.get(audioId).getFileUrl());
                    }
                }
            }
        }

        return new ResponseEntity<>(vo, HttpStatus.OK);
    }
}
