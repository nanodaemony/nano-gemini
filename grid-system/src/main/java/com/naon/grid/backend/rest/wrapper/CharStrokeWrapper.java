package com.naon.grid.backend.rest.wrapper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.backend.rest.vo.CharStrokeVO;

import java.util.Collections;
import java.util.List;

/**
 * 笔顺包装器
 *
 * @author chenzeng
 * @version 0.0.1
 * @date 2026/6/13 11:04
 */
public class CharStrokeWrapper {

    private CharStrokeWrapper() {
    }

    /**
     * 将笔顺JSON字符串转换为VO
     *
     * @param character  汉字
     * @param strokeJson hanzi-writer格式的笔顺JSON字符串（含strokes/medians/radStrokes）
     * @return CharStrokeVO，strokeJson为null时返回仅含character的空VO
     */
    @SuppressWarnings("unchecked")
    public static CharStrokeVO toStrokeVO(String character, String strokeJson) {
        CharStrokeVO vo = new CharStrokeVO();
        vo.setCharacter(character);
        if (strokeJson == null) {
            return vo;
        }
        JSONObject obj = JSON.parseObject(strokeJson);
        if (obj == null) {
            return vo;
        }

        // strokes: SVG路径字符串列表
        JSONArray strokesArr = obj.getJSONArray("strokes");
        if (strokesArr != null) {
            vo.setStrokes((List<String>) (List<?>) strokesArr.toJavaList(String.class));
        } else {
            vo.setStrokes(Collections.emptyList());
        }

        // medians: 坐标参考线（List<List<List<Integer>>>）
        JSONArray mediansArr = obj.getJSONArray("medians");
        if (mediansArr != null) {
            vo.setMedians((List<List<List<Integer>>>) (List<?>) mediansArr.toJavaList(List.class));
        } else {
            vo.setMedians(Collections.emptyList());
        }

        // radStrokes: 部首笔画索引（可选字段，可能不存在）
        JSONArray radArr = obj.getJSONArray("radStrokes");
        if (radArr != null) {
            vo.setRadStrokes((List<Integer>) (List<?>) radArr.toJavaList(Integer.class));
        }

        return vo;
    }
}
