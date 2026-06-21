package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.domain.vocabulary.VocabBook;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.modules.app.rest.vo.AppVocabBookListVO;
import com.naon.grid.modules.app.rest.vo.AppVocabBookWordVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端词汇书包装器
 */
public class AppVocabBookWrapper {

    public static List<AppVocabBookListVO> toBookVOList(List<VocabBook> books) {
        if (books == null) {
            return Collections.emptyList();
        }
        return books.stream().map(AppVocabBookWrapper::toBookVO).collect(Collectors.toList());
    }

    public static AppVocabBookListVO toBookVO(VocabBook book) {
        AppVocabBookListVO vo = new AppVocabBookListVO();
        vo.setId(book.getId());
        vo.setType(book.getType());
        vo.setName(book.getName());
        vo.setSubName(book.getSubName());
        vo.setCoverImage(book.getCoverImage());
        vo.setDesc(book.getDesc());
        return vo;
    }

    public static List<AppVocabBookWordVO> toWordVOList(List<VocabWord> words) {
        if (words == null) {
            return Collections.emptyList();
        }
        return words.stream().map(AppVocabBookWrapper::toWordVO).collect(Collectors.toList());
    }

    public static AppVocabBookWordVO toWordVO(VocabWord word) {
        AppVocabBookWordVO vo = new AppVocabBookWordVO();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setPinyin(word.getPinyin());
        vo.setHskLevel(word.getHskLevel());
        return vo;
    }
}
