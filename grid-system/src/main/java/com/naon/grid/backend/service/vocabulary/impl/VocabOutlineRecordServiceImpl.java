package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.VocabOutlineRecord;
import com.naon.grid.backend.repo.vocabulary.VocabOutlineRecordRepository;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabOutlineRecordMapper;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VocabOutlineRecordServiceImpl implements VocabOutlineRecordService {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef]+$");

    private final VocabOutlineRecordRepository vocabOutlineRecordRepository;
    private final VocabOutlineRecordMapper vocabOutlineRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordIfNeeded(String searchWord) {
        String processedWord = preprocessSearchWord(searchWord);
        if (processedWord == null) {
            return;
        }

        // 简单查询后处理 - Java 8兼容写法
        Optional<VocabOutlineRecord> recordOpt = vocabOutlineRecordRepository.findByWord(processedWord);
        if (recordOpt.isPresent()) {
            // 已存在，增加计数
            VocabOutlineRecord record = recordOpt.get();
            record.setSearchCount(record.getSearchCount() + 1);
            vocabOutlineRecordRepository.save(record);
        } else {
            // 不存在，插入新记录
            VocabOutlineRecord record = new VocabOutlineRecord();
            record.setWord(processedWord);
            record.setSearchCount(1);
            record.setStatus(0); // 0:未处理
            vocabOutlineRecordRepository.save(record);
        }
    }

    @Override
    public PageResult<VocabOutlineRecordDto> queryAll(VocabOutlineRecordQueryCriteria criteria, Pageable pageable) {
        Page<VocabOutlineRecord> page = vocabOutlineRecordRepository.findAll(
                (root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder),
                pageable
        );
        return PageUtil.toPage(page.map(vocabOutlineRecordMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsCompleted(Integer id) {
        VocabOutlineRecord record = vocabOutlineRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabOutlineRecord.class, "id", String.valueOf(id)));
        record.setStatus(1); // 1:已处理
        vocabOutlineRecordRepository.save(record);
    }

    /**
     * 预处理并验证搜索词
     * @param searchWord 原始搜索词
     * @return 处理后的搜索词，如果不符合条件返回null
     */
    private String preprocessSearchWord(String searchWord) {
        if (searchWord == null || searchWord.trim().isEmpty()) {
            return null;
        }

        // 去掉所有空格（包括首尾和中间）
        String processed = searchWord.replaceAll("\\s+", "");

        // 检查长度（不超过50字符）
        if (processed.length() > 50) {
            return null;
        }

        // 检查是否为全中文+中文标点
        if (!CHINESE_PATTERN.matcher(processed).matches()) {
            return null;
        }

        return processed;
    }
}
