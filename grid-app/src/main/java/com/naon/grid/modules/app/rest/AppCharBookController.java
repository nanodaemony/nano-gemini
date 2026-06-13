package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.domain.character.CharBook;
import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.service.character.CharBookService;
import com.naon.grid.modules.app.rest.vo.AppCharBookCharVO;
import com.naon.grid.modules.app.rest.vo.AppCharBookListVO;
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
 * 用户端汉字书接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/char-book")
@Api(tags = "用户：汉字书接口")
public class AppCharBookController {

    private final CharBookService charBookService;

    @ApiOperation("查询汉字书列表")
    @AnonymousGetMapping
    public ResponseEntity<List<AppCharBookListVO>> listBooks() {
        List<CharBook> books = charBookService.findAvailableBooks();
        List<AppCharBookListVO> vos = toBookVOList(books);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("查询汉字书下的汉字列表")
    @AnonymousGetMapping("/{id}/characters")
    public ResponseEntity<List<AppCharBookCharVO>> listCharacters(@PathVariable Long id) {
        CharBook book = charBookService.findAvailableById(id);
        List<CharCharacter> characters = charBookService.findCharactersByBook(book);
        List<AppCharBookCharVO> vos = toCharVOList(characters);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    private List<AppCharBookListVO> toBookVOList(List<CharBook> books) {
        if (books == null) {
            return Collections.emptyList();
        }
        return books.stream().map(this::toBookVO).collect(Collectors.toList());
    }

    private AppCharBookListVO toBookVO(CharBook book) {
        AppCharBookListVO vo = new AppCharBookListVO();
        vo.setId(book.getId());
        vo.setType(book.getType());
        vo.setName(book.getName());
        vo.setSubName(book.getSubName());
        vo.setCoverImage(book.getCoverImage());
        vo.setDesc(book.getDesc());
        return vo;
    }

    private List<AppCharBookCharVO> toCharVOList(List<CharCharacter> characters) {
        if (characters == null) {
            return Collections.emptyList();
        }
        return characters.stream().map(this::toCharVO).collect(Collectors.toList());
    }

    private AppCharBookCharVO toCharVO(CharCharacter charEntity) {
        AppCharBookCharVO vo = new AppCharBookCharVO();
        vo.setId(charEntity.getId());
        vo.setCharacter(charEntity.getCharacter());
        vo.setPinyin(charEntity.getPinyin());
        vo.setHskLevel(charEntity.getLevel());
        return vo;
    }
}
