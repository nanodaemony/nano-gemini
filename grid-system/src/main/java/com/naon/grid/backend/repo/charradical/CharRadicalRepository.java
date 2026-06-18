package com.naon.grid.backend.repo.charradical;

import com.naon.grid.backend.domain.charradical.CharRadical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharRadicalRepository extends JpaRepository<CharRadical, Long>,
        JpaSpecificationExecutor<CharRadical> {

    /**
     * 查询所有已发布的部首（按ID升序）
     *
     * @param status        有效状态
     * @param publishStatus 发布状态
     * @return 匹配的部首列表
     */
    List<CharRadical> findByStatusAndPublishStatusOrderByIdAsc(Integer status, String publishStatus);
}
