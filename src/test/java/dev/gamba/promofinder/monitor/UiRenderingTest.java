package dev.gamba.promofinder.monitor;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import dev.gamba.promofinder.domain.CheckRun;
import dev.gamba.promofinder.domain.CheckRunRepository;
import dev.gamba.promofinder.domain.Watch;
import dev.gamba.promofinder.domain.WatchRepository;

/** Renders every page and the watch card in each state, in both languages, to catch template errors. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:ui",
		"promo.scheduler.tick=1h",
		"promo.claude.command=claude-does-not-exist" })
@AutoConfigureMockMvc
class UiRenderingTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	WatchRepository watches;

	@Autowired
	CheckRunRepository runs;

	@Autowired
	CheckTracker tracker;

	Watch watch;

	@BeforeEach
	void setUp() {
		watch = new Watch();
		watch.setProduct("Galaxy S26 Ultra");
		watch.setMaxPrice(new BigDecimal("4500.00"));
		watch = watches.save(watch);
	}

	@ParameterizedTest
	@ValueSource(strings = { "en", "pt-BR" })
	void pagesRender(String lang) throws Exception {
		for (String path : new String[] { "/", "/stores", "/settings", "/watches/" + watch.getId(),
				"/ui/watches", "/ui/watches/summary", "/ui/watches/new", "/ui/watches/" + watch.getId() + "/edit",
				"/ui/stores" }) {
			mvc.perform(get(path).param("lang", lang)).andExpect(status().isOk());
		}
	}

	@Test
	void cardRendersWhileSearching() throws Exception {
		tracker.queue(watch.getId());
		mvc.perform(get("/ui/watches/{id}/card", watch.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("badge-queued")));
		tracker.start(watch.getId());
		try {
			mvc.perform(get("/ui/watches/{id}/card", watch.getId()))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("data-elapsed")));
		}
		finally {
			tracker.finish(watch.getId());
		}
	}

	@Test
	void cardAndDetailRenderDealWithHistory() throws Exception {
		Instant start = Instant.now().minus(3, ChronoUnit.HOURS);
		for (int i = 0; i < 3; i++) {
			var run = new CheckRun(watch.getId(), start.plus(i, ChronoUnit.HOURS));
			run.setFinishedAt(run.getStartedAt().plusSeconds(90));
			run.setBestPrice(new BigDecimal(4700 - i * 150));
			run.setBestStore("KaBuM!");
			runs.save(run);
		}
		watch.setLastCheckedAt(Instant.now());
		watch.setLastBestPrice(new BigDecimal("4400.00"));
		watch.setLastBestStore("KaBuM!");
		watch.setLastBestUrl("https://www.kabum.com.br/p/1");
		watches.save(watch);

		mvc.perform(get("/ui/watches/{id}/card", watch.getId()).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Deal found")))
				.andExpect(content().string(containsString("spark-line")));
		mvc.perform(get("/watches/{id}", watch.getId()).param("lang", "pt-BR"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("chart-line")))
				.andExpect(content().string(containsString("Oferta encontrada")));
	}

	@Test
	void cardRendersError() throws Exception {
		watch.setLastCheckedAt(Instant.now());
		watch.setLastErrorCode("CLAUDE_NOT_LOGGED_IN");
		watch.setLastStatus("not logged in");
		watches.save(watch);
		mvc.perform(get("/ui/watches/{id}/card", watch.getId()).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Claude Code is not logged in")));
	}

	@Test
	void invalidFormComesBackWithErrors() throws Exception {
		mvc.perform(post("/ui/watches").param("product", "").param("maxPrice", "").param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tell us which product to look for.")));
	}

	@Test
	void deleteOffersUndo() throws Exception {
		mvc.perform(post("/ui/watches/{id}/delete", watch.getId()).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(header().string("HX-Trigger", containsString("watchesChanged")))
				.andExpect(content().string(containsString("/restore")));
		mvc.perform(post("/ui/watches/{id}/restore", watch.getId())).andExpect(status().isOk());
	}
}
