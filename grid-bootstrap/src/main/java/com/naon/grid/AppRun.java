/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid;

import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.utils.SpringBeanHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开启审计功能 -> @EnableJpaAuditing
 *
 * @author Zheng Jie
 * @date 2018/11/15 9:20:19
 */
@Slf4j
@EnableAsync
@EnableScheduling
@RestController
@Api(hidden = true)
@SpringBootApplication(scanBasePackages = {
    "com.naon.grid.modules.billing",
    "com.naon.grid.modules.app",
    "com.naon.grid.modules.system",
    "com.naon.grid.modules.security",
    "com.naon.grid.service",
    "com.naon.grid.backend",
    "com.naon.grid.utils",
    "com.naon.grid.enums",
    "com.naon.grid.exception",
    "com.naon.grid.common",
    "com.naon.grid.config",
    "com.naon.grid.annotation",
    "com.naon.grid.aspect",
    "com.naon.grid.base",
    "com.naon.grid.domain",
    "com.naon.grid.logging"
})
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AppRun {

    public static void main(String[] args) {
        // 获取 active profile，先从系统属性，再从命令行参数，默认 dev
        String profile = System.getProperty("spring.profiles.active");
        if (profile == null) {
            for (String arg : args) {
                if (arg.startsWith("--spring.profiles.active=")) {
                    profile = arg.substring("--spring.profiles.active=".length());
                    break;
                }
            }
        }
        if (profile == null) {
            profile = "dev";
        }

        // 优先加载 .env.{profile}，如果不存在则加载 .env
        Dotenv dotenv;
        String envFileName = ".env." + profile;
        try {
            dotenv = Dotenv.configure()
                    .filename(envFileName)
                    .load();
            System.out.println("Loaded config from " + envFileName);
        } catch (Exception e) {
            // .env.{profile} 不存在，尝试加载 .env
            dotenv = Dotenv.configure()
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
            if (dotenv.entries().iterator().hasNext()) {
                System.out.println("Loaded config from .env");
            } else {
                System.out.println("No .env file found, using default configs");
            }
        }
        // 将 .env 中的配置设置到系统属性
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication springApplication = new SpringApplication(AppRun.class);
        // 监控应用的PID，启动时可指定PID路径：--spring.pid.file=/home/eladmin/app.pid
        // 或者在 application.yml 添加文件路径，方便 kill，kill `cat /home/eladmin/app.pid`
        springApplication.addListeners(new ApplicationPidFileWriter());
        ConfigurableApplicationContext context = springApplication.run(args);
        String port = context.getEnvironment().getProperty("server.port");
        log.info("---------------------------------------------");
        log.info("Local: http://localhost:{}", port);
        log.info("Swagger: http://localhost:{}/doc.html", port);
        log.info("---------------------------------------------");
    }

    @Bean
    public SpringBeanHolder springContextHolder() {
        return new SpringBeanHolder();
    }

    /**
     * 访问首页提示
     *
     * @return /
     */
    @AnonymousGetMapping("/")
    public String index() {
        return "Backend service started successfully";
    }
}
