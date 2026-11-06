package dev.gamba.promofinder.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.gamba.promofinder.domain.NotificationChannel;
import dev.gamba.promofinder.notify.NotificationService;

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

	private final NotificationService notifications;

	NotificationController(NotificationService notifications) {
		this.notifications = notifications;
	}

	/** Which channels have their settings filled in. */
	@GetMapping("/channels")
	Map<NotificationChannel, Boolean> channels() {
		return notifications.configuredChannels();
	}

	@PostMapping("/test/{channel}")
	Map<String, Boolean> test(@PathVariable NotificationChannel channel) {
		boolean sent = !notifications.send(List.of(channel), "Promo Finder test",
				"If you can read this, " + channel + " notifications work.").isEmpty();
		return Map.of("sent", sent);
	}
}
