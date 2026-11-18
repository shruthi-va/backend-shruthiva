package com.chubb.claims.claim.application;

import com.chubb.claims.claim.domain.NotificationChannel;

public record ClaimNotificationTarget(
        NotificationChannel channel,
        String address) {
}
