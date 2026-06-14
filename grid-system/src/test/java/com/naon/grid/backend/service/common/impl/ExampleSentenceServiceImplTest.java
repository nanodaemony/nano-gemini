package com.naon.grid.backend.service.common.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    void saveCreatesNewSentenceWhenNoId() {
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

        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> {
            ExampleSentence e = invocation.getArgument(0);
            e.setId(99L);
            return e;
        });

        ExampleSentenceDto result = service.save(dto);

        assertNotNull(result);
        assertEquals(99L, result.getId().longValue());

        ArgumentCaptor<ExampleSentence> captor = ArgumentCaptor.forClass(ExampleSentence.class);
        verify(repository).save(captor.capture());
        ExampleSentence saved = captor.getValue();
        assertEquals("你好，我叫小明。", saved.getSentence());
        assertEquals("nǐ hǎo, wǒ jiào xiǎo míng.", saved.getPinyin());
        assertEquals(Long.valueOf(21L), saved.getAudioId());
        assertEquals(Long.valueOf(34L), saved.getImageId());
        assertEquals(Integer.valueOf(5), saved.getSentenceOrder());
        assertEquals(StatusEnum.ENABLED.getCode(), saved.getStatus());
        assertEquals("Hello, my name is Xiaoming.",
                JsonUtils.parseTranslationList(saved.getTranslations()).get(0).getTranslation());
    }

    @Test
    void saveUpdatesExistingSentenceWhenIdProvided() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setSentence("旧例句");
        existing.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(88L);
        dto.setSentence("新例句");

        when(repository.findById(88L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExampleSentenceDto result = service.save(dto);

        assertEquals("新例句", existing.getSentence());
        verify(repository).save(existing);
    }

    @Test
    void saveReturnsNullWhenDtoIsNull() {
        assertNull(service.save(null));
    }

    @Test
    void saveReturnsNullWhenSentenceIsBlank() {
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setSentence("   ");
        assertNull(service.save(dto));
    }

    @Test
    void saveThrowsWhenIdNotFound() {
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(999L);
        dto.setSentence("新例句");

        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.save(dto));
    }

    @Test
    void findByIdReturnsDtoWhenFoundAndEnabled() {
        ExampleSentence entity = new ExampleSentence();
        entity.setId(1L);
        entity.setSentence("例句");
        entity.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        ExampleSentenceDto result = service.findById(1L);
        assertNotNull(result);
        assertEquals("例句", result.getSentence());
    }

    @Test
    void findByIdReturnsNullWhenDisabled() {
        ExampleSentence entity = new ExampleSentence();
        entity.setId(1L);
        entity.setStatus(StatusEnum.DISABLED.getCode());

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertNull(service.findById(1L));
    }

    @Test
    void findByIdReturnsNullWhenNull() {
        assertNull(service.findById(null));
    }

    @Test
    void findByIdsReturnsMapOfEnabledSentences() {
        ExampleSentence first = new ExampleSentence();
        first.setId(1L);
        first.setSentence("第一句");
        first.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence second = new ExampleSentence();
        second.setId(2L);
        second.setSentence("第二句");
        second.setStatus(StatusEnum.DISABLED.getCode());

        when(repository.findAllById(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(first, second));

        Map<Long, ExampleSentenceDto> result = service.findByIds(Arrays.asList(1L, 2L));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(1L));
    }

    @Test
    void disableByIdSetsStatusToDisabled() {
        ExampleSentence entity = new ExampleSentence();
        entity.setId(1L);
        entity.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.disableById(1L);

        assertEquals(StatusEnum.DISABLED.getCode(), entity.getStatus());
        verify(repository).save(entity);
    }
}
