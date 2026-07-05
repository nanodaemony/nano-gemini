package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "biz_collection_folder")
public class BizCollectionFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 32, nullable = false)
    private String name;

    @Column(name = "cover_image_id")
    private Long coverImageId;

    @Column(name = "is_default", nullable = false)
    private Integer isDefault = 0;

    @Column(name = "is_pinned", nullable = false)
    private Integer isPinned = 0;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
