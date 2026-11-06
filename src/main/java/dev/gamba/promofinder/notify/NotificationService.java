package dev.gamba.promofinder.notify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.gamba.promofinder.domain.NotificationChannel;

@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final Map<NotificationChannel, Notifier> notifiers;

	public NotificationService(List<Notifier> notifiers) {
		this.notifiers = notifiers.stream().collect(Collectors.toMap(Notifier::channel, Function.identity()));
	}

	public record ChannelStatus(NotificationChannel channel, boolean configured, List<String> missingSettings) {
	}

	/** All channels in enum order, with what each one still needs. */
	public List<ChannelStatus> status() {
		return Arrays.stream(NotificationChannel.values())
				.map(c -> {
					Notifier n = notifiers.get(c);
					return n == null ? new ChannelStatus(c, false, List.of())
							: new ChannelStatus(c, n.isConfigured(), n.missingSettings());
				})
				.toList();
	}

	public boolean isConfigured(NotificationChannel channel) {
		Notifier notifier = notifiers.get(channel);
		return notifier != null && notifier.isConfigured();
	}

	public boolean anyConfigured() {
		return notifiers.values().stream().anyMatch(Notifier::isConfigured);
	}

	public Map<NotificationChannel, Boolean> configuredChannels() {
		return notifiers.values().stream()
				.collect(Collectors.toMap(Notifier::channel, Notifier::isConfigured, (a, b) -> a, TreeMap::new));
	}

	/** Sends to every requested channel that is configured. Returns the channels that succeeded. */
	public List<NotificationChannel> send(Collection<NotificationChannel> channels, String subject, String body) {
		var sent = new ArrayList<NotificationChannel>();
		for (NotificationChannel channel : channels) {
			Notifier notifier = notifiers.get(channel);
			if (notifier == null || !notifier.isConfigured()) {
				log.warn("Channel {} is not configured, skipping", channel);
				continue;
			}
			try {
				notifier.send(subject, body);
				sent.add(channel);
			}
			catch (RuntimeException e) {
				log.error("Failed to send {} notification", channel, e);
			}
		}
		return sent;
	}
}
