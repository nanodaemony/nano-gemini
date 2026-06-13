package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharBookRepository extends JpaRepository<CharBook, Long> {

    /**
     * 查询所有有效书籍，按排序字段倒序排列
     *
     * @param status 有效状态
     * @return 书籍列表
     */
    List<CharBook> findByStatusOrderByOrderDesc(Integer status);
}
