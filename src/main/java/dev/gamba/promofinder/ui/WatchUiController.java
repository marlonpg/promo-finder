package dev.gamba.promofinder.ui;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.gamba.promofinder.domain.NotificationChannel;
import dev.gamba.promofinder.domain.Watch;
import dev.gamba.promofinder.domain.WatchRepository;
import dev.gamba.promofinder.monitor.WatchChecker;
import dev.gamba.promofinder.notify.NotificationService;

/** htmx endpoints behind the watches dashboard. They return HTML fragments, never full pages. */
@Controller
@RequestMapping("/ui/watches")
class WatchUiController {

	private static final String WATCHES_CHANGED = "watchesChanged";

	private final WatchRepository watches;
	private final WatchViews views;
	private final WatchChecker checker;
	private final NotificationService notifications;
	private final Fmt fmt;
	private final Ui ui;

	WatchUiController(WatchRepository watches, WatchViews views, WatchChecker checker,
			NotificationService notifications, Fmt fmt, Ui ui) {
		this.watches = watches;
		this.views = views;
		this.checker = checker;
		this.notifications = notifications;
		this.fmt = fmt;
		this.ui = ui;
	}

	@GetMapping
	String list(Model model) {
		model.addAttribute("views", views.of(watches.findByDeletedAtIsNullOrderByCreatedAtDesc()));
		return "fragments/watches :: list";
	}

	@GetMapping("/summary")
	String summary(Model model) {
		model.addAttribute("summary", ui.summary());
		return "fragments/watches :: stats";
	}

	@GetMapping("/{id}/card")
	String card(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean detail, Model model) {
		Optional<Watch> watch = find(id);
		if (watch.isEmpty()) {
			return Ui.NOTHING_VIEW;
		}
		model.addAttribute("v", views.of(watch.get()));
		model.addAttribute("onDetail", detail);
		return "fragments/watches :: card";
	}

	@GetMapping("/new")
	String newForm(Model model) {
		var form = new WatchForm();
		form.setCheckNow(true);
		var configured = EnumSet.noneOf(NotificationChannel.class);
		notifications.status().stream().filter(c -> c.configured()).forEach(c -> configured.add(c.channel()));
		form.setChannels(configured);
		return form(model, null, form, Map.of());
	}

	@GetMapping("/{id}/edit")
	String editForm(@PathVariable Long id, Model model) {
		Optional<Watch> watch = find(id);
		if (watch.isEmpty()) {
			return Ui.NOTHING_VIEW;
		}
		return form(model, id, WatchForm.of(watch.get(), fmt), Map.of());
	}

	@PostMapping
	String create(@ModelAttribute WatchForm form, Model model, HttpServletResponse response) {
		return save(null, new Watch(), form, model, response);
	}

	@PostMapping("/{id}")
	String update(@PathVariable Long id, @ModelAttribute WatchForm form, Model model, HttpServletResponse response) {
		Optional<Watch> watch = find(id);
		if (watch.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.watch.missing")), WATCHES_CHANGED, "closeDrawer");
		}
		return save(id, watch.get(), form, model, response);
	}

	private String save(Long id, Watch watch, WatchForm form, Model model, HttpServletResponse response) {
		Map<String, String> errors = form.validate();
		if (!errors.isEmpty()) {
			return form(model, id, form, errors);
		}
		form.applyTo(watch);
		Watch saved = watches.save(watch);
		if (form.isCheckNow()) {
			checker.requestCheck(saved.getId());
		}
		String message = id == null ? ui.msg("toast.watch.created", saved.getProduct())
				: ui.msg("toast.watch.updated", saved.getProduct());
		return ui.toast(model, response, Toast.success(message), WATCHES_CHANGED, "closeDrawer");
	}

	@PostMapping("/{id}/check")
	String check(@PathVariable Long id, Model model, HttpServletResponse response) {
		Optional<Watch> watch = find(id);
		if (watch.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.watch.missing")), WATCHES_CHANGED);
		}
		Toast toast = checker.requestCheck(id)
				? Toast.info(ui.msg("toast.check.started", watch.get().getProduct()))
				: Toast.info(ui.msg("toast.check.already"));
		return ui.toast(model, response, toast);
	}

	@PostMapping("/{id}/toggle")
	String toggle(@PathVariable Long id, Model model, HttpServletResponse response) {
		Optional<Watch> found = find(id);
		if (found.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.watch.missing")), WATCHES_CHANGED);
		}
		Watch watch = found.get();
		watch.setActive(!watch.isActive());
		watches.save(watch);
		String key = watch.isActive() ? "toast.watch.resumed" : "toast.watch.paused";
		return ui.toast(model, response, Toast.success(ui.msg(key, watch.getProduct())), WATCHES_CHANGED);
	}

	@PostMapping("/{id}/delete")
	String delete(@PathVariable Long id, Model model, HttpServletResponse response) {
		Optional<Watch> found = find(id);
		if (found.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.watch.missing")), WATCHES_CHANGED);
		}
		Watch watch = found.get();
		watch.setDeletedAt(Instant.now());
		watches.save(watch);
		var toast = Toast.undoable(ui.msg("toast.watch.deleted", watch.getProduct()), "/ui/watches/" + id + "/restore");
		return ui.toast(model, response, toast, WATCHES_CHANGED, "watchDeleted");
	}

	@PostMapping("/{id}/restore")
	String restore(@PathVariable Long id, Model model, HttpServletResponse response) {
		Optional<Watch> found = watches.findById(id);
		if (found.isEmpty()) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.undo.expired")));
		}
		Watch watch = found.get();
		watch.setDeletedAt(null);
		watches.save(watch);
		return ui.toast(model, response, Toast.success(ui.msg("toast.watch.restored", watch.getProduct())), WATCHES_CHANGED);
	}

	private String form(Model model, Long id, WatchForm form, Map<String, String> errors) {
		model.addAttribute("form", form);
		model.addAttribute("watchId", id);
		model.addAttribute("errors", errors);
		model.addAttribute("channels", notifications.status());
		return "fragments/watch-form :: form";
	}

	private Optional<Watch> find(Long id) {
		return watches.findById(id).filter(w -> w.getDeletedAt() == null);
	}
}
