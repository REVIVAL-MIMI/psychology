package com.psychology.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "psychologists")
@Data
@EqualsAndHashCode(callSuper = true)
public class Psychologist extends User {
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String education;

    private String specialization;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String photoUrl;

    private boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @OneToMany(mappedBy = "psychologist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Client> clients;

    @OneToMany(mappedBy = "psychologist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Invite> invites;

    // ОСТАВЛЯЕМ: При toString() исключаем ленивые коллекции
    @Override
    public String toString() {
        return "Psychologist{" +
                "id=" + getId() +
                ", phone='" + getPhone() + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", verified=" + verified +
                '}';
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (verified && verifiedAt == null) {
            verifiedAt = LocalDateTime.now();
        }
    }
}