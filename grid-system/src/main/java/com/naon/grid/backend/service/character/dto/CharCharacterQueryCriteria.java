package com.naon.grid.backend.service.character.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharCharacterQueryCriteria implements Serializable {

    @ApiModelProperty(value = "汉字或拼音模糊查询")
    @Query(blurry = "character,pinyin")
    private String blurry;

    @ApiModelProperty(value = "是否仅搜索汉字字段（true=仅匹配character，false=匹配character和pinyin）")
    private Boolean searchCharacterOnly = false;

    @ApiModelProperty(value = "发布状态")
    @Query
    private String publishStatus;
}
