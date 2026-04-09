package com.psychology.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "psychologist_availability",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_psychologist_availability_psychologist_date",
                        columnNames = {"psychologist_id", "work_date"}
                )
        }
)
@Data
public class PsychologistAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psychologist_id", nullable = false)
    private Psychologist psychologist;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "is_working", nullable = false)
    private Boolean working = true;

    @Column(name = "work_start_hour")
    private Integer workStartHour = 9;

    @Column(name = "work_end_hour")
    private Integer workEndHour = 20;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
