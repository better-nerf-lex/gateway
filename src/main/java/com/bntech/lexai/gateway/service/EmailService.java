package com.bntech.lexai.gateway.service;

import reactor.core.publisher.Mono;

public interface EmailService {

    Mono<Void> sendTeamInvitationEmail(
            String recipientEmail,
            String teamName,
            String inviterName,
            String token,
            String invitationUrl);
}
