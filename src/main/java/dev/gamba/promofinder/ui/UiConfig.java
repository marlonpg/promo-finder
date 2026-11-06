package dev.gamba.promofinder.ui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/** Language comes from the browser, and "?lang=en" / "?lang=pt-BR" switches it (remembered in a cookie). */
@Configuration
class UiConfig implements WebMvcConfigurer {

	@Bean
	LocaleResolver localeResolver() {
		return new CookieLocaleResolver("lang");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		var interceptor = new LocaleChangeInterceptor();
		interceptor.setParamName("lang");
		interceptor.setIgnoreInvalidLocale(true);
		registry.addInterceptor(interceptor);
	}
}
