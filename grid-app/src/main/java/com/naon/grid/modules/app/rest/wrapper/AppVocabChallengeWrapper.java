package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class AppVocabChallengeWrapper {

    /**
     * 将挑战题目数据转换为 AppExerciseQuestionDetailVO 列表
     */
    public static List<AppExerciseQuestionDetailVO> toChallengeVOList(
            List<ChallengeItem> items,
            Map<Long, AliOssStorageDto> imageMap) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<AppExerciseQuestionDetailVO> vos = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            vos.add(toChallengeVO(items.get(i), i + 1, imageMap));
        }
        return vos;
    }

    private static AppExerciseQuestionDetailVO toChallengeVO(
            ChallengeItem item, int sort, Map<Long, AliOssStorageDto> imageMap) {

        VocabWordDto correct = item.getCorrectWord();

        AppExerciseQuestionDetailVO vo = new AppExerciseQuestionDetailVO();
        vo.setId((long) -correct.getId());
        vo.setQuestionType("vocab_challenge");
        vo.setStem("请根据提示选出对应的词语");
        vo.setContent(buildContent(item.getDefImageId(), imageMap));
        vo.setOptions(buildOptions(correct, item.getDistractors()));
        vo.setAnswer(Collections.singletonList(
                findCorrectOption(vo.getOptions(), correct.getWord())));
        vo.setExplanation(null);
        vo.setAudio(null);
        vo.setAudioText(null);
        vo.setSort(sort);
        vo.setChildren(null);
        return vo;
    }

    private static AppExerciseQuestionDetailVO.QuestionContentVO buildContent(
            Long defImageId, Map<Long, AliOssStorageDto> imageMap) {
        AppExerciseQuestionDetailVO.QuestionContentVO content =
                new AppExerciseQuestionDetailVO.QuestionContentVO();
        content.setContentText(null);
        if (defImageId != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(defImageId);
            if (imgDto != null) {
                AppExerciseQuestionDetailVO.ImageVO imageVO =
                        new AppExerciseQuestionDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                content.setImage(imageVO);
            } else {
                log.error("挑战题目图片资源未找到, imageId={}", defImageId);
            }
        }
        return content;
    }

    private static List<AppExerciseQuestionDetailVO.QuestionOptionVO> buildOptions(
            VocabWordDto correctWord, List<VocabWordDto> distractors) {
        List<VocabWordDto> all = new ArrayList<>();
        all.add(correctWord);
        if (distractors != null) {
            all.addAll(distractors);
        }
        Collections.shuffle(all);
        String[] letters = {"A", "B", "C", "D"};
        return IntStream.range(0, Math.min(all.size(), 4))
                .mapToObj(i -> {
                    AppExerciseQuestionDetailVO.QuestionOptionVO opt =
                            new AppExerciseQuestionDetailVO.QuestionOptionVO();
                    opt.setOption(letters[i]);
                    opt.setOptionText(all.get(i).getWord());
                    opt.setImage(null);
                    return opt;
                })
                .collect(Collectors.toList());
    }

    private static String findCorrectOption(
            List<AppExerciseQuestionDetailVO.QuestionOptionVO> options,
            String correctWord) {
        for (AppExerciseQuestionDetailVO.QuestionOptionVO opt : options) {
            if (correctWord.equals(opt.getOptionText())) {
                return opt.getOption();
            }
        }
        return "A";
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ChallengeItem {
        private VocabWordDto correctWord;
        private Long defImageId;
        private List<VocabWordDto> distractors;
    }
}
