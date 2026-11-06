package dev.gamba.promofinder.ai;

import java.io.IOException;
import java.util.Locale;

public class ClaudeException extends IOException {

	private final CheckError error;

	public ClaudeException(CheckError error, String detail) {
		super(detail);
		this.error = error;
	}

	public ClaudeException(CheckError error, String detail, Throwable cause) {
		super(detail, cause);
		this.error = error;
	}

	public CheckError error() {
		return error;
	}

	/** Guesses the cause from what the CLI printed. */
	static ClaudeException fromCliOutput(String output) {
		String text = output.toLowerCase(Locale.ROOT);
		if (text.contains("not logged in") || text.contains("/login") || text.contains("please log in")
				|| text.contains("invalid api key") || text.contains("oauth") || text.contains("authentication")) {
			return new ClaudeException(CheckError.CLAUDE_NOT_LOGGED_IN, output);
		}
		if (text.contains("usage limit") || text.contains("limit reached") || text.contains("rate limit")
				|| text.contains("budget")) {
			return new ClaudeException(CheckError.CLAUDE_LIMIT_REACHED, output);
		}
		return new ClaudeException(CheckError.UNKNOWN, output);
	}
}
