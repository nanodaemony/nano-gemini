package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.character.CharBookService;
import com.naon.grid.modules.app.rest.vo.AppCharBookCharVO;
import com.naon.grid.modules.app.rest.vo.AppCharBookListVO;
import com.naon.grid.modules.app.rest.wrapper.AppCharBookWrapper;
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
        return new ResponseEntity<>(AppCharBookWrapper.toBookVOList(charBookService.findAvailableBooks()), HttpStatus.OK);
    }

    @ApiOperation("查询汉字书下的汉字列表")
    @AnonymousGetMapping("/{id}/characters")
    public ResponseEntity<List<AppCharBookCharVO>> listCharacters(@PathVariable Long id) {
        return new ResponseEntity<>(AppCharBookWrapper.toCharVOList(
                charBookService.findCharactersByBook(charBookService.findAvailableById(id))), HttpStatus.OK);
    }
}
