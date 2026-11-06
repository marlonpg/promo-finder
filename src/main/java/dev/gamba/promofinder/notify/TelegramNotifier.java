package dev.gamba.promofinder.notify;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import dev.gamba.promofinder.config.PromoProperties;
import dev.gamba.promofinder.domain.NotificationChannel;

@Component
class TelegramNotifier implements Notifier {

	private final RestClient http;
	private final PromoProperties.Telegram props;

	TelegramNotifier(PromoProperties props) {
		this.http = RestClient.builder().baseUrl("https://api.telegram.org").build();
		this.props = props.notification().telegram();
	}

	@Override
	public NotificationChannel channel() {
		return NotificationChannel.TELEGRAM;
	}

	@Override
	public List<String> missingSettings() {
		return Stream.of(
				StringUtils.hasText(props.botToken()) ? null : "promo.notification.telegram.bot-token",
				StringUtils.hasText(props.chatId()) ? null : "promo.notification.telegram.chat-id")
				.filter(s -> s != null).toList();
	}

	@Override
	public void send(String subject, String body) {
		http.post()
				.uri("/bot{token}/sendMessage", props.botToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("chat_id", props.chatId(), "text", subject + "\n\n" + body))
				.retrieve()
				.toBodilessEntity();
	}
}
