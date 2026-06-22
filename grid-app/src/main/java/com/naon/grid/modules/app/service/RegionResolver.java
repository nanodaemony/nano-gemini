package com.naon.grid.modules.app.service;

public interface RegionResolver {
    /**
     * 根据IP地址解析所属区域 A/B/C/D/E
     */
    String resolve(String ip);
}
