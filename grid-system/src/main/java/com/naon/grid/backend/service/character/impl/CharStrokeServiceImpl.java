package com.naon.grid.backend.service.character.impl;

import com.naon.grid.backend.domain.character.CharStroke;
import com.naon.grid.backend.repo.character.CharStrokeRepository;
import com.naon.grid.backend.service.character.CharStrokeService;
import com.naon.grid.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharStrokeServiceImpl implements CharStrokeService {

    private final CharStrokeRepository charStrokeRepository;

    @Override
    public String findByCharacter(String character) {
        return charStrokeRepository
                .findByCharacterAndStatus(character, StatusEnum.ENABLED.getCode())
                .map(CharStroke::getStroke)
                .orElse(null);
    }
}
