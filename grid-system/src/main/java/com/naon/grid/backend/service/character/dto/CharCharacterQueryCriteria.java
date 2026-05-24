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
}
