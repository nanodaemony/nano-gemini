package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharStrokeVO implements Serializable {

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "笔顺SVG路径数据")
    private List<String> strokes;

    @ApiModelProperty(value = "笔顺坐标参考线数据（每个元素为笔画的坐标点列表）")
    private List<List<List<Integer>>> medians;

    @ApiModelProperty(value = "部首笔画索引（部分汉字有此数据）")
    private List<Integer> radStrokes;
}
