package dev.gamba.promofinder.ui;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.gamba.promofinder.domain.Store;
import dev.gamba.promofinder.domain.StoreRepository;
import dev.gamba.promofinder.monitor.WatchChecker;

@Controller
@RequestMapping("/ui/stores")
class StoreUiController {

	public record Suggestion(String name, String domain) {
	}

	/** Popular Brazilian shops offered as one-click additions. */
	static final List<Suggestion> SUGGESTIONS = List.of(
			new Suggestion("Amazon BR", "amazon.com.br"),
			new Suggestion("KaBuM!", "kabum.com.br"),
			new Suggestion("Magazine Luiza", "magazineluiza.com.br"),
			new Suggestion("Mercado Livre", "mercadolivre.com.br"),
			new Suggestion("Casas Bahia", "casasbahia.com.br"),
			new Suggestion("Fast Shop", "fastshop.com.br"),
			new Suggestion("Americanas", "americanas.com.br"),
			new Suggestion("Carrefour", "carrefour.com.br"),
			new Suggestion("Pichau", "pichau.com.br"),
			new Suggestion("Terabyte", "terabyteshop.com.br"));

	private static final String STORES_CHANGED = "storesChanged";

	private final StoreRepository stores;
	private final Ui ui;

	StoreUiController(StoreRepository stores, Ui ui) {
		this.stores = stores;
		this.ui = ui;
	}

	@GetMapping
	String list(Model model) {
		addListModel(model);
		return "fragments/stores :: content";
	}

	void addListModel(Model model) {
		List<Store> all = stores.findByDeletedAtIsNullOrderByName();
		Set<String> domains = all.stream().map(Store::getDomain).collect(Collectors.toSet());
		model.addAttribute("stores", all);
		model.addAttribute("suggestions", SUGGESTIONS.stream().filter(s -> !domains.contains(s.domain())).toList());
	}

	@PostMapping
	String add(@RequestParam(required = false) String name, @RequestParam(required = false) String domain,
			Model model, HttpServletResponse response) {
		String cleanDomain = domain == null ? "" : WatchChecker.normalizeDomain(domain);
		if (!cleanDomain.matches("[a-z0-9-]+(\\.[a-z0-9-]+)+")) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.store.invalid")));
		}
		boolean exists = stores.findByDeletedAtIsNullOrderByName().stream().anyMatch(s -> s.getDomain().equals(cleanDomain));
		if (exists) {
			return ui.toast(model, response, Toast.info(ui.msg("toast.store.exists", cleanDomain)));
		}
		var store = new Store();
		store.setName(name == null || name.isBlank() ? cleanDomain : name.trim());
		store.setDomain(cleanDomain);
		stores.save(store);
		return ui.toast(model, response, Toast.success(ui.msg("toast.store.added", store.getName())), STORES_CHANGED,
				"storeAdded");
	}

	@PostMapping("/{id}/toggle")
	String toggle(@PathVariable Long id, Model model, HttpServletResponse response) {
		Optional<Store> found = stores.findById(id).filter(s -> s.getDeletedAt() == null);
		if (found.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.store.missing")), STORES_CHANGED);
		}
		Store store = found.get();
		store.setEnabled(!store.isEnabled());
		stores.save(store);
		String key = store.isEnabled() ? "toast.store.enabled" : "toast.store.disabled";
		return ui.toast(model, response, Toast.success(ui.msg(key, store.getName())), STORES_CHANGED);
	}

	@PostMapping("/{id}/delete")
	String delete(@PathVariable Long id, Model model, HttpServletResponse response) {
		Optional<Store> found = stores.findById(id).filter(s -> s.getDeletedAt() == null);
		if (found.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.store.missing")), STORES_CHANGED);
		}
		Store store = found.get();
		store.setDeletedAt(Instant.now());
		stores.save(store);
		var toast = Toast.undoable(ui.msg("toast.store.deleted", store.getName()), "/ui/stores/" + id + "/restore");
		return ui.toast(model, response, toast, STORES_CHANGED);
	}

	@PostMapping("/{id}/restore")
	String restore(@PathVariable Long id, Model model, HttpServletResponse response) {
		Optional<Store> found = stores.findById(id);
		if (found.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.undo.expired")));
		}
		Store store = found.get();
		store.setDeletedAt(null);
		stores.save(store);
		return ui.toast(model, response, Toast.success(ui.msg("toast.store.restored", store.getName())), STORES_CHANGED);
	}
}
