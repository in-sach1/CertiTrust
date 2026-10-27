package com.digitalcertificate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** CertiTrust - Verify Every Credential. Trust Every Certificate. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DigitalCertificateApplication {
    public static void main(String[] args) {
        SpringApplication.run(DigitalCertificateApplication.class, args);
    }
}
