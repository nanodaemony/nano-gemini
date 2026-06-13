package com.naon.grid.backend.service.character;

import com.naon.grid.backend.domain.character.CharBook;
import com.naon.grid.backend.domain.character.CharCharacter;

import java.util.List;

public interface CharBookService {

    /**
     * 查询所有有效的汉字书（status=1），按 order 倒序排列
     */
    List<CharBook> findAvailableBooks();

    /**
     * 根据 ID 查询汉字书，不存在或已下架抛 EntityNotFoundException
     */
    CharBook findAvailableById(Long id);

    /**
     * 根据书籍配置查询对应的汉字列表
     * - 如果 book.hskLevel 不为空：按 HSK 等级从 char_character 查询已发布的汉字
     * - 如果 book.wordIds 不为空：按 ID 列表查询，并保持 word_ids 中的顺序
     * - 两者均为空：返回空列表
     */
    List<CharCharacter> findCharactersByBook(CharBook book);
}
