package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.service.dto.AgentRegisterDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import javax.servlet.http.HttpServletRequest;

public interface AgentService {
    TokenDTO register(AgentRegisterDTO dto, HttpServletRequest request);
    void approve(Integer agentId);
    void reject(Integer agentId, String reason);
}
