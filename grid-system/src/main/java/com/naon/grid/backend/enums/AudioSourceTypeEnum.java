package com.naon.grid.backend.enums;

public enum AudioSourceTypeEnum {

    TTS(1, "tts", "TTS"),

    UPLOAD(2, "upload", "自己上传");

    private final Integer id;

    private final String code;

    private final String description;

    AudioSourceTypeEnum(Integer id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
