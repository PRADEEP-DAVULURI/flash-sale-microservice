package com.mediai.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;



@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore // never serialize the lazy Hibernate proxy for this into JSON responses
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String fileName;
    private String filePath;

    @Builder.Default
    private String status = "PROCESSING"; // PROCESSING | ANALYZED | FAILED

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiSummaryJson; // raw JSON returned by the Python AI service

    private Integer healthScore;

    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

	
    public static void main(String[] args) {

        Report report = Report.builder()
                .fileName("test.pdf")
                .status("PROCESSING")
                .build();

        System.out.println(report.getFileName());
    }

}
