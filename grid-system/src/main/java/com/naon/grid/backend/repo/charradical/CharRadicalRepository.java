package com.naon.grid.backend.repo.charradical;

import com.naon.grid.backend.domain.charradical.CharRadical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * 随机取已发布部首（排除指定 ID 列表）。
     *
     * @param excludeIds 要排除的部首ID列表
     * @param limit      返回数量上限
     * @return 随机部首列表
     */
    @Query(value = "SELECT * FROM char_radical WHERE status = 1 " +
        "AND publish_status = 'published' AND id NOT IN ?1 " +
        "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<CharRadical> findRandomPublishedExcluding(List<Long> excludeIds, int limit);
}
