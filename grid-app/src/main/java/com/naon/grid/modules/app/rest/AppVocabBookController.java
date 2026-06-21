package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.vocabulary.VocabBookService;
import com.naon.grid.modules.app.rest.vo.AppVocabBookListVO;
import com.naon.grid.modules.app.rest.vo.AppVocabBookWordVO;
import com.naon.grid.modules.app.rest.wrapper.AppVocabBookWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        return new ResponseEntity<>(AppVocabBookWrapper.toBookVOList(vocabBookService.findAvailableBooks()), HttpStatus.OK);
    }

    @ApiOperation("查询词汇书下的词汇列表")
    @AnonymousGetMapping("/{id}/words")
    public ResponseEntity<List<AppVocabBookWordVO>> listWords(@PathVariable Long id) {
        return new ResponseEntity<>(AppVocabBookWrapper.toWordVOList(
                vocabBookService.findWordsByBook(vocabBookService.findAvailableById(id))), HttpStatus.OK);
    }
}
