package dev.gamba.promofinder.ui;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.gamba.promofinder.config.PromoProperties;
import dev.gamba.promofinder.domain.CheckRunRepository;
import dev.gamba.promofinder.domain.OfferRepository;
import dev.gamba.promofinder.domain.PricePoint;
import dev.gamba.promofinder.domain.PricePointRepository;
import dev.gamba.promofinder.domain.Watch;
import dev.gamba.promofinder.domain.WatchRepository;
import dev.gamba.promofinder.notify.NotificationService;

@Controller
class PageController {

	private final WatchRepository watches;
	private final OfferRepository offers;
	private final CheckRunRepository runs;
	private final PricePointRepository pricePoints;
	private final NotificationService notifications;
	private final WatchViews views;
	private final StoreUiController storeUi;
	private final PromoProperties props;
	private final UiEvents events;
	private final Ui ui;

	PageController(WatchRepository watches, OfferRepository offers, CheckRunRepository runs,
			PricePointRepository pricePoints, NotificationService notifications, WatchViews views,
			StoreUiController storeUi, PromoProperties props, UiEvents events, Ui ui) {
		this.watches = watches;
		this.offers = offers;
		this.runs = runs;
		this.pricePoints = pricePoints;
		this.notifications = notifications;
		this.views = views;
		this.storeUi = storeUi;
		this.props = props;
		this.events = events;
		this.ui = ui;
	}

	@GetMapping("/")
	String dashboard(Model model) {
		model.addAttribute("page", "watches");
		model.addAttribute("views", views.of(watches.findByDeletedAtIsNullOrderByCreatedAtDesc()));
		model.addAttribute("summary", ui.summary());
		return "index";
	}

	@GetMapping("/watches/{id}")
	String watch(@PathVariable Long id, Model model) {
		Watch watch = watches.findById(id).filter(w -> w.getDeletedAt() == null)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		List<PricePoint> latestPrices = runs.findFirstByWatchIdAndErrorCodeIsNullOrderByStartedAtDesc(id)
				.map(r -> pricePoints.findByCheckRunIdOrderByPrice(r.getId()))
				.orElse(List.of());
		model.addAttribute("page", "watches");
		model.addAttribute("v", views.of(watch));
		model.addAttribute("onDetail", true);
		model.addAttribute("chart", Chart.of(views.history(watch, Duration.ofDays(30)), watch.getMaxPrice(), 640, 220));
		model.addAttribute("latestPrices", latestPrices.stream().sorted(Comparator.comparing(PricePoint::getPrice)).toList());
		model.addAttribute("deals", offers.findTop50ByWatchIdOrderByFoundAtDesc(id));
		model.addAttribute("runs", runs.findTop20ByWatchIdOrderByStartedAtDesc(id));
		return "watch";
	}

	@GetMapping("/stores")
	String stores(Model model) {
		model.addAttribute("page", "stores");
		storeUi.addListModel(model);
		return "stores";
	}

	@GetMapping("/settings")
	String settings(Model model) {
		model.addAttribute("page", "settings");
		model.addAttribute("channels", notifications.status());
		model.addAttribute("claude", props.claude());
		return "settings";
	}

	@GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter events() {
		return events.subscribe();
	}
}
