package dev.gamba.promofinder.notify;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import dev.gamba.promofinder.config.PromoProperties;
import dev.gamba.promofinder.domain.NotificationChannel;

/** SMS through Twilio's REST API. */
@Component
class SmsNotifier implements Notifier {

	private static final int MAX_SMS_LENGTH = 320;

	private final RestClient http;
	private final PromoProperties.Sms props;

	SmsNotifier(PromoProperties props) {
		this.http = RestClient.builder().baseUrl("https://api.twilio.com").build();
		this.props = props.notification().sms();
	}

	@Override
	public NotificationChannel channel() {
		return NotificationChannel.SMS;
	}

	@Override
	public List<String> missingSettings() {
		return Stream.of(
				StringUtils.hasText(props.twilioAccountSid()) ? null : "promo.notification.sms.twilio-account-sid",
				StringUtils.hasText(props.twilioAuthToken()) ? null : "promo.notification.sms.twilio-auth-token",
				StringUtils.hasText(props.from()) ? null : "promo.notification.sms.from",
				StringUtils.hasText(props.to()) ? null : "promo.notification.sms.to")
				.filter(s -> s != null).toList();
	}

	@Override
	public void send(String subject, String body) {
		String text = subject + "\n" + body;
		var form = new LinkedMultiValueMap<String, String>();
		form.add("From", props.from());
		form.add("To", props.to());
		form.add("Body", text.length() > MAX_SMS_LENGTH ? text.substring(0, MAX_SMS_LENGTH - 3) + "..." : text);
		http.post()
				.uri("/2010-04-01/Accounts/{sid}/Messages.json", props.twilioAccountSid())
				.headers(h -> h.setBasicAuth(props.twilioAccountSid(), props.twilioAuthToken()))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.toBodilessEntity();
	}
}
