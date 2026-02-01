package com.psychology.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardDTO {
    // Общие поля
    private UserType userType;
    private LocalDateTime lastLogin;

    // Для психолога
    private Long totalClients;
    private Long upcomingSessions;
    private Long unreadMessages;
    private Double monthlyRevenue;

    // Для клиента
    private String psychologistName;
    private LocalDateTime nextSession;
    private Long pendingTasks;

    @Data
    public static class QuickStats {
        private String title;
        private Long value;
        private String change; // "+12%"
        private String icon;
    }

    @Data
    public static class ActivityItem {
        private LocalDateTime time;
        private String type; // "session", "message", "journal", "recommendation"
        private String description;
        private String user; // для психолога: имя клиента
        private boolean isNew;
    }

    public enum UserType {
        CLIENT, PSYCHOLOGIST, ADMIN
    }
}