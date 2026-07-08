package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.rest.vo.LearningHistoryItemVO;

import java.util.List;

public interface LearningHistoryService {

    /**
     * 添加或更新学习记录（已存在则提序到最新）
     *
     * @param userId    用户ID
     * @param bizType   业务类型
     * @param contentId 内容ID
     */
    void addRecord(Long userId, String bizType, Long contentId);

    /**
     * 查询最近学习记录（最多50条，按学习时间倒序）
     *
     * @param userId 用户ID
     * @return 学习记录列表
     */
    List<LearningHistoryItemVO> getHistory(Long userId);

    /**
     * 删除单条学习记录
     *
     * @param userId    用户ID
     * @param bizType   业务类型
     * @param contentId 内容ID
     */
    void removeRecord(Long userId, String bizType, Long contentId);

    /**
     * 清空所有学习记录
     *
     * @param userId 用户ID
     */
    void clearAll(Long userId);
}
