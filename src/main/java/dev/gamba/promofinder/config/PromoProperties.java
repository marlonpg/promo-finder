package dev.gamba.promofinder.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("promo")
public record PromoProperties(Claude claude, Notification notification) {

	public record Claude(String command, String model, Duration timeout, String workDir) {
	}

	public record Notification(Email email, Telegram telegram, Sms sms) {
	}

	public record Email(String from, String to) {
	}

	public record Telegram(String botToken, String chatId) {
	}

	public record Sms(String twilioAccountSid, String twilioAuthToken, String from, String to) {
	}
}
