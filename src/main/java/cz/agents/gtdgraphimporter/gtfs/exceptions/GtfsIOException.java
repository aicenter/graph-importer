package cz.agents.gtdgraphimporter.gtfs.exceptions;

/**
 * An exception thrown during a GTFS input or output operation.
 * 
 * @author Radek Holy
 */
public class GtfsIOException extends GtfsException {

	/**
	 * Class version number.
	 * 
	 * If the class changes follow these <a href=
	 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678"
	 * >guidelines</a>.
	 */
	private static final long serialVersionUID = -8365269838068878359L;

	/**
	 * Construct an instance.
	 */
	public GtfsIOException() {
		super();
	}

	/**
	 * Construct an instance.
	 * 
	 * @param message
	 *            A detail message.
	 */
	public GtfsIOException(final String message) {
		super(message);
	}

	/**
	 * Construct an instance.
	 * 
	 * @param cause
	 *            A cause.
	 */
	public GtfsIOException(final Throwable cause) {
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
	public GtfsIOException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
