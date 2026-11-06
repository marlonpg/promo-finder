package dev.gamba.promofinder.notify;

import java.util.List;

import dev.gamba.promofinder.domain.NotificationChannel;

public interface Notifier {

	NotificationChannel channel();

	/** Property names that still need a value; empty when the channel is ready. */
	List<String> missingSettings();

	/** False when the channel's settings are missing; the channel is then skipped. */
	default boolean isConfigured() {
		return missingSettings().isEmpty();
	}

	void send(String subject, String body);
}
