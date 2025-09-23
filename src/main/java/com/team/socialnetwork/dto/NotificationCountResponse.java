package com.team.socialnetwork.dto;

public class NotificationCountResponse {
    private long unreadCount;

    public NotificationCountResponse() {}

    public NotificationCountResponse(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}