package dev.gamba.promofinder.monitor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** In-memory view of which watches are waiting for, or in the middle of, a Claude search. */
@Component
public class CheckTracker {

	private final Map<Long, Instant> queued = new ConcurrentHashMap<>();
	private final Map<Long, Instant> running = new ConcurrentHashMap<>();

	/** Returns false if the watch is already queued or running. */
	synchronized boolean queue(Long watchId) {
		if (queued.containsKey(watchId) || running.containsKey(watchId)) {
			return false;
		}
		queued.put(watchId, Instant.now());
		return true;
	}

	synchronized void start(Long watchId) {
		queued.remove(watchId);
		running.put(watchId, Instant.now());
	}

	synchronized void finish(Long watchId) {
		queued.remove(watchId);
		running.remove(watchId);
	}

	public Instant runningSince(Long watchId) {
		return running.get(watchId);
	}

	public boolean isQueued(Long watchId) {
		return queued.containsKey(watchId);
	}

	public boolean isBusy() {
		return !running.isEmpty();
	}
}
