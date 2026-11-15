package com.mediai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediai.backend.entity.Report;
import com.mediai.backend.entity.User;
import com.mediai.backend.repository.ReportRepository;
import com.mediai.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final WebClient aiServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mediai.upload-dir}")
    private String uploadDir;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository,
                          WebClient aiServiceWebClient) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.aiServiceWebClient = aiServiceWebClient;
    }

    public Report uploadAndAnalyze(String userEmail, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // 1. persist the raw file to disk (swap for S3/GCS in production)
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String storedName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Report report = Report.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .filePath(target.toString())
                .status("PROCESSING")
                .build();
        report = reportRepository.save(report);

        // 2. forward the file to the Python AI microservice for analysis
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            Map<String, Object> aiResult = aiServiceWebClient.post()
                    .uri("/analyze")
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // IMPORTANT: real JSON via Jackson, not Map.toString() (which produces {key=value}, not valid JSON)
            report.setAiSummaryJson(aiResult != null ? objectMapper.writeValueAsString(aiResult) : "{}");
            report.setHealthScore(aiResult != null && aiResult.get("health_score") != null
                    ? ((Number) aiResult.get("health_score")).intValue() : null);
            report.setStatus("ANALYZED");
        } catch (Exception e) {
            report.setStatus("FAILED");
            report.setAiSummaryJson("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }

        return reportRepository.save(report);
    }

    public List<Report> listForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return reportRepository.findByUserOrderByUploadedAtDesc(user);
    }

    public Report getByIdForUser(String userEmail, Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found."));

        if (!report.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Report not found.");
        }
        return report;
    }
}