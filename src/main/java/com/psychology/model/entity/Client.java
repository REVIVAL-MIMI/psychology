package com.psychology.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Data
@EqualsAndHashCode(callSuper = true)
public class Client extends User {
    @Column(name = "full_name", nullable = false)
    private String fullName;

    private Integer age;

    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psychologist_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "clients", "invites"})
    private Psychologist psychologist;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Override
    public String toString() {
        return "Client{" +
                "id=" + getId() +
                ", phone='" + getPhone() + '\'' +
                ", fullName='" + fullName + '\'' +
                ", age=" + age +
                '}';
    }
}