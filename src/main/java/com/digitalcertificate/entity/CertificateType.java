package com.digitalcertificate.entity;

public enum CertificateType {
    COURSE_COMPLETION("Course Completion"),
    DEGREE("Degree"),
    DIPLOMA("Diploma"),
    PARTICIPATION("Participation"),
    ACHIEVEMENT("Achievement");

    private final String label;

    CertificateType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
