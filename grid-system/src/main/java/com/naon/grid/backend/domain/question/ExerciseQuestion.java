package com.naon.grid.backend.domain.question;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "exercise_question")
public class ExerciseQuestion extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "question_type", length = 32, nullable = false)
    private String questionType;

    @Column(name = "stem", length = 512)
    private String stem;

    @Column(name = "content", length = 4096)
    private String content;

    @Column(name = "options", length = 2048)
    private String options;

    @Column(name = "answer", length = 512)
    private String answer;

    @Column(name = "explanation", length = 1024)
    private String explanation;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "sort")
    private Integer sort = 0;

    @Column(name = "draft_content", columnDefinition = "text")
    private String draftContent;

    @Column(name = "edit_status", length = 20)
    private String editStatus = EditStatusEnum.DRAFT.getCode();

    @Column(name = "publish_status", length = 20)
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
