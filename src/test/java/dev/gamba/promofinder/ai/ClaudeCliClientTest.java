package dev.gamba.promofinder.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import dev.gamba.promofinder.config.PromoProperties;
import tools.jackson.databind.json.JsonMapper;

class ClaudeCliClientTest {

	private final ClaudeCliClient client = new ClaudeCliClient(
			new PromoProperties(new PromoProperties.Claude("claude", "sonnet", Duration.ofMinutes(1), "x"), null),
			JsonMapper.builder().build());

	@Test
	void parsesCliEnvelopeWithFencedJson() throws IOException {
		String answer = "Here you go:\\n```json\\n{\\\"offers\\\":[{\\\"store\\\":\\\"KaBuM\\\",\\\"title\\\":\\\"S26 Ultra\\\","
				+ "\\\"price\\\":4399.9,\\\"currency\\\":\\\"BRL\\\",\\\"url\\\":\\\"https://kabum.com.br/p/1\\\","
				+ "\\\"inStock\\\":true,\\\"condition\\\":\\\"new\\\"}],\\\"notes\\\":\\\"ok\\\"}\\n```";
		String envelope = "{\"type\":\"result\",\"is_error\":false,\"total_cost_usd\":0.12,\"result\":\"" + answer + "\"}";

		SearchResult result = client.parse(envelope);

		assertThat(result.offers()).hasSize(1);
		assertThat(result.offers().getFirst().price()).isEqualByComparingTo(new BigDecimal("4399.90"));
		assertThat(result.offers().getFirst().url()).isEqualTo("https://kabum.com.br/p/1");
		assertThat(result.notes()).isEqualTo("ok");
		assertThat(result.costUsd()).isEqualTo(0.12);
	}

	@Test
	void failsWhenCliReportsError() {
		assertThatThrownBy(() -> client.parse("{\"is_error\":true,\"result\":\"not logged in\"}"))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("not logged in");
	}

	@Test
	void prefersStructuredOutput() throws IOException {
		String envelope = "{\"is_error\":false,\"total_cost_usd\":0.05,\"result\":\"Here is what I found.\","
				+ "\"structured_output\":{\"offers\":[{\"store\":\"Amazon BR\",\"title\":\"S26\",\"price\":6649.05,"
				+ "\"url\":\"https://www.amazon.com.br/dp/1\"}],\"notes\":\"one offer\"}}";

		SearchResult result = client.parse(envelope);

		assertThat(result.offers()).singleElement()
				.satisfies(o -> assertThat(o.price()).isEqualByComparingTo("6649.05"));
		assertThat(result.notes()).isEqualTo("one offer");
		assertThat(result.costUsd()).isEqualTo(0.05);
	}

	@Test
	void proseAnswerFailsButKeepsCost() {
		String envelope = "{\"is_error\":false,\"total_cost_usd\":0.31,"
				+ "\"result\":\"All current market prices are above your target.\"}";

		assertThatThrownBy(() -> client.parse(envelope))
				.isInstanceOfSatisfying(ClaudeException.class, e -> {
					assertThat(e.error()).isEqualTo(CheckError.CLAUDE_BAD_ANSWER);
					assertThat(e.costUsd()).isEqualTo(0.31);
				});
	}

	@Test
	void cliErrorKeepsCost() {
		assertThatThrownBy(() -> client.parse("{\"is_error\":true,\"total_cost_usd\":0.12,\"result\":\"usage limit reached\"}"))
				.isInstanceOfSatisfying(ClaudeException.class, e -> {
					assertThat(e.error()).isEqualTo(CheckError.CLAUDE_LIMIT_REACHED);
					assertThat(e.costUsd()).isEqualTo(0.12);
				});
	}

	@Test
	void answerSchemaIsValidJsonWithoutWhitespace() {
		assertThat(ClaudeCliClient.ANSWER_SCHEMA).doesNotContainPattern("\\s");
		assertThat(JsonMapper.builder().build().readTree(ClaudeCliClient.ANSWER_SCHEMA).path("required").toString())
				.isEqualTo("[\"offers\",\"notes\"]");
	}
}
