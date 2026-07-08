package com.naon.grid.modules.app.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.enums.CollectionBizTypeEnum;
import com.naon.grid.modules.app.rest.vo.LearningHistoryItemVO;
import com.naon.grid.modules.app.service.LearningHistoryService;
import com.naon.grid.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningHistoryServiceImpl implements LearningHistoryService {

    private static final int MAX_SIZE = 50;
    private static final long TTL_SECONDS = 7776000L; // 90 days
    private static final String ZSET_KEY_PREFIX = "learning:history:";
    private static final String HASH_KEY_PREFIX = "learning:history:meta:";

    private final RedisUtils redisUtils;
    private final CharCharacterService charCharacterService;
    private final VocabWordService vocabWordService;
    private final CharRadicalService charRadicalService;
    private final GrammarPointService grammarPointService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabComparisonGroupService vocabComparisonGroupService;

    @Override
    public void addRecord(Long userId, String bizType, Long contentId) {
        // 1. validate bizType
        CollectionBizTypeEnum type = CollectionBizTypeEnum.fromCode(bizType);
        if (type == null) {
            throw new BadRequestException("不支持的业务类型: " + bizType);
        }

        // 2. validate contentId exists + resolve contentName
        String contentName = resolveContentName(type, contentId);

        // 3. build keys and member
        String zsetKey = ZSET_KEY_PREFIX + userId;
        String hashKey = HASH_KEY_PREFIX + userId;
        String member = bizType + ":" + contentId;
        double score = System.currentTimeMillis();

        // 4. ZADD (auto updates score if member exists)
        redisUtils.zAdd(zsetKey, member, score);

        // 5. HSET metadata
        JSONObject meta = new JSONObject();
        meta.put("contentName", contentName);
        redisUtils.hset(hashKey, member, meta.toJSONString());

        // 6. trim to MAX_SIZE, clean orphan Hash fields
        long count = redisUtils.zCard(zsetKey);
        if (count > MAX_SIZE) {
            long toRemove = count - MAX_SIZE;
            // get the entries to be removed (oldest = lowest score = ranks 0..toRemove-1)
            Set<Object> removedMembers = redisUtils.zRevRange(zsetKey, MAX_SIZE, count - 1);
            redisUtils.zRemRangeByRank(zsetKey, 0, toRemove - 1);
            if (removedMembers != null) {
                for (Object rm : removedMembers) {
                    redisUtils.hdel(hashKey, rm);
                }
            }
        }

        // 7. refresh TTL on both keys
        redisUtils.expire(zsetKey, TTL_SECONDS);
        redisUtils.expire(hashKey, TTL_SECONDS);
    }

    @Override
    public List<LearningHistoryItemVO> getHistory(Long userId) {
        String zsetKey = ZSET_KEY_PREFIX + userId;
        String hashKey = HASH_KEY_PREFIX + userId;

        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisUtils.zRevRangeWithScores(zsetKey, 0, MAX_SIZE - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<LearningHistoryItemVO> result = new ArrayList<>();

        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Object value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null) continue;

            String member = String.valueOf(value);
            int colonIdx = member.lastIndexOf(':');
            if (colonIdx < 0) continue;

            String bizType = member.substring(0, colonIdx);
            String contentIdStr = member.substring(colonIdx + 1);
            Long contentId;
            try {
                contentId = Long.parseLong(contentIdStr);
            } catch (NumberFormatException e) {
                continue;
            }

            // get contentName from Hash
            Object metaObj = redisUtils.hget(hashKey, member);
            String contentName = null;
            if (metaObj != null) {
                JSONObject meta = JSON.parseObject(metaObj.toString());
                contentName = meta.getString("contentName");
            }

            LearningHistoryItemVO vo = new LearningHistoryItemVO();
            vo.setBizType(bizType);
            vo.setContentId(contentId);
            vo.setContentName(contentName);
            if (score != null) {
                vo.setLearnedAt(sdf.format(new Date(score.longValue())));
            }
            result.add(vo);
        }

        return result;
    }

    @Override
    public void removeRecord(Long userId, String bizType, Long contentId) {
        String zsetKey = ZSET_KEY_PREFIX + userId;
        String hashKey = HASH_KEY_PREFIX + userId;
        String member = bizType + ":" + contentId;

        redisUtils.zRemove(zsetKey, member);
        redisUtils.hdel(hashKey, member);
    }

    @Override
    public void clearAll(Long userId) {
        redisUtils.del(ZSET_KEY_PREFIX + userId, HASH_KEY_PREFIX + userId);
    }

    /**
     * Resolve content name by bizType and contentId.
     * Mirrors CollectionWrapper.resolveContentName() pattern.
     * All backend service findById methods throw EntityNotFoundException on miss.
     */
    private String resolveContentName(CollectionBizTypeEnum type, Long contentId) {
        try {
            switch (type) {
                case CHARACTER: {
                    if (contentId > Integer.MAX_VALUE || contentId < Integer.MIN_VALUE) {
                        throw new BadRequestException("汉字ID超出范围");
                    }
                    CharCharacterDto dto = charCharacterService.findById(contentId.intValue());
                    return dto != null ? dto.getCharacter() : null;
                }
                case VOCABULARY: {
                    if (contentId > Integer.MAX_VALUE || contentId < Integer.MIN_VALUE) {
                        throw new BadRequestException("词汇ID超出范围");
                    }
                    VocabWordDto dto = vocabWordService.findById(contentId.intValue());
                    return dto != null ? dto.getWord() : null;
                }
                case RADICAL: {
                    CharRadicalDto dto = charRadicalService.findById(contentId);
                    return dto != null ? dto.getRadical() : null;
                }
                case GRAMMAR: {
                    GrammarPointDto dto = grammarPointService.findById(contentId);
                    return dto != null ? dto.getName() : null;
                }
                case GRAMMAR_COMPARISON: {
                    GrammarComparisonGroupDto dto = grammarComparisonGroupService.findById(contentId);
                    return dto != null ? dto.getGroupKey() : null;
                }
                case VOCAB_COMPARISON: {
                    VocabComparisonGroupDto dto = vocabComparisonGroupService.findById(contentId);
                    return dto != null ? dto.getGroupKey() : null;
                }
                default:
                    return null;
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException(type.getDescription() + "不存在");
        }
    }
}
