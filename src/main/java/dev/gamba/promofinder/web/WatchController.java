package dev.gamba.promofinder.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.gamba.promofinder.domain.NotificationChannel;
import dev.gamba.promofinder.domain.Offer;
import dev.gamba.promofinder.domain.OfferRepository;
import dev.gamba.promofinder.domain.Watch;
import dev.gamba.promofinder.domain.WatchRepository;
import dev.gamba.promofinder.monitor.WatchChecker;

@RestController
@RequestMapping("/api/watches")
class WatchController {

	record WatchRequest(@NotBlank String product, String details,
			@NotNull @DecimalMin("0.01") BigDecimal maxPrice,
			@Min(5) Integer intervalMinutes, Boolean active, Set<NotificationChannel> channels) {
	}

	private final WatchRepository watches;
	private final OfferRepository offers;
	private final WatchChecker checker;

	WatchController(WatchRepository watches, OfferRepository offers, WatchChecker checker) {
		this.watches = watches;
		this.offers = offers;
		this.checker = checker;
	}

	@GetMapping
	List<Watch> list() {
		return watches.findByDeletedAtIsNullOrderByCreatedAtDesc();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	Watch create(@Valid @RequestBody WatchRequest request) {
		return watches.save(apply(new Watch(), request));
	}

	@PutMapping("/{id}")
	Watch update(@PathVariable Long id, @Valid @RequestBody WatchRequest request) {
		return watches.save(apply(find(id), request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id) {
		Watch watch = find(id);
		watch.setDeletedAt(Instant.now());
		watches.save(watch);
	}

	/** Starts a check in the background; it can take a few minutes. */
	@PostMapping("/{id}/check")
	@ResponseStatus(HttpStatus.ACCEPTED)
	void checkNow(@PathVariable Long id) {
		checker.requestCheck(find(id).getId());
	}

	@GetMapping("/{id}/offers")
	List<Offer> offers(@PathVariable Long id) {
		return offers.findTop50ByWatchIdOrderByFoundAtDesc(id);
	}

	private Watch find(Long id) {
		return watches.findById(id).filter(w -> w.getDeletedAt() == null).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private static Watch apply(Watch watch, WatchRequest request) {
		watch.setProduct(request.product().trim());
		watch.setDetails(request.details());
		watch.setMaxPrice(request.maxPrice());
		watch.setIntervalMinutes(request.intervalMinutes() == null ? 60 : request.intervalMinutes());
		watch.setActive(request.active() == null || request.active());
		watch.setChannels(request.channels() == null || request.channels().isEmpty()
				? EnumSet.noneOf(NotificationChannel.class)
				: EnumSet.copyOf(request.channels()));
		return watch;
	}
}
