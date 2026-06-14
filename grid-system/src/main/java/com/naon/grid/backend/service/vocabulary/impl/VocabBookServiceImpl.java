package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.VocabBook;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.repo.vocabulary.VocabBookRepository;
import com.naon.grid.backend.repo.vocabulary.VocabWordRepository;
import com.naon.grid.backend.service.vocabulary.VocabBookService;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabBookServiceImpl implements VocabBookService {

    private final VocabBookRepository vocabBookRepository;
    private final VocabWordRepository vocabWordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VocabBook> findAvailableBooks() {
        return vocabBookRepository.findByStatusOrderByOrderDesc(StatusEnum.ENABLED.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public VocabBook findAvailableById(Long id) {
        VocabBook book = vocabBookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabBook.class, "id", String.valueOf(id)));
        if (!StatusEnum.ENABLED.getCode().equals(book.getStatus())) {
            throw new EntityNotFoundException(VocabBook.class, "id", String.valueOf(id));
        }
        return book;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabWord> findWordsByBook(VocabBook book) {
        // 方式1：通过 HSK 等级查询
        if (book.getHskLevel() != null && !book.getHskLevel().trim().isEmpty()) {
            return vocabWordRepository.findByHskLevelAndStatusAndPublishStatus(
                    book.getHskLevel().trim(),
                    StatusEnum.ENABLED.getCode(),
                    PublishStatusEnum.PUBLISHED.getCode()
            );
        }

        // 方式2：通过 word_ids 查询
        if (book.getWordIds() != null && !book.getWordIds().trim().isEmpty()) {
            List<Integer> idList = parseWordIds(book.getWordIds());
            if (idList.isEmpty()) {
                return Collections.emptyList();
            }

            List<VocabWord> words = vocabWordRepository.findByIdIn(idList);
            // 过滤：仅保留已发布且有效的词汇
            words.removeIf(w ->
                    !StatusEnum.ENABLED.getCode().equals(w.getStatus())
                            || !PublishStatusEnum.PUBLISHED.getCode().equals(w.getPublishStatus())
            );

            // 按 word_ids 原始顺序排序
            Map<Integer, VocabWord> wordMap = words.stream()
                    .collect(Collectors.toMap(VocabWord::getId, w -> w, (a, b) -> a));
            List<VocabWord> sorted = new ArrayList<>();
            for (Integer id : idList) {
                VocabWord w = wordMap.get(id);
                if (w != null) {
                    sorted.add(w);
                }
            }
            return sorted;
        }

        return Collections.emptyList();
    }

    /**
     * 解析 word_ids JSON 字符串为 Integer 列表
     */
    private List<Integer> parseWordIds(String wordIds) {
        try {
            List<String> strList = JsonUtils.parseStringList(wordIds);
            return strList.stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BadRequestException("词汇ID列表数据解析失败");
        }
    }
}
