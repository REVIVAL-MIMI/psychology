package com.psychology.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.psychology.dto.Views;
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
    @JsonView(Views.Internal.class)
    @JsonIgnoreProperties(value = {"psychologist.clients", "psychologist.invites"}, allowSetters = true)
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