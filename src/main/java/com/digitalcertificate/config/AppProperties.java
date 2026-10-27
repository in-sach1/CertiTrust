package com.digitalcertificate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Typed access to the "app.*" settings in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:8080") String baseUrl,
        @DefaultValue("false") boolean demoData,
        @DefaultValue("true") boolean logIp,
        @DefaultValue Storage storage) {

    public record Storage(
            @DefaultValue("./storage/uploads") String uploadDir,
            @DefaultValue("./storage/certificates") String certificateDir,
            @DefaultValue("./storage/qr") String qrDir) { }
}
