package com.naon.grid.backend.service.common;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;

import java.util.Collection;
import java.util.Map;

public interface ExampleSentenceService {

    ExampleSentenceDto findOne(String bizType, Long bizId);

    Map<Long, ExampleSentenceDto> findByBizIds(String bizType, Collection<Long> bizIds);

    ExampleSentenceDto syncOne(String bizType, Long bizId, ExampleSentenceDto sentence);

    void disableByBizIds(String bizType, Collection<Long> bizIds);
}