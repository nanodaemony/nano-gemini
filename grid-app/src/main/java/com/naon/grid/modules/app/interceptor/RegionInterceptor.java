package com.naon.grid.modules.app.interceptor;

import com.naon.grid.modules.app.service.RegionResolver;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegionInterceptor implements HandlerInterceptor {

    private final RegionResolver regionResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);
        request.setAttribute("_region", region);
        log.debug("Region resolved: ip={}, region={}", ip, region);
        return true;
    }
}
