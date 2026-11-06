package dev.gamba.promofinder.ui;

/** A message shown in the corner of the page. {@code undoUrl} adds an "Undo" button. */
public record Toast(String type, String message, String undoUrl) {

	static Toast success(String message) {
		return new Toast("success", message, null);
	}

	static Toast info(String message) {
		return new Toast("info", message, null);
	}

	static Toast error(String message) {
		return new Toast("error", message, null);
	}

	static Toast undoable(String message, String undoUrl) {
		return new Toast("success", message, undoUrl);
	}
}
