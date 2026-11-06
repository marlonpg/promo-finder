package dev.gamba.promofinder.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

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

import dev.gamba.promofinder.domain.Store;
import dev.gamba.promofinder.domain.StoreRepository;
import dev.gamba.promofinder.monitor.WatchChecker;

@RestController
@RequestMapping("/api/stores")
class StoreController {

	record StoreRequest(@NotBlank String name, @NotBlank String domain, Boolean enabled) {
	}

	private final StoreRepository stores;

	StoreController(StoreRepository stores) {
		this.stores = stores;
	}

	@GetMapping
	List<Store> list() {
		return stores.findByDeletedAtIsNullOrderByName();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	Store create(@Valid @RequestBody StoreRequest request) {
		return stores.save(apply(new Store(), request));
	}

	@PutMapping("/{id}")
	Store update(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
		Store store = stores.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return stores.save(apply(store, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id) {
		stores.findById(id).ifPresent(store -> {
			store.setDeletedAt(Instant.now());
			stores.save(store);
		});
	}

	private static Store apply(Store store, StoreRequest request) {
		store.setName(request.name().trim());
		store.setDomain(WatchChecker.normalizeDomain(request.domain()));
		store.setEnabled(request.enabled() == null || request.enabled());
		return store;
	}
}
