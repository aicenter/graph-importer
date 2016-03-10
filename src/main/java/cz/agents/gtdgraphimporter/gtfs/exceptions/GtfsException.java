package cz.agents.gtdgraphimporter.gtfs.exceptions;

/**
 * Base GTFS exception.
 * 
 * @author Radek Holy
 */
public class GtfsException extends Exception {

	/**
	 * Class version number.
	 * 
	 * If the class changes follow these <a href=
	 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678"
	 * >guidelines</a>.
	 */
	private static final long serialVersionUID = -5935451468855533998L;

	/**
	 * Construct an instance.
	 */
	public GtfsException() {
		super();
	}

	/**
	 * Construct an instance.
	 * 
	 * @param message
	 *            A detail message.
	 */
	public GtfsException(final String message) {
		super(message);
	}

	/**
	 * Construct an instance.
	 * 
	 * @param cause
	 *            A cause.
	 */
	public GtfsException(final Throwable cause) {
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
	public GtfsException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
