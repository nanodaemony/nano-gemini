package com.naon.grid.modules.app.service.dto;

import lombok.Data;

@Data
public class DeleteAccountRequest {

    private String password;

    private String emailCode;
}
