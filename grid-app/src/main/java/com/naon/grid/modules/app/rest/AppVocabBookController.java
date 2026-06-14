package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.domain.vocabulary.VocabBook;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.service.vocabulary.VocabBookService;
import com.naon.grid.modules.app.rest.vo.AppVocabBookListVO;
import com.naon.grid.modules.app.rest.vo.AppVocabBookWordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端词汇书接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/vocab-book")
@Api(tags = "用户：词汇书接口")
public class AppVocabBookController {

    private final VocabBookService vocabBookService;

    @ApiOperation("查询词汇书列表")
    @AnonymousGetMapping
    public ResponseEntity<List<AppVocabBookListVO>> listBooks() {
        List<VocabBook> books = vocabBookService.findAvailableBooks();
        List<AppVocabBookListVO> vos = toBookVOList(books);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("查询词汇书下的词汇列表")
    @AnonymousGetMapping("/{id}/words")
    public ResponseEntity<List<AppVocabBookWordVO>> listWords(@PathVariable Long id) {
        VocabBook book = vocabBookService.findAvailableById(id);
        List<VocabWord> words = vocabBookService.findWordsByBook(book);
        List<AppVocabBookWordVO> vos = toWordVOList(words);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    private List<AppVocabBookListVO> toBookVOList(List<VocabBook> books) {
        if (books == null) {
            return Collections.emptyList();
        }
        return books.stream().map(this::toBookVO).collect(Collectors.toList());
    }

    private AppVocabBookListVO toBookVO(VocabBook book) {
        AppVocabBookListVO vo = new AppVocabBookListVO();
        vo.setId(book.getId());
        vo.setType(book.getType());
        vo.setName(book.getName());
        vo.setSubName(book.getSubName());
        vo.setCoverImage(book.getCoverImage());
        vo.setDesc(book.getDesc());
        return vo;
    }

    private List<AppVocabBookWordVO> toWordVOList(List<VocabWord> words) {
        if (words == null) {
            return Collections.emptyList();
        }
        return words.stream().map(this::toWordVO).collect(Collectors.toList());
    }

    private AppVocabBookWordVO toWordVO(VocabWord word) {
        AppVocabBookWordVO vo = new AppVocabBookWordVO();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setPinyin(word.getPinyin());
        vo.setHskLevel(word.getHskLevel());
        return vo;
    }
}
