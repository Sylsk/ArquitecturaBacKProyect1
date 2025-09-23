package com.team.socialnetwork.dto;

public class MarkNotificationReadRequest {
    private Long notificationId;

    public MarkNotificationReadRequest() {}

    public MarkNotificationReadRequest(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
}