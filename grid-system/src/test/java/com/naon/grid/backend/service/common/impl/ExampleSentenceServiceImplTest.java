package com.naon.grid.backend.service.common.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.enums.SentenceBizTypeEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExampleSentenceServiceImplTest {

    private ExampleSentenceRepository repository;
    private ExampleSentenceServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ExampleSentenceRepository.class);
        service = new ExampleSentenceServiceImpl(repository);
    }

    @Test
    void syncOneCreatesSentenceWhenNoIdIsProvided() {
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setSentence("你好，我叫小明。");
        dto.setPinyin("nǐ hǎo, wǒ jiào xiǎo míng.");
        dto.setAudioId(21L);
        dto.setImageId(34L);
        dto.setOrder(5);

        TextTranslation translation = new TextTranslation();
        translation.setLanguage("en");
        translation.setTranslation("Hello, my name is Xiaoming.");
        dto.setTranslations(Collections.singletonList(translation));

        when(repository.findByBizTypeAndBizIdAndStatus(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Collections.emptyList());
        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, dto);

        ArgumentCaptor<ExampleSentence> captor = ArgumentCaptor.forClass(ExampleSentence.class);
        verify(repository).save(captor.capture());
        ExampleSentence saved = captor.getValue();
        assertEquals(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), saved.getBizType());
        assertEquals(Long.valueOf(9L), saved.getBizId());
        assertEquals("你好，我叫小明。", saved.getSentence());
        assertEquals(Long.valueOf(21L), saved.getAudioId());
        assertEquals(Long.valueOf(34L), saved.getImageId());
        assertEquals(Integer.valueOf(5), saved.getSentenceOrder());
        assertEquals(StatusEnum.ENABLED.getCode(), saved.getStatus());
        assertEquals("Hello, my name is Xiaoming.", JsonUtils.parseTranslationList(saved.getTranslations()).get(0).getTranslation());
    }

    @Test
    void syncOneUpdatesOwnedSentenceAndDisablesOtherActiveRows() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        existing.setBizId(9L);
        existing.setSentence("旧例句");
        existing.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence duplicate = new ExampleSentence();
        duplicate.setId(89L);
        duplicate.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        duplicate.setBizId(9L);
        duplicate.setSentence("重复例句");
        duplicate.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(88L);
        dto.setSentence("新例句");

        when(repository.findById(88L)).thenReturn(Optional.of(existing));
        when(repository.findByBizTypeAndBizIdAndStatus(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Arrays.asList(existing, duplicate));
        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, dto);

        assertEquals("新例句", existing.getSentence());
        assertEquals(StatusEnum.DISABLED.getCode(), duplicate.getStatus());
        verify(repository).save(existing);
        verify(repository).save(duplicate);
    }

    @Test
    void syncOneRejectsSentenceIdOwnedByAnotherBizObject() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        existing.setBizId(10L);
        existing.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(88L);
        dto.setSentence("新例句");

        when(repository.findById(88L)).thenReturn(Optional.of(existing));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, dto));

        assertTrue(exception.getMessage().contains("例句ID不属于当前业务对象"));
        verify(repository, never()).save(any(ExampleSentence.class));
    }

    @Test
    void syncOneDisablesExistingSentencesWhenRequestIsEmpty() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        existing.setBizId(9L);
        existing.setSentence("旧例句");
        existing.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findByBizTypeAndBizIdAndStatus(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Collections.singletonList(existing));

        service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, null);

        assertEquals(StatusEnum.DISABLED.getCode(), existing.getStatus());
        verify(repository).save(existing);
    }

    @Test
    void findByBizIdsReturnsOneSentencePerBizId() {
        ExampleSentence first = new ExampleSentence();
        first.setId(88L);
        first.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        first.setBizId(9L);
        first.setSentence("第一条例句");
        first.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence second = new ExampleSentence();
        second.setId(90L);
        second.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        second.setBizId(10L);
        second.setSentence("第二条例句");
        second.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findByBizTypeAndBizIdInAndStatus(
                eq(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode()), eq(Arrays.asList(9L, 10L)), eq(StatusEnum.ENABLED.getCode())))
                .thenReturn(Arrays.asList(first, second));

        Map<Long, ExampleSentenceDto> result = service.findByBizIds(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), Arrays.asList(9L, 10L));

        assertEquals(2, result.size());
        assertEquals("第一条例句", result.get(9L).getSentence());
        assertEquals("第二条例句", result.get(10L).getSentence());
    }
}
