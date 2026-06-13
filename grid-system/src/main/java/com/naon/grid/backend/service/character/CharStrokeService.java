package com.naon.grid.backend.service.character;

public interface CharStrokeService {

    /**
     * 根据汉字查询笔顺JSON
     *
     * @param character 汉字
     * @return 笔顺JSON字符串（hanzi-writer格式），不存在返回null
     */
    String findByCharacter(String character);
}
