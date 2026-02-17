package com.bntech.lexai.gateway.service;

import com.postmarkapp.postmark.Postmark;
import com.postmarkapp.postmark.client.ApiClient;
import com.postmarkapp.postmark.client.data.model.message.Message;
import com.postmarkapp.postmark.client.data.model.message.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class PostmarkEmailService implements EmailService {

    private final ApiClient postmarkClient;
    private final String fromEmail;
    private final boolean emailEnabled;

    public PostmarkEmailService(
            @Value("${postmark.api-token}") String apiToken,
            @Value("${app.email.from}") String fromEmail,
            @Value("${app.email.enabled:true}") boolean emailEnabled) {
        this.postmarkClient = Postmark.getApiClient(apiToken);
        this.fromEmail = fromEmail;
        this.emailEnabled = emailEnabled;
    }

    @Override
    public Mono<Void> sendTeamInvitationEmail(
            String recipientEmail,
            String teamName,
            String inviterName,
            String token,
            String invitationUrl) {
        if (!emailEnabled) {
            log.info("Email sending is disabled, skipping invitation email to {}", recipientEmail);
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
            Message message = new Message(
                    fromEmail,
                    recipientEmail,
                    buildInvitationSubject(teamName),
                    buildInvitationHtmlBody(teamName, inviterName, invitationUrl, token));

            MessageResponse response = postmarkClient.deliverMessage(message);
            log.info("Successfully sent invitation email to {} (MessageID: {})", recipientEmail,
                    response.getMessageId());
            return response;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnError(error -> log.error("Failed to send invitation email to {}", recipientEmail, error))
                .onErrorResume(error -> {
                    // Don't fail the invitation creation if email fails
                    log.warn("Email sending failed but continuing with invitation creation");
                    return Mono.empty();
                });
    }

    private String buildInvitationSubject(String teamName) {
        return String.format("You've been invited to join %s on Paragrafic", teamName);
    }

    private String buildInvitationHtmlBody(String teamName, String inviterName, String invitationUrl, String token) {
        String acceptUrl = invitationUrl + "/accept?token=" + token;
        String rejectUrl = invitationUrl + "/reject?token=" + token;

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            margin: 0;
                            padding: 0;
                            background-color: #f4f4f4;
                        }
                        .container {
                            max-width: 600px;
                            margin: 40px auto;
                            background: white;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #555AB9 0%%, #91BAF8 100%%);
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            color: white;
                            margin: 0;
                            font-size: 24px;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .content p {
                            margin: 0 0 20px;
                            font-size: 16px;
                        }
                        .invite-box {
                            background: #f8f9fa;
                            border-left: 4px solid #555AB9;
                            padding: 20px;
                            margin: 30px 0;
                        }
                        .invite-box strong {
                            color: #555AB9;
                            font-size: 18px;
                        }
                        .button-container {
                            text-align: center;
                            margin: 30px 0;
                        }
                        .button {
                            display: inline-block;
                            padding: 14px 32px;
                            margin: 10px;
                            text-decoration: none;
                            border-radius: 6px;
                            font-weight: bold;
                            font-size: 16px;
                        }
                        .button-accept {
                            background: #555AB9;
                            color: white !important;
                        }
                        .button-reject {
                            background: #e0e0e0;
                            color: #666 !important;
                        }
                        .footer {
                            background: #f8f9fa;
                            padding: 20px 30px;
                            text-align: center;
                            font-size: 14px;
                            color: #666;
                        }
                        .expiry-notice {
                            background: #fff3cd;
                            border: 1px solid #ffc107;
                            border-radius: 4px;
                            padding: 12px;
                            margin: 20px 0;
                            font-size: 14px;
                            color: #856404;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✉️ Team Invitation</h1>
                        </div>
                        <div class="content">
                            <p>Hi there,</p>
                            <p><strong>%s</strong> has invited you to join their team on Paragrafic.</p>

                            <div class="invite-box">
                                <strong>%s</strong>
                            </div>

                            <p>Paragrafic is a legal document management and generation platform. By accepting this invitation, you'll be able to collaborate with your team on legal documents and cases.</p>

                            <div class="expiry-notice">
                                ⏰ <strong>This invitation expires in 7 days.</strong>
                            </div>

                            <div class="button-container">
                                <a href="%s" class="button button-accept">Accept Invitation</a>
                                <a href="%s" class="button button-reject">Decline</a>
                            </div>

                            <p style="font-size: 14px; color: #666; margin-top: 30px;">
                                If you weren't expecting this invitation, you can safely ignore this email.
                            </p>
                        </div>
                        <div class="footer">
                            <p>© 2026 Paragrafic. All rights reserved.</p>
                            <p>This is an automated email, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(inviterName, teamName, acceptUrl, rejectUrl);
    }
}
