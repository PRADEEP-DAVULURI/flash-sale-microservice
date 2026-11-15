package com.mediai.backend.repository;

import com.mediai.backend.entity.Report;
import com.mediai.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByUserOrderByUploadedAtDesc(User user);
}
