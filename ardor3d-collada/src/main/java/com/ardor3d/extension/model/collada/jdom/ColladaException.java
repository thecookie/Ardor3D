package com.ardor3d.extension.model.collada.jdom;

public class ColladaException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ColladaException(final String message, final Object source) {
		super(ColladaException.createMessage(message, source));
	}

	public ColladaException(final String msg, final Object source, final Throwable cause) {
		super(ColladaException.createMessage(msg, source), cause);
	}

	private static String createMessage(final String message, final Object source) {
		return "Collada problem for source: " + source.toString() + ": " + message;
	}
}
