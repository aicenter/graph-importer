package cz.agents.gtdgraphimporter.gtfs.impl;

import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.gtfs.GTFSDatabaseLoader;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsParseException;

import java.sql.Connection;
import java.sql.Date;

/**
 * This class provides loading of GTFS data from a database while failing in
 * case of invalid data.
 *
 * @author Radek Holy
 */
public final class GTFSDatabaseLoaderStrictImpl extends GTFSDatabaseLoader {

	/**
	 * Construct a new instance.
	 *
	 * @param connection
	 *            Connection to a database.
	 * @param epsgSrid
	 *            EPSG SRID of coordinates projection.
	 * @param gtfsUnitToMetersMultiplier
	 *            A number used to multiply traveled distance specified in GTFS
	 *            data to convert it to meters.
	 * @param sqlResultDownloadSize
	 *            Max. numberOfNodes (number of rows) of one batch locally stored
	 *            (cached) while downloading SQL results.
	 * @param pruneBeforeDate
	 *            Min. allowed loaded date (inclusive). This setting should be
	 *            used rarely to accelerate GTFS loading time. In other cases, a
	 *            GTFS graph filtering mechanism should be used.
	 * @param pruneAfterDate
	 *            Max. allowed loaded date (exclusive). This setting should be
	 *            used rarely to accelerate GTFS loading time. In other cases, a
	 *            GTFS graph filtering mechanism should be used.
	 */
	public GTFSDatabaseLoaderStrictImpl(final Connection connection, final int epsgSrid,
			final double gtfsUnitToMetersMultiplier, final int sqlResultDownloadSize, final Date pruneBeforeDate,
			final Date pruneAfterDate) {
		super(connection, epsgSrid, gtfsUnitToMetersMultiplier, sqlResultDownloadSize, pruneBeforeDate, pruneAfterDate);
	}


	/**
	 * Construct a new instance.
	 *
	 * @param connection
	 *            Connection to a database.
	 * @param epsgProjection
	 *            Coordinate projection.
	 * @param gtfsUnitToMetersMultiplier
	 *            A number used to multiply traveled distance specified in GTFS
	 *            data to convert it to meters.
	 * @param sqlResultDownloadSize
	 *            Max. numberOfNodes (number of rows) of one batch locally stored
	 *            (cached) while downloading SQL results.
	 * @param pruneBeforeDate
	 *            Min. allowed loaded date (inclusive). This setting should be
	 *            used rarely to accelerate GTFS loading time. In other cases, a
	 *            GTFS graph filtering mechanism should be used.
	 * @param pruneAfterDate
	 *            Max. allowed loaded date (exclusive). This setting should be
	 *            used rarely to accelerate GTFS loading time. In other cases, a
	 *            GTFS graph filtering mechanism should be used.
	 */
	public GTFSDatabaseLoaderStrictImpl(final Connection connection, final Transformer epsgProjection,
			final double gtfsUnitToMetersMultiplier, final int sqlResultDownloadSize, final Date pruneBeforeDate,
			final Date pruneAfterDate) {
		super(connection, epsgProjection, gtfsUnitToMetersMultiplier, sqlResultDownloadSize, pruneBeforeDate, pruneAfterDate);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void handleParseException(final String message, final Throwable exception)
			throws GtfsParseException {
		throw new GtfsParseException(message, exception);
	}

}
