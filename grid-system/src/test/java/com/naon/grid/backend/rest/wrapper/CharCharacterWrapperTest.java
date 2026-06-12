package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CharCharacterCreateRequest;
import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CharCharacterVO;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CharCharacterWrapperTest {

    @Test
    void toDtoMapsNewCharacterFieldsAndSingleWordSentence() {
        CharCharacterCreateRequest request = new CharCharacterCreateRequest();
        request.setCharacter("你");
        request.setHskLevel("1");
        request.setPinyin("nǐ");
        request.setAudioId(12L);
        request.setTraditional("你");
        request.setRadicalId(3L);
        request.setRadical("亻");
        request.setComponentCombination("亻 + 尔");
        request.setCharDesc("第二人称代词");
        request.setStroke("stroke-json");

        TextTranslationRequest descTranslation = new TextTranslationRequest();
        descTranslation.setLanguage("en");
        descTranslation.setTranslation("you");
        request.setCharDescTranslations(Collections.singletonList(descTranslation));

        CharCharacterCreateRequest.CharWordRequest wordRequest = new CharCharacterCreateRequest.CharWordRequest();
        wordRequest.setId(9);
        wordRequest.setWordItem("你好");
        wordRequest.setHskLevel("1");
        wordRequest.setPinyin("nǐ hǎo");
        wordRequest.setPartOfSpeech("interj.");
        wordRequest.setWordItemTranslations(Collections.singletonList(descTranslation));
        wordRequest.setOrder(7);

        ExampleSentenceRequest sentenceRequest = new ExampleSentenceRequest();
        sentenceRequest.setId(88L);
        sentenceRequest.setSentence("你好，我叫小明。");
        sentenceRequest.setPinyin("nǐ hǎo, wǒ jiào xiǎo míng.");
        sentenceRequest.setAudioId(21L);
        sentenceRequest.setImageId(34L);
        sentenceRequest.setOrder(5);
        sentenceRequest.setTranslations(Collections.singletonList(descTranslation));
        wordRequest.setSentenceContent(sentenceRequest);
        request.setWords(Collections.singletonList(wordRequest));

        CharCharacterDto dto = CharCharacterWrapper.toDto(request);

        assertEquals("你", dto.getCharacter());
        assertEquals("1", dto.getLevel());
        assertEquals("nǐ", dto.getPinyin());
        assertEquals(Long.valueOf(12L), dto.getAudioId());
        assertEquals(Long.valueOf(3L), dto.getRadicalId());
        assertEquals("亻 + 尔", dto.getComponentCombination());
        assertEquals("第二人称代词", dto.getCharDesc());
        assertEquals("stroke-json", dto.getStroke());
        assertEquals(1, dto.getDescTranslations().size());

        assertEquals(1, dto.getWords().size());
        CharWordDto wordDto = dto.getWords().get(0);
        assertEquals(Integer.valueOf(9), wordDto.getId());
        assertEquals("你好", wordDto.getWordItem());
        assertEquals("1", wordDto.getLevel());
        assertEquals(Integer.valueOf(7), wordDto.getWordOrder());
        assertNotNull(wordDto.getWordItemSentence());
        assertEquals(Long.valueOf(88L), wordDto.getWordItemSentence().getId());
        assertEquals("你好，我叫小明。", wordDto.getWordItemSentence().getSentence());
    }

    @Test
    void toVoMapsSingleWordSentence() {
        CharCharacterDto dto = new CharCharacterDto();
        dto.setId(1);
        dto.setCharacter("你");
        dto.setLevel("1");
        dto.setRadicalId(3L);
        dto.setComponentCombination("亻 + 尔");

        CharWordDto wordDto = new CharWordDto();
        wordDto.setId(9);
        wordDto.setCharId(1);
        wordDto.setWordItem("你好");
        wordDto.setLevel("1");
        wordDto.setWordOrder(7);

        ExampleSentenceDto sentenceDto = new ExampleSentenceDto();
        sentenceDto.setId(88L);
        sentenceDto.setSentence("你好，我叫小明。");
        sentenceDto.setPinyin("nǐ hǎo, wǒ jiào xiǎo míng.");
        sentenceDto.setAudioId(21L);
        sentenceDto.setImageId(34L);
        sentenceDto.setOrder(5);
        wordDto.setWordItemSentence(sentenceDto);
        dto.setWords(Collections.singletonList(wordDto));

        CharCharacterVO vo = CharCharacterWrapper.toVO(dto);

        assertEquals(Long.valueOf(3L), vo.getRadicalId());
        assertEquals("亻 + 尔", vo.getComponentCombination());
        assertEquals(1, vo.getWords().size());
        assertNotNull(vo.getWords().get(0).getWordItemSentence());
        assertEquals(Long.valueOf(88L), vo.getWords().get(0).getWordItemSentence().getId());
        assertEquals("你好，我叫小明。", vo.getWords().get(0).getWordItemSentence().getSentence());
    }
}
