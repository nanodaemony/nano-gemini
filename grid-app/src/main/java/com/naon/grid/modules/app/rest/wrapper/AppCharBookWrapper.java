package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.domain.character.CharBook;
import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.modules.app.rest.vo.AppCharBookCharVO;
import com.naon.grid.modules.app.rest.vo.AppCharBookListVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端汉字书包装器
 */
public class AppCharBookWrapper {

    public static List<AppCharBookListVO> toBookVOList(List<CharBook> books) {
        if (books == null) {
            return Collections.emptyList();
        }
        return books.stream().map(AppCharBookWrapper::toBookVO).collect(Collectors.toList());
    }

    public static AppCharBookListVO toBookVO(CharBook book) {
        AppCharBookListVO vo = new AppCharBookListVO();
        vo.setId(book.getId());
        vo.setType(book.getType());
        vo.setName(book.getName());
        vo.setSubName(book.getSubName());
        vo.setCoverImage(book.getCoverImage());
        vo.setDesc(book.getDesc());
        return vo;
    }

    public static List<AppCharBookCharVO> toCharVOList(List<CharCharacter> characters) {
        if (characters == null) {
            return Collections.emptyList();
        }
        return characters.stream().map(AppCharBookWrapper::toCharVO).collect(Collectors.toList());
    }

    public static AppCharBookCharVO toCharVO(CharCharacter charEntity) {
        AppCharBookCharVO vo = new AppCharBookCharVO();
        vo.setId(charEntity.getId());
        vo.setCharacter(charEntity.getCharacter());
        vo.setPinyin(charEntity.getPinyin());
        vo.setHskLevel(charEntity.getLevel());
        return vo;
    }
}
