package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailService(@Value("${brevo.api-key}") String apiKey, @Value("${brevo.sender-email}") String senderEmail, @Value("${brevo.sender-name}") String senderName) {

        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);

        this.restClient = RestClient.builder().baseUrl("https://api.brevo.com/v3").requestFactory(factory).build();
    }

    public void sendVerificationEmail(String recipientEmail, String verificationLink) {
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new ApiException("Email service is not configured");
        }
        // The verification URL is present only behind the action button, not as visible text.
        String safeLink = verificationLink.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
        String html = """
                <!doctype html><html lang="ar" dir="rtl"><head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:32px 12px;background:#f3f7fa;font-family:Tahoma,Arial,sans-serif;color:#1f2d3d">
                <table role="presentation" cellpadding="0" cellspacing="0" style="width:100%%;max-width:540px;margin:0 auto;background:#fff;border-radius:20px;border:1px solid #e4edf2">
                <tr><td style="padding:36px 30px;text-align:center">
                <div style="font-size:30px;font-weight:800;color:#176c86;letter-spacing:2px">NAWELL</div>
                <div style="font-size:18px;font-weight:bold;color:#176c86;margin-top:5px">ناول</div>
                <h1 style="font-size:25px;line-height:1.5;margin:30px 0 12px">تأكيد البريد الإلكتروني</h1>
                <p style="font-size:16px;line-height:1.9;color:#526474;margin:0 0 28px">أهلًا بك في ناول! اضغط الزر التالي لتأكيد بريدك الإلكتروني والبدء في تبادل المهارات</p>
                <a href="%s" style="display:inline-block;background:#176c86;color:#fff;text-decoration:none;padding:15px 34px;border-radius:12px;font-size:16px;font-weight:bold">تأكيد البريد الإلكتروني</a>
                <p style="font-size:13px;line-height:1.8;color:#7c8b98;margin:28px 0 0">الزر صالح لمدة 24 ساعة. إذا لم تطلب هذه الرسالة، يمكنك تجاهلها</p>
                </td></tr></table>
                <p style="text-align:center;font-size:12px;color:#8a98a4;margin-top:20px">NAWELL · ناول</p>
                </body></html>
                """.formatted(safeLink);
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", recipientEmail)),
                "subject", "ناول | تأكيد البريد الإلكتروني",
                "htmlContent", html,
                "textContent", "أهلًا بك في ناول. افتح الرسالة بصيغة HTML واضغط زر تأكيد البريد الإلكتروني. تنتهي صلاحية الطلب بعد 24 ساعة."
        );
        try {
            restClient.post().uri("/smtp/email").header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (org.springframework.web.client.RestClientResponseException e) {
            System.err.println("Brevo verification email rejected with HTTP " + e.getStatusCode().value());
            throw new ApiException("Could not send verification email. Please try again later");
        } catch (RestClientException e) {
            System.err.println("Brevo verification email request failed: " + e.getClass().getSimpleName());
            throw new ApiException("Could not send verification email. Please try again later");
        }
    }

    /** Branded notification layout. Escape all dynamic content before placing it in HTML. */
    private String notificationHtml(String subject, String message) {
        String title = escapeHtml(subject);
        String body = escapeHtml(message).replace("\n", "<br>");
        return """
                <!doctype html><html lang="ar" dir="rtl"><head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:32px 12px;background:#f3f7fa;font-family:Tahoma,Arial,sans-serif;color:#1f2d3d">
                <table role="presentation" cellpadding="0" cellspacing="0" style="width:100%;max-width:540px;margin:0 auto;background:#fff;border-radius:20px;border:1px solid #e4edf2">
                <tr><td style="padding:36px 30px;text-align:center">
                <div style="font-size:30px;font-weight:800;color:#176c86;letter-spacing:2px">NAWELL</div>
                <div style="font-size:18px;font-weight:bold;color:#176c86;margin-top:5px">ناول</div>
                <h1 style="font-size:23px;line-height:1.5;margin:28px 0 16px">__TITLE__</h1>
                <p style="font-size:16px;line-height:1.9;color:#526474;margin:0">__BODY__</p>
                <p style="font-size:13px;color:#7c8b98;margin-top:32px">NAWELL · ناول</p>
                </td></tr></table></body></html>
                """.replace("__TITLE__", title).replace("__BODY__", body);
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ارسال ايميل عام (نعطيه العنوان والنص) لاشعارات التوكنز
    public void sendEmail(String recipientEmail, String subject, String text) {
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new ApiException("Email service is not configured");
        }

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(
                        Map.of("email", recipientEmail)
                ),
                "subject", subject,
                "textContent", text,
                "htmlContent", notificationHtml(subject, text)
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException exception) {
            throw new ApiException("Could not send email");
        }
    }

    public void sendSessionEmail(
            String recipientEmail,
            String subject,
            String message) {

        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new ApiException("Email service is not configured");
        }

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(
                        Map.of("email", recipientEmail)
                ),
                "subject", subject,
                "textContent", message,
                "htmlContent", notificationHtml(subject, message)
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException exception) {
            throw new ApiException(
                    "Could not send email. Please try again later"
            );
        }
    }
}
