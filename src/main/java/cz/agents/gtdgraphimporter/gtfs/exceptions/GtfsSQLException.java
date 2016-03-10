package cz.agents.gtdgraphimporter.gtfs.exceptions;

import java.sql.SQLException;

/**
 * An exception thrown during a database input or output GTFS operation.
 * 
 * @author Radek Holy
 */
public final class GtfsSQLException extends GtfsIOException {

	/**
	 * Class version number.
	 * 
	 * If the class changes follow these <a href=
	 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678"
	 * >guidelines</a>.
	 */
	private static final long serialVersionUID = -713480003776554993L;

	/**
	 * Construct an instance.
	 * 
	 * This method is deprecated because it is expected that an instance
	 * contains an SQLException cause.
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private GtfsSQLException() {
		throw new UnsupportedOperationException(
				"This method is not supported because it is expected that an instance contains an SQLException cause.");
	}

	/**
	 * Construct an instance.
	 * 
	 * This method is deprecated because it is expected that an instance
	 * contains an SQLException cause.
	 * 
	 * @param message
	 *            A detail message.
	 */
	@SuppressWarnings("unused")
	@Deprecated
	public GtfsSQLException(final String message) {
		throw new UnsupportedOperationException(
				"This method is not supported because it is expected that an instance contains an SQLException cause.");
	}

	/**
	 * Construct an instance.
	 * 
	 * @param cause
	 *            A cause.
	 */
	public GtfsSQLException(final SQLException cause) {
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
	public GtfsSQLException(final String message, final SQLException cause) {
		super(message, cause);
	}
}
