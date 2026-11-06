package dev.gamba.promofinder.notify;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import dev.gamba.promofinder.config.PromoProperties;
import dev.gamba.promofinder.domain.NotificationChannel;

@Component
class EmailNotifier implements Notifier {

	private final ObjectProvider<JavaMailSender> mailSender;
	private final PromoProperties.Email props;

	EmailNotifier(ObjectProvider<JavaMailSender> mailSender, PromoProperties props) {
		this.mailSender = mailSender;
		this.props = props.notification().email();
	}

	@Override
	public NotificationChannel channel() {
		return NotificationChannel.EMAIL;
	}

	@Override
	public List<String> missingSettings() {
		var missing = new ArrayList<String>();
		if (mailSender.getIfAvailable() == null) {
			missing.add("spring.mail.host");
		}
		if (!StringUtils.hasText(props.to())) {
			missing.add("promo.notification.email.to");
		}
		return missing;
	}

	@Override
	public void send(String subject, String body) {
		var message = new SimpleMailMessage();
		if (StringUtils.hasText(props.from())) {
			message.setFrom(props.from());
		}
		message.setTo(props.to());
		message.setSubject(subject);
		message.setText(body);
		mailSender.getObject().send(message);
	}
}
