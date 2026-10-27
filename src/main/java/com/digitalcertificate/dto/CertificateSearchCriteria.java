package com.digitalcertificate.dto;

import com.digitalcertificate.entity.CertificateType;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/** Everything a certificate search can filter and sort by (bound from the query string of the list pages and the API). */
public class CertificateSearchCriteria {
    private String q = "";
    private String filter = "ALL";
    private CertificateType type;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
    private int page;
    private String sort = "createdAt";
    private String dir = "desc";

    public String getQ() { return q == null ? "" : q.trim(); }
    public void setQ(String q) { this.q = q; }
    public String getFilter() { return filter == null || filter.isBlank() ? "ALL" : filter; }
    public void setFilter(String filter) { this.filter = filter; }
    public CertificateType getType() { return type; }
    public void setType(CertificateType type) { this.type = type; }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public int getPage() { return Math.max(page, 0); }
    public void setPage(int page) { this.page = page; }
    public String getSort() { return sort == null || sort.isBlank() ? "createdAt" : sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getDir() { return dir == null || dir.isBlank() ? "desc" : dir; }
    public void setDir(String dir) { this.dir = dir; }
}
