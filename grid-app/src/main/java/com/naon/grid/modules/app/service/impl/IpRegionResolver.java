package com.naon.grid.modules.app.service.impl;

import com.naon.grid.modules.app.service.RegionResolver;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;

@Slf4j
@Service
public class IpRegionResolver implements RegionResolver {

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try {
            // Try to load from classpath; if ip2region.xdb not found, fallback to no-op
            InputStream is = new ClassPathResource("ip2region.xdb").getInputStream();
            byte[] cBuff = new byte[is.available()];
            is.read(cBuff);
            is.close();
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("IP2Region searcher initialized successfully");
        } catch (Exception e) {
            log.warn("IP2Region db not found at classpath:ip2region.xdb, using default region C. Error: {}", e.getMessage());
            searcher = null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (Exception e) {
                log.warn("Error closing IP2Region searcher", e);
            }
        }
    }

    @Override
    public String resolve(String ip) {
        if (searcher == null) {
            return "C"; // Default to China
        }

        try {
            String result = searcher.search(ip);
            // ip2region returns format: "中国|华东|上海市|联通"
            if (result != null) {
                return mapToRegion(result);
            }
        } catch (Exception e) {
            log.debug("IP region lookup failed for IP: {}", ip);
        }
        return "C"; // Default fallback
    }

    /**
     * Map ip2region result to our region code A/B/C/D/E
     * Based on country/region mapping rules from the pricing spec.
     */
    String mapToRegion(String raw) {
        if (raw == null || raw.isEmpty()) return "C";

        String[] parts = raw.split("\\|");
        String country = parts.length > 0 ? parts[0] : "";
        String province = parts.length > 2 ? parts[2] : "";

        // C区: 中国大陆
        if ("中国".equals(country)) return "C";

        // A区: 北美、西欧、北欧
        if (containsAny(country, "美国", "加拿大", "英国", "德国", "法国", "意大利", "西班牙",
                "荷兰", "比利时", "瑞士", "瑞典", "挪威", "丹麦", "芬兰", "爱尔兰", "奥地利",
                "葡萄牙", "希腊", "卢森堡", "冰岛")) return "A";

        // B区: 日韩澳新、新加坡及港澳台
        if (containsAny(country, "日本", "韩国", "澳大利亚", "新西兰", "新加坡")) return "B";
        if (containsAny(country, "香港", "澳门", "台湾")) return "B";
        if (containsAny(province, "香港", "澳门", "台湾")) return "B";

        // 中东高收入国家 → B区
        if (containsAny(country, "沙特阿拉伯", "阿联酋", "卡塔尔", "科威特", "阿曼", "巴林")) return "B";

        // D区: 东南亚(除新加坡)、东欧、拉美
        if (containsAny(country, "泰国", "越南", "印度尼西亚", "马来西亚", "菲律宾", "缅甸",
                "柬埔寨", "老挝", "文莱", "东帝汶", "波兰", "捷克", "匈牙利", "罗马尼亚",
                "乌克兰", "俄罗斯", "巴西", "墨西哥", "阿根廷", "智利", "哥伦比亚",
                "秘鲁", "委内瑞拉")) return "D";

        // E区: 非洲、南亚、中亚
        if (containsAny(country, "印度", "巴基斯坦", "孟加拉国", "斯里兰卡", "尼泊尔",
                "南非", "尼日利亚", "肯尼亚", "埃及", "埃塞俄比亚", "坦桑尼亚", "加纳",
                "安哥拉", "乌干达", "哈萨克斯坦", "乌兹别克斯坦")) return "E";

        // Default to A for unrecognized high-income countries
        return "A";
    }

    private boolean containsAny(String text, String... values) {
        if (text == null) return false;
        for (String v : values) {
            if (text.contains(v)) return true;
        }
        return false;
    }
}
