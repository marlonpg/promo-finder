package dev.gamba.promofinder.ui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.gamba.promofinder.monitor.WatchUpdated;

/**
 * Pushes Server-Sent Events to open pages. Each event's data is just a name ("watch-5", "summary");
 * the page then fetches the fresh HTML itself, in its own language.
 */
@Component
class UiEvents {

	/** Also lets us notice pages that went away without saying so, and drop them. */
	private static final long HEARTBEAT_SECONDS = 25;

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(
			Thread.ofPlatform().name("sse-heartbeat").daemon().factory());

	UiEvents() {
		heartbeat.scheduleAtFixedRate(this::ping, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
	}

	SseEmitter subscribe() {
		var emitter = new SseEmitter(0L);
		emitters.add(emitter);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));
		return emitter;
	}

	@EventListener
	void onWatchUpdated(WatchUpdated event) {
		send(SseEmitter.event().data("watch-" + event.watchId()));
		send(SseEmitter.event().data("summary"));
	}

	private void ping() {
		send(SseEmitter.event().comment("ping"));
	}

	private synchronized void send(SseEmitter.SseEventBuilder event) {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(event);
			}
			catch (IOException | IllegalStateException e) {
				emitters.remove(emitter);
				emitter.completeWithError(e);
			}
		}
	}

	@PreDestroy
	void shutdown() {
		heartbeat.shutdownNow();
		emitters.forEach(SseEmitter::complete);
	}
}
