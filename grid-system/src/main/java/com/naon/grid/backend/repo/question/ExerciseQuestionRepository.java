package com.naon.grid.backend.repo.question;

import com.naon.grid.backend.domain.question.ExerciseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseQuestionRepository extends JpaRepository<ExerciseQuestion, Long>, JpaSpecificationExecutor<ExerciseQuestion> {

    List<ExerciseQuestion> findByParentIdAndStatus(Long parentId, Integer status);

    long countByParentIdAndStatus(Long parentId, Integer status);
}
