package dev.gamba.promofinder.ui;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.gamba.promofinder.ai.CheckError;
import dev.gamba.promofinder.ai.ClaudeCliClient;
import dev.gamba.promofinder.ai.ClaudeException;
import dev.gamba.promofinder.domain.NotificationChannel;
import dev.gamba.promofinder.notify.NotificationService;

@Controller
@RequestMapping("/ui/settings")
class SettingsUiController {

	private final NotificationService notifications;
	private final ClaudeCliClient claude;
	private final Ui ui;

	SettingsUiController(NotificationService notifications, ClaudeCliClient claude, Ui ui) {
		this.notifications = notifications;
		this.claude = claude;
		this.ui = ui;
	}

	@PostMapping("/test/{channel}")
	String test(@PathVariable NotificationChannel channel, Model model, HttpServletResponse response) {
		String name = ui.msg("channel." + channel);
		if (!notifications.isConfigured(channel)) {
			return ui.toast(model, response, Toast.error(ui.msg("toast.test.notConfigured", name)));
		}
		boolean sent = !notifications.send(List.of(channel), ui.msg("test.subject"), ui.msg("test.body", name)).isEmpty();
		Toast toast = sent ? Toast.success(ui.msg("toast.test.sent", name)) : Toast.error(ui.msg("toast.test.failed", name));
		return ui.toast(model, response, toast);
	}

	@PostMapping("/claude")
	String testClaude(Model model, HttpServletResponse response) {
		try {
			return ui.toast(model, response, Toast.success(ui.msg("toast.claude.ok", claude.version())));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ui.toast(model, response, Toast.error(ui.msg("error.UNKNOWN.title")));
		}
		catch (Exception e) {
			CheckError error = e instanceof ClaudeException ce ? ce.error() : CheckError.UNKNOWN;
			return ui.toast(model, response,
					Toast.error(ui.msg("error." + error + ".title") + " " + ui.msg("error." + error + ".hint")));
		}
	}
}
