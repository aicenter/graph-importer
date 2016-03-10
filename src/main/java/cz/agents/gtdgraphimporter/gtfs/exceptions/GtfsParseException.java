package cz.agents.gtdgraphimporter.gtfs.exceptions;

/**
 * An exception thrown during parsing GTFS data.
 * 
 * @author Radek Holy
 */
public final class GtfsParseException extends GtfsException {

	/**
	 * Class version number.
	 * 
	 * If the class changes follow these <a href=
	 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678"
	 * >guidelines</a>.
	 */
	private static final long serialVersionUID = -8180152665692947340L;

	/**
	 * Construct an instance.
	 */
	public GtfsParseException() {
		super();
	}

	/**
	 * Construct an instance.
	 * 
	 * @param message
	 *            A detail message.
	 */
	public GtfsParseException(final String message) {
		super(message);
	}

	/**
	 * Construct an instance.
	 * 
	 * @param cause
	 *            A cause.
	 */
	public GtfsParseException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Construct an instance.
	 * 
	 * @param message
	 *            A detail message.
	 * @param cause
	 *            A cause.
	 */
	public GtfsParseException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
