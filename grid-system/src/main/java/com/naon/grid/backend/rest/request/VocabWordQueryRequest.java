package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class VocabWordQueryRequest implements Serializable {

    @ApiModelProperty(value = "词汇模糊查询")
    private String blurry;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
