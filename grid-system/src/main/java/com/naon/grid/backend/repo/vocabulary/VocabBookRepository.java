package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabBookRepository extends JpaRepository<VocabBook, Long> {

    /**
     * 查询所有有效书籍，按排序字段倒序排列
     *
     * @param status 有效状态
     * @return 书籍列表
     */
    List<VocabBook> findByStatusOrderByOrderDesc(Integer status);
}
