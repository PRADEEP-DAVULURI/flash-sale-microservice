package com.mediai.backend.controller;

import com.mediai.backend.entity.Report;
import com.mediai.backend.service.ReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Report upload(@RequestParam("file") MultipartFile file, Authentication auth) throws IOException {
        // auth.getName() is the email extracted from the JWT by JwtAuthFilter
        return reportService.uploadAndAnalyze(auth.getName(), file);
    }

    @GetMapping
    public List<Report> list(Authentication auth) {
        return reportService.listForUser(auth.getName());
    }

    @GetMapping("/{id}")
    public Report getById(@PathVariable Long id, Authentication auth) {
        return reportService.getByIdForUser(auth.getName(), id);
    }
}