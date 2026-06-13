package com.naon.grid.backend.service.character.impl;

import com.naon.grid.backend.domain.character.CharBook;
import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.repo.character.CharBookRepository;
import com.naon.grid.backend.repo.character.CharCharacterRepository;
import com.naon.grid.backend.service.character.CharBookService;
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
public class CharBookServiceImpl implements CharBookService {

    private final CharBookRepository charBookRepository;
    private final CharCharacterRepository charCharacterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CharBook> findAvailableBooks() {
        return charBookRepository.findByStatusOrderByOrderDesc(StatusEnum.ENABLED.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public CharBook findAvailableById(Long id) {
        CharBook book = charBookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharBook.class, "id", String.valueOf(id)));
        if (!StatusEnum.ENABLED.getCode().equals(book.getStatus())) {
            throw new EntityNotFoundException(CharBook.class, "id", String.valueOf(id));
        }
        return book;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CharCharacter> findCharactersByBook(CharBook book) {
        // 方式1：通过 HSK 等级查询
        if (book.getHskLevel() != null && !book.getHskLevel().trim().isEmpty()) {
            return charCharacterRepository.findByLevelAndStatusAndPublishStatus(
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

            List<CharCharacter> characters = charCharacterRepository.findByIdIn(idList);
            // 过滤：仅保留已发布且有效的汉字
            characters.removeIf(c ->
                    !StatusEnum.ENABLED.getCode().equals(c.getStatus())
                            || !PublishStatusEnum.PUBLISHED.getCode().equals(c.getPublishStatus())
            );

            // 按 word_ids 原始顺序排序
            Map<Integer, CharCharacter> charMap = characters.stream()
                    .collect(Collectors.toMap(CharCharacter::getId, c -> c, (a, b) -> a));
            List<CharCharacter> sorted = new ArrayList<>();
            for (Integer id : idList) {
                CharCharacter c = charMap.get(id);
                if (c != null) {
                    sorted.add(c);
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
            throw new BadRequestException("汉字ID列表数据解析失败");
        }
    }
}
