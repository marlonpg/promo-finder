package dev.gamba.promofinder.ai;

/** Why a check failed, in terms the UI can explain to the user (see messages: error.*). */
public enum CheckError {
	CLAUDE_NOT_FOUND, CLAUDE_NOT_LOGGED_IN, CLAUDE_LIMIT_REACHED, CLAUDE_TIMEOUT, CLAUDE_BAD_ANSWER, UNKNOWN
}
