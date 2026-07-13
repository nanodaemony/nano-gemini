package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonGroupVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonItemVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppSearchResultVO;
import com.naon.grid.modules.app.rest.vo.AppTopicBaseVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;
import com.naon.grid.modules.app.rest.wrapper.AppCharCharacterWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppGrammarPointWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppSearchWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppTopicWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppVocabWordWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app")
@Api(tags = "用户：统一搜索")
public class AppSearchController {

    private final VocabWordService vocabWordService;
    private final CharCharacterService charCharacterService;
    private final GrammarPointService grammarPointService;
    private final VocabComparisonGroupService vocabComparisonGroupService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabOutlineRecordService vocabOutlineRecordService;
    private final TopicService topicService;

    @ApiOperation("统一搜索（词汇/汉字/语法/辨析）")
    @AnonymousGetMapping("/search")
    public ResponseEntity<AppSearchResultVO> search(@RequestParam(required = false) String q) {
        if (q == null || q.trim().isEmpty()) {
            return new ResponseEntity<>(
                    AppSearchWrapper.toResultVO(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList()),
                    HttpStatus.OK);
        }

        String keyword = q.trim();

        List<AppVocabWordBaseVO> vocab = searchVocab(keyword);
        List<AppCharCharacterBaseVO> character = searchCharacter(keyword);
        List<AppGrammarPointBaseVO> grammar = searchGrammar(keyword);
        List<AppComparisonGroupVO> comparison = searchComparison(keyword);
        List<AppTopicBaseVO> topic = searchTopic(keyword);

        return new ResponseEntity<>(
                AppSearchWrapper.toResultVO(vocab, character, grammar, comparison, topic),
                HttpStatus.OK);
    }

    private List<AppVocabWordBaseVO> searchVocab(String keyword) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(keyword);
        criteria.setPublishStatus("published");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
        List<AppVocabWordBaseVO> vos = AppVocabWordWrapper.toBaseVOList(dtos);

        if (vos.isEmpty()) {
            vocabOutlineRecordService.recordIfNeeded(keyword);
        }

        return vos;
    }

    // TODO: Add DB-level LIMIT to searchPublishedByCharacter to avoid loading all rows for common characters
    private List<AppCharCharacterBaseVO> searchCharacter(String keyword) {
        List<CharCharacterDto> dtos = charCharacterService.searchPublishedByCharacter(keyword);
        if (dtos.size() > 20) {
            dtos = dtos.subList(0, 20);
        }
        return AppCharCharacterWrapper.toBaseVOList(dtos);
    }

    private List<AppGrammarPointBaseVO> searchGrammar(String keyword) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        List<GrammarPointDto> dtos = grammarPointService.searchPublished(keyword, pageable).getContent();
        return AppGrammarPointWrapper.toBaseVOList(dtos);
    }

    private List<AppComparisonGroupVO> searchComparison(String keyword) {
        List<AppComparisonGroupVO> result = new ArrayList<>();

        // TODO: Consider interleaving vocab and grammar comparison results (e.g., 10+10) for fairer distribution

        // 词汇辨析
        List<VocabComparisonGroupDto> vocabGroups = vocabComparisonGroupService.searchByWordFuzzy(keyword, 20);
        for (VocabComparisonGroupDto dto : vocabGroups) {
            List<AppComparisonItemVO> items = AppSearchWrapper.toVocabComparisonItemVOList(dto.getItems());
            result.add(AppSearchWrapper.toComparisonGroupVO(dto, "vocab", items));
        }

        // 语法辨析
        List<GrammarComparisonGroupDto> grammarGroups = grammarComparisonGroupService.searchByGrammarNameFuzzy(keyword, 20);
        for (GrammarComparisonGroupDto dto : grammarGroups) {
            List<AppComparisonItemVO> items = AppSearchWrapper.toGrammarComparisonItemVOList(dto.getItems());
            result.add(AppSearchWrapper.toComparisonGroupVO(dto, "grammar", items));
        }

        if (result.size() > 20) {
            return result.subList(0, 20);
        }
        return result;
    }

    private List<AppTopicBaseVO> searchTopic(String keyword) {
        List<TopicDto> dtos = topicService.searchPublished(keyword);
        if (dtos.size() > 20) {
            dtos = dtos.subList(0, 20);
        }
        return AppTopicWrapper.toBaseVOList(dtos);
    }
}
