package com.naon.grid.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AudioFileFormatEnum {

    MP3(1, "mp3", "MP3"),

    WAV(2, "wav", "WAV"),

    M4A(3, "m4a", "M4A");

    private final Integer id;

    private final String code;

    private final String description;

    AudioFileFormatEnum(Integer id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static AudioFileFormatEnum fromCode(String code) {
        for (AudioFileFormatEnum format : values()) {
            if (format.code.equalsIgnoreCase(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown audio file format: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
