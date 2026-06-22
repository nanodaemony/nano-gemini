package com.naon.grid.modules.billing.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.naon.grid.modules.billing.domain")
@EnableJpaRepositories("com.naon.grid.modules.billing.repository")
public class BillingJpaConfig {
}
