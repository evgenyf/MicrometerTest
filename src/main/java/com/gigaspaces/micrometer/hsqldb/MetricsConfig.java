package com.gigaspaces.micrometer.hsqldb;

import io.micrometer.core.instrument.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public HsqldbRegistryConfig hsqldbRegistryConfig() {
        return HsqldbRegistryConfig.DEFAULT;
    }

    @Bean
    public HsqldbMetricsRegistry hsqldbMeterRegistry(HsqldbRegistryConfig customRegistryConfig, Clock clock) {
        return new HsqldbMetricsRegistry(customRegistryConfig, clock);
    }
}