package com.naon.grid.backend.service.game.impl;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.domain.character.CharComparison;
import com.naon.grid.backend.domain.character.CharWord;
import com.naon.grid.backend.domain.charradical.CharRadical;
import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.repo.character.CharCharacterRepository;
import com.naon.grid.backend.repo.character.CharComparisonRepository;
import com.naon.grid.backend.repo.character.CharWordRepository;
import com.naon.grid.backend.repo.charradical.CharRadicalRepository;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.repo.vocabulary.VocabWordRepository;
import com.naon.grid.backend.service.game.GameCharacterService;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameExplanationDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameOptionDTO;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 汉字大挑战 — 出题服务实现。
 * <p>
 * 每个方法内部处理数据不足的降级逻辑（返回实际可用数量，不抛异常）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameCharacterServiceImpl implements GameCharacterService {

    private static final int QUESTION_COUNT = 10;
    private static final int DISTRACTOR_COUNT = 3;
    private static final List<String> OPTION_KEYS = Arrays.asList("A", "B", "C", "D");

    private final CharCharacterRepository charCharacterRepository;
    private final CharComparisonRepository charComparisonRepository;
    private final CharWordRepository charWordRepository;
    private final CharRadicalRepository charRadicalRepository;
    private final VocabWordRepository vocabWordRepository;
    private final ExampleSentenceRepository exampleSentenceRepository;

    // ==================== 部首识记 ====================

    @Override
    @Transactional(readOnly = true)
    public List<GameQuestionDTO> generateRadicalQuestions(List<String> hskLevels) {
        List<CharCharacter> chars = charCharacterRepository
                .findRandomPublishedByHskLevels(hskLevels, QUESTION_COUNT);
        if (chars.isEmpty()) {
            log.warn("部首识记: 等级 {} 无可用汉字", hskLevels);
            return Collections.emptyList();
        }

        List<GameQuestionDTO> questions = new ArrayList<>();
        for (int i = 0; i < chars.size(); i++) {
            CharCharacter ch = chars.get(i);
            GameQuestionDTO q = buildRadicalQuestion(ch, i + 1);
            questions.add(q);
        }
        return questions;
    }

    private GameQuestionDTO buildRadicalQuestion(CharCharacter ch, int index) {
        GameQuestionDTO q = new GameQuestionDTO();
        q.setGameType("radical");
        q.setQuestionIndex(index);
        q.setStem(ch.getCharacter());
        q.setCharacter(ch.getCharacter());
        q.setPinyin(ch.getPinyin());

        // 正确答案: 该字所属部首
        String correctRadical = ch.getRadical();
        Long correctRadicalId = ch.getRadicalId();

        // 干扰项: 随机取其他部首
        List<Long> excludeIds = new ArrayList<>();
        if (correctRadicalId != null) {
            excludeIds.add(correctRadicalId);
        }
        List<CharRadical> distractorRadicals = charRadicalRepository
                .findRandomPublishedExcluding(excludeIds, DISTRACTOR_COUNT);

        // 构建选项列表
        List<GameOptionDTO> options = new ArrayList<>();

        GameOptionDTO correctOption = new GameOptionDTO();
        correctOption.setText(correctRadical);
        correctOption.setIsCorrect(true);
        options.add(correctOption);

        for (CharRadical dr : distractorRadicals) {
            GameOptionDTO opt = new GameOptionDTO();
            opt.setText(dr.getRadical());
            opt.setIsCorrect(false);
            options.add(opt);
        }

        // 打乱并分配 key
        Collections.shuffle(options);
        assignKeys(options, q);

        // 解析
        GameExplanationDTO exp = new GameExplanationDTO();
        exp.setRadical(correctRadical);
        if (correctRadicalId != null) {
            List<CharRadical> rads = charRadicalRepository.findAllById(Collections.singletonList(correctRadicalId));
            if (!rads.isEmpty()) {
                CharRadical rad = rads.get(0);
                exp.setRadicalName(rad.getRadicalName());
                exp.setRadicalMeaning(parseTranslations(rad.getEvolutionDescTranslations()));
            }
        }
        q.setExplanation(exp);

        return q;
    }

    // ==================== 形近字辨析 ====================

    @Override
    @Transactional(readOnly = true)
    public List<GameQuestionDTO> generateComparisonQuestions() {
        List<CharComparison> comparisons = charComparisonRepository.findRandomEnabled(QUESTION_COUNT);
        if (comparisons.isEmpty()) {
            log.warn("形近字辨析: 无可用辨析数据");
            return Collections.emptyList();
        }

        // 批量预加载所有涉及的 char_character
        List<Integer> charIds = comparisons.stream()
                .map(CharComparison::getCharId)
                .distinct()
                .collect(Collectors.toList());
        List<CharCharacter> chars = charCharacterRepository.findByIdIn(charIds);

        List<GameQuestionDTO> questions = new ArrayList<>();
        Set<Integer> usedComparisonIds = new HashSet<>();
        for (int i = 0; i < comparisons.size(); i++) {
            CharComparison comp = comparisons.get(i);
            if (!usedComparisonIds.add(comp.getId())) {
                continue; // 去重
            }
            CharCharacter target = findCharById(chars, comp.getCharId());
            if (target == null) {
                log.warn("形近字辨析: comparison id={} 关联的 charId={} 不存在", comp.getId(), comp.getCharId());
                continue;
            }
            GameQuestionDTO q = buildComparisonQuestion(comp, target, i + 1, chars);
            questions.add(q);
        }
        return questions;
    }

    private GameQuestionDTO buildComparisonQuestion(CharComparison comp, CharCharacter target,
                                                     int index, List<CharCharacter> allChars) {
        GameQuestionDTO q = new GameQuestionDTO();
        q.setGameType("comparison");
        q.setQuestionIndex(index);
        q.setCharacter(target.getCharacter());
        q.setPinyin(target.getPinyin());

        // 题干: 优先使用例句，将目标字替换为 ____
        String stem = buildComparisonStem(comp.getCharId(), target.getCharacter());
        q.setStem(stem);

        // 正确答案
        String correctChar = target.getCharacter();

        // 干扰项: 同 charId 下的其他 comparison_char，或同 radical 下的字
        List<GameOptionDTO> options = new ArrayList<>();

        GameOptionDTO correctOption = new GameOptionDTO();
        correctOption.setText(correctChar);
        correctOption.setIsCorrect(true);
        options.add(correctOption);

        List<String> distractors = findComparisonDistractors(comp, target, allChars, DISTRACTOR_COUNT);
        for (String d : distractors) {
            GameOptionDTO opt = new GameOptionDTO();
            opt.setText(d);
            opt.setIsCorrect(false);
            options.add(opt);
        }

        Collections.shuffle(options);
        assignKeys(options, q);

        // 解析
        GameExplanationDTO exp = new GameExplanationDTO();
        exp.setComparisonChar(comp.getComparisonChar());
        exp.setComparisonPinyin(comp.getComparisonPinyin());
        exp.setComparisonDesc(parseTranslations(comp.getComparisonDescTranslations()));
        q.setExplanation(exp);

        return q;
    }

    private String buildComparisonStem(Integer charId, String targetChar) {
        List<CharWord> words = charWordRepository.findByCharIdAndStatus(charId, 1);
        for (CharWord w : words) {
            if (w.getSentenceId() != null) {
                try {
                    ExampleSentence es = exampleSentenceRepository.findById(w.getSentenceId()).orElse(null);
                    if (es != null && es.getSentence() != null && es.getSentence().contains(targetChar)) {
                        return es.getSentence().replace(targetChar, "____");
                    }
                } catch (Exception e) {
                    // 忽略例句加载失败，继续尝试下一个
                }
            }
        }
        // 无合适例句时: 用释义提示作为题干
        return "____ (" + targetChar + ")";
    }

    private List<String> findComparisonDistractors(CharComparison comp, CharCharacter target,
                                                    List<CharCharacter> allChars, int count) {
        List<String> result = new ArrayList<>();
        // 优先: 同一 char_id 下的其他 comparison_char
        List<CharComparison> siblingComparisons = charComparisonRepository
                .findByCharIdAndStatus(comp.getCharId(), 1);
        for (CharComparison sc : siblingComparisons) {
            if (result.size() >= count) break;
            if (!sc.getComparisonChar().equals(target.getCharacter())
                    && !result.contains(sc.getComparisonChar())) {
                result.add(sc.getComparisonChar());
            }
        }
        // 不足时: 同 radical 下的其他字
        if (result.size() < count && target.getRadical() != null) {
            for (CharCharacter ch : allChars) {
                if (result.size() >= count) break;
                if (!ch.getCharacter().equals(target.getCharacter())
                        && target.getRadical().equals(ch.getRadical())
                        && !result.contains(ch.getCharacter())) {
                    result.add(ch.getCharacter());
                }
            }
        }
        // 再不足: 同等级随机字
        if (result.size() < count && target.getLevel() != null) {
            List<CharCharacter> randomPool = charCharacterRepository
                    .findByLevelAndStatusAndPublishStatus(target.getLevel(), 1, "published");
            for (CharCharacter ch : randomPool) {
                if (result.size() >= count) break;
                if (!ch.getCharacter().equals(target.getCharacter())
                        && !result.contains(ch.getCharacter())) {
                    result.add(ch.getCharacter());
                }
            }
        }
        return result;
    }

    // ==================== 汉字组词 ====================

    @Override
    @Transactional(readOnly = true)
    public List<GameQuestionDTO> generateWordFormationQuestions(List<String> hskLevels) {
        List<CharCharacter> chars = charCharacterRepository
                .findRandomPublishedWithMinWords(hskLevels, QUESTION_COUNT);
        if (chars.isEmpty()) {
            log.warn("汉字组词: 等级 {} 无含足够组词的汉字", hskLevels);
            return Collections.emptyList();
        }

        List<GameQuestionDTO> questions = new ArrayList<>();
        Set<Integer> usedCharIds = new HashSet<>();
        for (int i = 0; i < chars.size(); i++) {
            CharCharacter ch = chars.get(i);
            if (!usedCharIds.add(ch.getId())) {
                continue;
            }
            List<CharWord> words = charWordRepository.findByCharIdAndStatus(ch.getId(), 1);
            if (words.size() < 2) {
                continue;
            }
            GameQuestionDTO q = buildWordFormationQuestion(ch, words, i + 1, hskLevels);
            questions.add(q);
        }
        return questions;
    }

    private GameQuestionDTO buildWordFormationQuestion(CharCharacter ch, List<CharWord> words,
                                                        int index, List<String> hskLevels) {
        GameQuestionDTO q = new GameQuestionDTO();
        q.setGameType("word_formation");
        q.setQuestionIndex(index);
        q.setStem(ch.getCharacter());
        q.setCharacter(ch.getCharacter());
        q.setPinyin(ch.getPinyin());

        // 正确答案: 随机取一条组词
        Collections.shuffle(words);
        CharWord correctWord = words.get(0);

        // 干扰项: 从 vocab_word 取同等级词（排除含目标字的）
        List<VocabWord> distractorVocabs = vocabWordRepository
                .findRandomPublishedExcludingChar(hskLevels, ch.getCharacter(), DISTRACTOR_COUNT + 3);
        // 过滤掉与正确答案相同的词
        List<VocabWord> filtered = new ArrayList<>();
        for (VocabWord vw : distractorVocabs) {
            if (filtered.size() >= DISTRACTOR_COUNT) break;
            if (!vw.getWord().equals(correctWord.getWordItem())) {
                filtered.add(vw);
            }
        }
        // 如果同等级不足，放宽等级重试
        if (filtered.size() < DISTRACTOR_COUNT) {
            List<String> allLevels = Arrays.asList("1", "2", "3", "4", "5", "6");
            List<VocabWord> fallback = vocabWordRepository
                    .findRandomPublishedExcludingChar(allLevels, ch.getCharacter(), DISTRACTOR_COUNT + 3);
            for (VocabWord vw : fallback) {
                if (filtered.size() >= DISTRACTOR_COUNT) break;
                if (!vw.getWord().equals(correctWord.getWordItem())
                        && !containsVocabWord(filtered, vw)) {
                    filtered.add(vw);
                }
            }
        }

        // 构建选项
        List<GameOptionDTO> options = new ArrayList<>();

        GameOptionDTO correctOption = new GameOptionDTO();
        correctOption.setText(correctWord.getWordItem());
        correctOption.setIsCorrect(true);
        options.add(correctOption);

        for (VocabWord vw : filtered) {
            GameOptionDTO opt = new GameOptionDTO();
            opt.setText(vw.getWord());
            opt.setIsCorrect(false);
            options.add(opt);
        }

        Collections.shuffle(options);
        assignKeys(options, q);

        // 解析
        GameExplanationDTO exp = new GameExplanationDTO();
        exp.setCorrectWord(correctWord.getWordItem());
        exp.setCorrectWordPinyin(correctWord.getPinyin());
        exp.setCorrectWordPos(correctWord.getPartOfSpeech());
        exp.setCorrectWordMeaning(parseTranslations(correctWord.getWordItemTranslations()));
        q.setExplanation(exp);

        return q;
    }

    // ==================== 通用工具方法 ====================

    /**
     * 为打乱后的选项列表分配 A/B/C/D key，并将正确答案的 key 记录到 question 上。
     */
    private void assignKeys(List<GameOptionDTO> options, GameQuestionDTO question) {
        for (int i = 0; i < options.size() && i < OPTION_KEYS.size(); i++) {
            GameOptionDTO opt = options.get(i);
            opt.setKey(OPTION_KEYS.get(i));
            if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                question.setCorrectKey(opt.getKey());
            }
        }
        question.setOptions(options);
    }

    /**
     * 解析 JSON 翻译字段为 TextTranslation 列表。
     */
    private List<TextTranslation> parseTranslations(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.parseTranslationList(json);
        } catch (Exception e) {
            log.warn("解析翻译 JSON 失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private CharCharacter findCharById(List<CharCharacter> chars, Integer id) {
        for (CharCharacter ch : chars) {
            if (ch.getId().equals(id)) {
                return ch;
            }
        }
        return null;
    }

    private boolean containsVocabWord(List<VocabWord> list, VocabWord word) {
        for (VocabWord vw : list) {
            if (vw.getId().equals(word.getId())) {
                return true;
            }
        }
        return false;
    }
}
