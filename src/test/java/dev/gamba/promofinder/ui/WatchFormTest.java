package dev.gamba.promofinder.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WatchFormTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"R$ 4.500,00 | 4500.00",
			"4.500,5     | 4500.50",
			"4500        | 4500.00",
			"4500.50     | 4500.50",
			"4.500       | 4500.00",
			"1.234.567,89| 1234567.89" })
	void parsesBrazilianAndPlainPrices(String input, String expected) {
		var form = new WatchForm();
		form.setMaxPrice(input);
		assertThat(form.parsedPrice()).isEqualByComparingTo(new BigDecimal(expected));
	}

	@ParameterizedTest
	@CsvSource({ "''", "abc", "R$" })
	void rejectsMissingPrice(String input) {
		var form = new WatchForm();
		form.setProduct("Phone");
		form.setMaxPrice(input);
		assertThat(form.validate()).containsKey("maxPrice");
	}
}
