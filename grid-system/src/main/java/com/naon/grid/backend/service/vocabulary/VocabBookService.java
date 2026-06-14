package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabBook;
import com.naon.grid.backend.domain.vocabulary.VocabWord;

import java.util.List;

public interface VocabBookService {

    /**
     * 查询所有有效的词汇书（status=1），按 order 倒序排列
     */
    List<VocabBook> findAvailableBooks();

    /**
     * 根据 ID 查询词汇书，不存在或已下架抛 EntityNotFoundException
     */
    VocabBook findAvailableById(Long id);

    /**
     * 根据书籍配置查询对应的词汇列表
     * - 如果 book.hskLevel 不为空：按 HSK 等级从 vocab_word 查询已发布的词汇
     * - 如果 book.wordIds 不为空：按 ID 列表查询，并保持 word_ids 中的顺序
     * - 两者均为空：返回空列表
     */
    List<VocabWord> findWordsByBook(VocabBook book);
}
