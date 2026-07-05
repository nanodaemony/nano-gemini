package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;

import java.util.List;
import java.util.Map;

public interface CollectionService {

    /**
     * 创建默认收藏夹（用户注册时调用）
     */
    BizCollectionFolder createDefaultFolder(Long userId);

    /**
     * 新建自定义收藏夹
     */
    BizCollectionFolder createFolder(Long userId, String name, Long coverImageId);

    /**
     * 查询用户的所有收藏夹列表（按置顶+时间排序）
     */
    List<BizCollectionFolder> listFolders(Long userId);

    /**
     * 查询收藏夹详情（含归属校验）
     */
    BizCollectionFolder getFolder(Long folderId, Long userId);

    /**
     * 修改收藏夹名称
     */
    void updateFolderName(Long folderId, Long userId, String name);

    /**
     * 修改收藏夹封面图
     */
    void updateFolderCover(Long folderId, Long userId, Long coverImageId);

    /**
     * 删除收藏夹（级联软删所有收藏项）
     */
    void deleteFolder(Long folderId, Long userId);

    /**
     * 置顶收藏夹
     */
    void pinFolder(Long folderId, Long userId);

    /**
     * 取消置顶
     */
    void unpinFolder(Long folderId, Long userId);

    /**
     * 添加内容到收藏夹（folderId为null时使用默认收藏夹）
     */
    void addItem(Long userId, Long folderId, String bizType, Long contentId, String contentText);

    /**
     * 取消收藏（软删除）
     */
    void removeItem(Long itemId, Long userId);

    /**
     * 查询收藏夹下的所有有效收藏项（按bizType分组）
     */
    Map<String, List<BizCollectionItem>> getFolderItemsGrouped(Long folderId);

    /**
     * 查询某个内容是否已收藏
     * @return 收藏项，未收藏返回null
     */
    BizCollectionItem checkCollected(Long userId, String bizType, Long contentId);

    /**
     * 获取有效收藏项计数
     */
    long countActiveItems(Long folderId);
}
