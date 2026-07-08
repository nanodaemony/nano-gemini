package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppRadicalPracticeVO implements Serializable {

    @ApiModelProperty(value = "部首分组列表（目标部首+2个随机部首）")
    private List<RadicalGroup> radicals;

    @Getter
    @Setter
    public static class RadicalGroup implements Serializable {

        @ApiModelProperty(value = "部首ID")
        private Long radicalId;

        @ApiModelProperty(value = "部首字符")
        private String radical;

        @ApiModelProperty(value = "部首名称")
        private String radicalName;

        @ApiModelProperty(value = "该部首下的汉字列表（最多10个）")
        private List<AppPracticeCharVO> characters;
    }

    public static AppRadicalPracticeVO withRadicals(List<RadicalGroup> radicals) {
        AppRadicalPracticeVO vo = new AppRadicalPracticeVO();
        vo.setRadicals(radicals);
        return vo;
    }
}
