package com.digitalcertificate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Optional credentials for creating the first administrator on an empty database. */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record InitialAdminProperties(
        @DefaultValue("") String email,
        @DefaultValue("") String password) {
}
