package cz.agents.gtdgraphimporter.gtfs;

import com.google.common.collect.ImmutableMap;
import cz.agents.basestructures.GPSLocation;
import cz.agents.geotools.EPSGProjection;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsParseException;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsSQLException;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.additional.WheelchairBoarding;
import org.joda.time.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * This class provides loading of GTFS data from a database.
 *
 * @author Radek Holy
 */
public abstract class GTFSDatabaseLoader implements GTFSDataLoader {

	/**
	 * A mapping from GTFS-specific route type constants to {@link ModeOfTransport} enumeration.
	 * <p>
	 * The type number 8 is not specified in GTFS, but it is used in some data.
	 * <p>
	 * TODO Handle extended set of route types, e.g. 109 in Helsinki https://support.google
	 * .com/transitpartners/answer/3520902?hl=en
	 * <p>
	 * TODO Handle them also in GraphConstraintPtModesImpl
	 */
	public static final ImmutableMap<Integer, ModeOfTransport> GTFS_ROUTE_TYPE_TO_MODE_ENUM;

	static {
		ModeOfTransport[] modes = {
				ModeOfTransport.TRAM,
				ModeOfTransport.UNDERGROUND,
				ModeOfTransport.TRAIN,
				ModeOfTransport.BUS,
				ModeOfTransport.FERRY,
				ModeOfTransport.OTHER,
				ModeOfTransport.OTHER,
				ModeOfTransport.OTHER,
				ModeOfTransport.OTHER};

		ImmutableMap.Builder<Integer, ModeOfTransport> builder = ImmutableMap.builder();
		for (int i = 0; i < modes.length; ++i) {
			builder.put(i, modes[i]);
		}
		builder.put(100, ModeOfTransport.TRAIN);
		builder.put(109, ModeOfTransport.TRAIN);
		builder.put(700, ModeOfTransport.BUS);
		builder.put(800, ModeOfTransport.TROLLEYBUS);
		builder.put(900, ModeOfTransport.TRAM);
		builder.put(1000, ModeOfTransport.FERRY);

		GTFS_ROUTE_TYPE_TO_MODE_ENUM = builder.build();
	}

	/**
	 * Connection to the database.
	 */
	private final Connection connection;

	/**
	 * EPSG projector of coordinates.
	 */
	private final EPSGProjection epsgProjection;

	/**
	 * A number used to multiply traveled distance specified in GTFS data to convert it to meters.
	 */
	private final double gtfsUnitToMetersMultiplier;

	/**
	 * Max. numberOfNodes (number of rows) of one batch locally stored (cached) while downloading SQL results.
	 */
	private final int sqlResultDownloadSize;

	/**
	 * Min. allowed loaded date (inclusive).
	 */
	private final Date pruneBeforeDate;

	/**
	 * Max. allowed loaded date (exclusive).
	 */
	private final Date pruneAfterDate;

	/**
	 * Construct a new instance.
	 *
	 * @param connection
	 * 		Connection to a database.
	 * @param projection
	 * 		Coordinate projection.
	 * @param gtfsUnitToMetersMultiplier
	 * 		A number used to multiply traveled distance specified in GTFS data to convert it to meters.
	 * @param sqlResultDownloadSize
	 * 		Max. numberOfNodes (number of rows) of one batch locally stored (cached) while downloading SQL results.
	 * @param pruneBeforeDate
	 * 		Min. allowed loaded date (inclusive). This setting should be used rarely to accelerate GTFS loading time
	 * 		. In
	 * 		other cases, a GTFS graph filtering mechanism should be used.
	 * @param pruneAfterDate
	 * 		Max. allowed loaded date (exclusive). This setting should be used rarely to accelerate GTFS loading time
	 * 		. In
	 * 		other cases, a GTFS graph filtering mechanism should be used.
	 */
	public GTFSDatabaseLoader(final Connection connection, final EPSGProjection projection,
							  final double gtfsUnitToMetersMultiplier, final int sqlResultDownloadSize,
							  final Date pruneBeforeDate, final Date pruneAfterDate) {
		super();

		if (connection == null) {
			throw new NullPointerException("bad database connection");
		}
		if (sqlResultDownloadSize < 0) {
			throw new IllegalArgumentException("bad download numberOfNodes");
		}
		if (pruneBeforeDate == null) {
			throw new NullPointerException("bad min. date");
		}
		if (pruneAfterDate == null) {
			throw new NullPointerException("bad max. date");
		}
		if (pruneBeforeDate.after(pruneAfterDate)) {
			throw new IllegalArgumentException("min. date must be before max. date");
		}

		this.connection = connection;
		this.epsgProjection = projection;
		this.gtfsUnitToMetersMultiplier = gtfsUnitToMetersMultiplier;
		this.sqlResultDownloadSize = sqlResultDownloadSize;
		this.pruneBeforeDate = pruneBeforeDate;
		this.pruneAfterDate = pruneAfterDate;
	}

	/**
	 * Construct a new instance.
	 *
	 * @param connection
	 * 		Connection to a database.
	 * @param epsgSrid
	 * 		EPSG SRID of coordinates projection.
	 * @param gtfsUnitToMetersMultiplier
	 * 		A number used to multiply traveled distance specified in GTFS data to convert it to meters.
	 * @param sqlResultDownloadSize
	 * 		Max. numberOfNodes (number of rows) of one batch locally stored (cached) while downloading SQL results.
	 * @param pruneBeforeDate
	 * 		Min. allowed loaded date (inclusive). This setting should be used rarely to accelerate GTFS loading time
	 * 		. In
	 * 		other cases, a GTFS graph filtering mechanism should be used.
	 * @param pruneAfterDate
	 * 		Max. allowed loaded date (exclusive). This setting should be used rarely to accelerate GTFS loading time
	 * 		. In
	 * 		other cases, a GTFS graph filtering mechanism should be used.
	 */
	public GTFSDatabaseLoader(final Connection connection, final int epsgSrid, final double gtfsUnitToMetersMultiplier,
							  final int sqlResultDownloadSize, final Date pruneBeforeDate, final Date pruneAfterDate) {
		this(connection, createProjection(epsgSrid), gtfsUnitToMetersMultiplier, sqlResultDownloadSize, pruneBeforeDate,
				pruneAfterDate);

	}



	/**
	 * Handle potential exception thrown when parsing the data.
	 *
	 * @param message
	 * 		A message.
	 * @param exception
	 * 		An exception.
	 *
	 * @throws GtfsParseException
	 * 		An exception thrown if it is necessary.
	 */
	protected abstract void handleParseException(final String message,
												 final Throwable exception) throws GtfsParseException;

	/**
	 * Handle potential exception thrown when using the connection.
	 *
	 * @param message
	 * 		A message.
	 * @param exception
	 * 		An exception.
	 *
	 * @throws GtfsSQLException
	 * 		An exception thrown if it is necessary.
	 */
	protected final void handleSqlException(final String message,
											final SQLException exception) throws GtfsSQLException {
		throw new GtfsSQLException(message, exception);
	}

	/**
	 * Load public transport agencies information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsSQLException
	 * 		If the database connection fails.
	 * @throws GtfsParseException
	 * 		If the data contains a mistake.
	 */
	public void loadAgencies(final GTFSDataHandler dataHandler) throws GtfsSQLException, GtfsParseException {
		if (dataHandler == null) {
			throw new NullPointerException("bad handler");
		}

		final String sqlQuery = String.format("SELECT DISTINCT %s, %s, %s, %s, %s FROM %s;",
				SCHEMA_AGENCY_TABLE_ID_COLUMN, SCHEMA_AGENCY_TABLE_NAME_COLUMN, SCHEMA_AGENCY_TABLE_TIMEZONE_COLUMN,
				SCHEMA_AGENCY_TABLE_URL_COLUMN, SCHEMA_AGENCY_TABLE_PHONE_COLUMN, SCHEMA_AGENCY_TABLE);
		// logger.debug(sqlQuery);

		try {
			connection.setAutoCommit(false); // Needed for "setFetchSize".
			final Statement sqlStatement = connection.createStatement();
			sqlStatement.setFetchSize(sqlResultDownloadSize);
			final ResultSet sqlResult = sqlStatement.executeQuery(sqlQuery);
			while (sqlResult.next()) {
				final String id = sqlResult.getString(1);
				final String name = sqlResult.getString(2);
				final String timeZoneId = sqlResult.getString(3);
				final String agencyUrl = sqlResult.getString(4);
				final String agencyPhone = sqlResult.getString(5);

				// Check all required values are provided.
				if (timeZoneId == null) {
					handleParseException("not all necessary information about agency set: ".concat(id), null);
					continue;
				}

				// Convert data to acceptable data types.
				final DateTimeZone timeZone;
				try {
					timeZone = DateTimeZone.forID(timeZoneId);
				} catch (final IllegalArgumentException e) {
					handleParseException("unknown time zone ID: ".concat(timeZoneId), e);
					continue;
				}

				try {
					dataHandler.addAgency(id, name, timeZone, agencyUrl, agencyPhone);
				} catch (final IllegalArgumentException | IllegalStateException e) {
					handleParseException("not all necessary information about agency already set: ".concat(id), e);
				}
			}
			sqlResult.close();
			sqlStatement.close();
			connection.setAutoCommit(true);
		} catch (final SQLException e) {
			handleSqlException("connection failed during agencies loading", e);
		}
	}

	/**
	 * Load public transport stops information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsSQLException
	 * 		If the database connection fails.
	 * @throws GtfsParseException
	 * 		If the data contains a mistake.
	 */
	public void loadStops(final GTFSDataHandler dataHandler) throws GtfsSQLException, GtfsParseException {
		if (dataHandler == null) {
			throw new NullPointerException("bad handler");
		}

		final String sqlQuery = String.format("SELECT DISTINCT %s, %s, %s, %s, %s, %s, %s, %s FROM %s;",
				SCHEMA_STOPS_TABLE_ID_COLUMN, SCHEMA_STOPS_TABLE_LATITUDE_COLUMN, SCHEMA_STOPS_TABLE_LONGITUDE_COLUMN,
				SCHEMA_STOPS_TABLE_DESCRIPTION_COLUMN, SCHEMA_STOPS_TABLE_ZONE_ID_COLUMN,
				SCHEMA_STOPS_TABLE_CODE_COLUMN, SCHEMA_STOPS_TABLE_NAME_COLUMN,
				SCHEMA_STOPS_TABLE_WHEELCHAIR_BOARDING_COLUMN, SCHEMA_STOPS_TABLE);

		try {
			connection.setAutoCommit(false); // Needed for "setFetchSize".
			final Statement sqlStatement = connection.createStatement();
			sqlStatement.setFetchSize(sqlResultDownloadSize);
			final ResultSet sqlResult = sqlStatement.executeQuery(sqlQuery);
			while (sqlResult.next()) {
				final String id = sqlResult.getString(1);
				final Double latitude = getDouble(sqlResult, 2);
				final Double longitude = getDouble(sqlResult, 3);
				final String description = sqlResult.getString(4);
				final String zone = sqlResult.getString(5);
				final String code = sqlResult.getString(6);
				final String name = sqlResult.getString(7);
				// null is intentionally translated to 0
				final int wheelchairBoardingIndex = sqlResult.getInt(8);

				// Check all required values are provided.
				if (id == null || latitude == null || longitude == null || name == null) {
					handleParseException("not all necessary information about stop set: ".concat(id), null);
					continue;
				}
				if (wheelchairBoardingIndex < 0 || wheelchairBoardingIndex >= WheelchairBoarding.values().length) {
					handleParseException(String.format("unknown wheelchair boarding type: %d",
							wheelchairBoardingIndex),
							null);
					continue;
				}

				GPSLocation location;
				if (epsgProjection != null) {
					location = epsgProjection.getProjectedGPSLocation((int) (latitude * 1E6), (int) (longitude * 1E6),
							0);
				} else {
					location = new GPSLocation(latitude, longitude, 0, 0);
				}
				final WheelchairBoarding wheelchairBoarding = WheelchairBoarding.values()[wheelchairBoardingIndex];

				try {
					dataHandler.addStop(id, code, name, description, location, zone, wheelchairBoarding);
				} catch (final IllegalArgumentException | IllegalStateException e) {
					handleParseException("not all necessary information about stop already set: ".concat(id), e);
				}
			}
			sqlResult.close();
			sqlStatement.close();
			connection.setAutoCommit(true);
		} catch (final SQLException e) {
			handleSqlException("connection failed during stops loading", e);
		}
	}

	/**
	 * Load public transport routes information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsSQLException
	 * 		If the database connection fails.
	 * @throws GtfsParseException
	 * 		If the data contains a mistake.
	 */
	public void loadRoutes(final GTFSDataHandler dataHandler) throws GtfsSQLException, GtfsParseException {
		if (dataHandler == null) {
			throw new NullPointerException("bad handler");
		}

		final String sqlQuery = String.format("SELECT DISTINCT %s, %s, %s, %s, %s, %s FROM %s;",
				SCHEMA_ROUTES_TABLE_ID_COLUMN, SCHEMA_ROUTES_TABLE_AGENCY_ID_COLUMN, SCHEMA_ROUTES_TABLE_TYPE_COLUMN,
				SCHEMA_ROUTES_TABLE_SHORT_NAME_COLUMN, SCHEMA_ROUTES_TABLE_LONG_NAME_COLUMN,
				SCHEMA_ROUTES_TABLE_DESCRIPTION_COLUMN, SCHEMA_ROUTES_TABLE);

		try {
			connection.setAutoCommit(false); // Needed for "setFetchSize".
			final Statement sqlStatement = connection.createStatement();
			sqlStatement.setFetchSize(sqlResultDownloadSize);
			final ResultSet sqlResult = sqlStatement.executeQuery(sqlQuery);
			while (sqlResult.next()) {
				// Fetch the result.
				final String id = sqlResult.getString(1);
				final String agency = sqlResult.getString(2);
				final Integer type = getInteger(sqlResult, 3);
				final String shortName = sqlResult.getString(4);
				final String longName = sqlResult.getString(5);
				final String description = sqlResult.getString(6);

				// Check all required values are provided.
				if (id == null || type == null) {
					handleParseException("not all necessary information about route set: ".concat(id), null);
					continue;
				}
				if (!GTFS_ROUTE_TYPE_TO_MODE_ENUM.containsKey(type)) {
					handleParseException(String.format("unknown route type: %d", type), null);
					continue;
				}

				// Convert data to acceptable data types.
				final ModeOfTransport mode = GTFS_ROUTE_TYPE_TO_MODE_ENUM.get(type);

				try {
					dataHandler.addRoute(id, agency, mode, shortName, longName, description);
				} catch (final IllegalArgumentException | IllegalStateException e) {
					handleParseException("not all necessary information about route already set: ".concat(id), e);
				}
			}
			sqlResult.close();
			sqlStatement.close();
			connection.setAutoCommit(true);
		} catch (final SQLException e) {
			handleSqlException("connection failed during routes loading", e);
		}
	}

	/**
	 * Load public transport trip date intervals information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsSQLException
	 * 		If the database connection fails.
	 * @throws GtfsParseException
	 * 		If the data contains a mistake.
	 */
	public void loadDateIntervals(final GTFSDataHandler dataHandler) throws GtfsSQLException, GtfsParseException {
		if (dataHandler == null) {
			throw new NullPointerException("bad handler");
		}

		final String sqlQuery = String.format("SELECT DISTINCT %s, %s, %s, %s, %s, %s, %s, %s, %s, %s FROM %s WHERE " +
						"%s" +
						" < '%s' AND %s >= '%s';", SCHEMA_CALENDAR_TABLE_SERVICE_ID_COLUMN,
				SCHEMA_CALENDAR_TABLE_START_COLUMN,
				SCHEMA_CALENDAR_TABLE_END_COLUMN, SCHEMA_CALENDAR_TABLE_MONDAY_COLUMN,
				SCHEMA_CALENDAR_TABLE_TUESDAY_COLUMN, SCHEMA_CALENDAR_TABLE_WEDNESDAY_COLUMN,
				SCHEMA_CALENDAR_TABLE_THURSDAY_COLUMN, SCHEMA_CALENDAR_TABLE_FRIDAY_COLUMN,
				SCHEMA_CALENDAR_TABLE_SATURDAY_COLUMN, SCHEMA_CALENDAR_TABLE_SUNDAY_COLUMN, SCHEMA_CALENDAR_TABLE,
				SCHEMA_CALENDAR_TABLE_START_COLUMN, pruneAfterDate, SCHEMA_CALENDAR_TABLE_END_COLUMN, pruneBeforeDate);

		try {
			connection.setAutoCommit(false); // Needed for "setFetchSize".
			final Statement sqlStatement = connection.createStatement();
			sqlStatement.setFetchSize(sqlResultDownloadSize);
			final ResultSet sqlResult = sqlStatement.executeQuery(sqlQuery);
			while (sqlResult.next()) {
				// Fetch the result.
				final String id = sqlResult.getString(1);
				final Date startSql = sqlResult.getDate(2);
				final Date endSql = sqlResult.getDate(3);
				final List<Boolean> isAvailableInDay = new ArrayList<>(DateTimeConstants.DAYS_PER_WEEK);
				for (int i = 0; i < DateTimeConstants.DAYS_PER_WEEK; i++) {
					isAvailableInDay.add(getBoolean(sqlResult, 4 + i));
				}

				// Check all required values are provided.
				if (id == null || startSql == null || endSql == null || isAvailableInDay.contains(null)) {
					handleParseException("not all necessary information about service set: ".concat(id), null);
					continue;
				}
				if (startSql.after(endSql)) {
					handleParseException("start date cannot be after end date", null);
					continue;
				}

				// Convert data to acceptable data types.
				LocalDate startJoda = null;
				LocalDate endJoda = null;
				try {
					startJoda = new LocalDate(!startSql.before(pruneBeforeDate) ? startSql : pruneBeforeDate);
					endJoda = endSql.before(pruneAfterDate) ? new LocalDate(endSql) : new LocalDate(
							pruneAfterDate).minusDays(1);
				} catch (Exception ignored) {
				}

				try {
					// The handler takes left-closed interval.
					dataHandler.addDateInterval(id, startJoda, endJoda.plusDays(1), isAvailableInDay);
				} catch (final IllegalArgumentException | IllegalStateException e) {
					handleParseException("not all necessary information about service already set: ".concat(id), e);
				}
			}
			sqlResult.close();
			sqlStatement.close();
			connection.setAutoCommit(true);
		} catch (final SQLException e) {
			handleSqlException("connection failed during date intervals loading", e);
		}
	}

	/**
	 * Load public transport trip dates information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsSQLException
	 * 		If the database connection fails.
	 * @throws GtfsParseException
	 * 		If the data contains a mistake.
	 */
	public void loadDates(final GTFSDataHandler dataHandler) throws GtfsSQLException, GtfsParseException {
		if (dataHandler == null) {
			throw new NullPointerException("bad handler");
		}

		final String sqlQuery = String.format("SELECT DISTINCT %s, %s, %s FROM %s WHERE %s >= '%s' AND %s < '%s';",
				SCHEMA_CALENDAR_DATES_TABLE_SERVICE_ID_COLUMN, SCHEMA_CALENDAR_DATES_TABLE_DATE_COLUMN,
				SCHEMA_CALENDAR_DATES_TABLE_TYPE_COLUMN, SCHEMA_CALENDAR_DATES_TABLE,
				SCHEMA_CALENDAR_DATES_TABLE_DATE_COLUMN, pruneBeforeDate, SCHEMA_CALENDAR_DATES_TABLE_DATE_COLUMN,
				pruneAfterDate);

		try {
			connection.setAutoCommit(false); // Needed for "setFetchSize".
			final Statement sqlStatement = connection.createStatement();
			sqlStatement.setFetchSize(sqlResultDownloadSize);
			final ResultSet sqlResult = sqlStatement.executeQuery(sqlQuery);
			while (sqlResult.next()) {
				// Fetch the result.
				final String id = sqlResult.getString(1);
				final Date dateSql = sqlResult.getDate(2);
				final Integer type = getInteger(sqlResult, 3);

				// Check all required values are provided.
				if (id == null || dateSql == null && type == null) {
					handleParseException("not all necessary information about service set: ".concat(id), null);
					continue;
				}
				if (!GTFSDataLoader.GTFS_EXCEPTION_TYPE_TO_AVAILABILITY_FLAG.containsKey(type)) {
					handleParseException(String.format("unknown exception type: %d", type), null);
					continue;
				}

				// Convert data to acceptable data types.
				final LocalDate dateJoda = new LocalDate(dateSql);
				final boolean isAvailable = GTFSDataLoader.GTFS_EXCEPTION_TYPE_TO_AVAILABILITY_FLAG.get(type);

				try {
					if (isAvailable) {
						dataHandler.addDate(id, dateJoda);
					} else {
						dataHandler.removeDate(id, dateJoda);
					}
				} catch (final IllegalArgumentException | IllegalStateException e) {
					handleParseException("not all necessary information about service already set: ".concat(id), e);
				}
			}
			sqlResult.close();
			sqlStatement.close();
			connection.setAutoCommit(true);
		} catch (final SQLException e) {
			handleSqlException("connection failed during dates loading", e);
		}
	}

	/**
	 * Load public transport departures information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsSQLException
	 * 		If the database connection fails.
	 * @throws GtfsParseException
	 * 		If the data contains a mistake.
	 */
	public void loadDepartures(final GTFSDataHandler dataHandler) throws GtfsSQLException, GtfsParseException {
		if (dataHandler == null) {
			throw new NullPointerException("bad handler");
		}

		final TripMemory lastTrips = new TripMemory();
		boolean tripHasError = false;
		PartialDeparture previousDeparture = null;
		Period frequencyOffset = Period.ZERO;

		final String sqlQuery = String.format("SELECT DISTINCT %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s " +
						"FROM" + " %s JOIN %s ON %s = %s LEFT JOIN %s ON %s = %s ORDER BY %s ASC, %s ASC, %s ASC;",
				SCHEMA_TRIPS_TABLE_ID_COLUMN, SCHEMA_TRIPS_TABLE_TRIP_HEADSIGN_COLUMN,
				SCHEMA_STOP_TIMES_TABLE_SEQUENCE_COLUMN, SCHEMA_STOP_TIMES_TABLE_STOP_ID_COLUMN,
				SCHEMA_TRIPS_TABLE_ROUTE_ID_COLUMN, SCHEMA_TRIPS_TABLE_SERVICE_ID_COLUMN,
				SCHEMA_STOP_TIMES_TABLE_DISTANCE_COLUMN, SCHEMA_STOP_TIMES_TABLE_ARRIVAL_COLUMN,
				SCHEMA_STOP_TIMES_TABLE_DEPARTURE_COLUMN, SCHEMA_FREQUENCIES_TABLE_START_COLUMN,
				SCHEMA_FREQUENCIES_TABLE_END_COLUMN, SCHEMA_FREQUENCIES_TABLE_HEADWAY_COLUMN,
				SCHEMA_FREQUENCIES_TABLE_EXACT_COLUMN, SCHEMA_STOP_TIMES_TABLE, SCHEMA_TRIPS_TABLE,
				SCHEMA_STOP_TIMES_TABLE_TRIP_ID_COLUMN, SCHEMA_TRIPS_TABLE_ID_COLUMN, SCHEMA_FREQUENCIES_TABLE,
				SCHEMA_STOP_TIMES_TABLE_TRIP_ID_COLUMN, SCHEMA_FREQUENCIES_TABLE_TRIP_ID_COLUMN,
				SCHEMA_TRIPS_TABLE_ID_COLUMN, SCHEMA_FREQUENCIES_TABLE_START_COLUMN,
				SCHEMA_STOP_TIMES_TABLE_SEQUENCE_COLUMN);

		try {
			connection.setAutoCommit(false); // Needed for "setFetchSize".
			final Statement sqlStatement = connection.createStatement();
			sqlStatement.setFetchSize(sqlResultDownloadSize);
			// logger.debug(sqlQuery);
			final ResultSet sqlResult = sqlStatement.executeQuery(sqlQuery);
			while (sqlResult.next()) {
				// Fetch the result.
				final String id = sqlResult.getString(1);
				final String trip_headsign = sqlResult.getString(2);
				final Integer order = getInteger(sqlResult, 3);
				final String stop = sqlResult.getString(4);
				final String route = sqlResult.getString(5);
				final String service = sqlResult.getString(6);
				final Double distanceFromStart = getDouble(sqlResult, 7);
				final String arrivalS = sqlResult.getString(8);
				final String departureS = sqlResult.getString(9);
				final String frequencyStartS = sqlResult.getString(10);
				final String frequencyEndS = sqlResult.getString(11);
				final Integer headwayInSeconds = getInteger(sqlResult, 12);
				final Boolean exactTimes = getBoolean(sqlResult, 13);

				// Check all required values are provided.
				final boolean isDefinedByFrequency = frequencyStartS != null && frequencyEndS != null &&
						headwayInSeconds != null;
				assert isDefinedByFrequency || (frequencyStartS == null && frequencyEndS == null && headwayInSeconds
						== null && exactTimes == null) : "How to handle null in frequency definition.";
				if (id == null || order == null || stop == null || route == null || service == null || arrivalS ==
						null || departureS == null) {
					handleParseException("not all necessary information about trip set: ".concat(id), null);
					tripHasError = true;
					continue;
				}

				// Convert data to acceptable data types.
				final Period departurePeriod = Period.parse(departureS,
						GTFSDataLoader.GTFS_ARRIVAL_DEPARTURE_FORMATTER);
				final Period arrivalPeriod = Period.parse(arrivalS, GTFSDataLoader.GTFS_ARRIVAL_DEPARTURE_FORMATTER);
				final Period frequencyStartPeriod = frequencyStartS == null ? null : Period.parse(frequencyStartS,
						GTFSDataLoader.GTFS_ARRIVAL_DEPARTURE_FORMATTER);
				final Period frequencyEndPeriod = frequencyEndS == null ? null : Period.parse(frequencyEndS,
						GTFSDataLoader.GTFS_ARRIVAL_DEPARTURE_FORMATTER);
				final Period headwayPeriod = headwayInSeconds == null ? null : Period.seconds(headwayInSeconds);
				final PartialDeparture nextDeparture = new PartialDeparture(id, stop, order, route, service,
						distanceFromStart, departurePeriod, frequencyStartPeriod, frequencyEndPeriod, headwayPeriod,
						exactTimes);

				if (lastTrips.isNew(nextDeparture)) {
					// Start of a new trip resets variables.
					lastTrips.add(nextDeparture);
					tripHasError = false;
					previousDeparture = null;
					frequencyOffset = Period.ZERO;
				} else if (!tripHasError) {
					// Add new departure for previous stop because the arrival
					// is known.
					assert previousDeparture.order < nextDeparture.order : String.format(
							"The SQL result is probably " + "not sorted by stop sequence numbers. Check the SQL " +
									"results in the log and the query: " +
									"%s", sqlQuery);
					assert previousDeparture.route.equals(nextDeparture.route) : "Is it really meaningful?";

					final ReadablePeriod startTime = previousDeparture.frequencyStart == null ? previousDeparture
							.departureTime : previousDeparture.frequencyStart.plus(
							frequencyOffset);
					final ReadablePeriod timePeriod = previousDeparture.headway == null ? new Period(
							1) : previousDeparture.headway;
					final ReadablePeriod endTime = previousDeparture.frequencyStart == null ? previousDeparture
							.departureTime.plus(
							timePeriod) : previousDeparture.frequencyEnd.plus(frequencyOffset);
					final Period travelTime = arrivalPeriod.minus(previousDeparture.departureTime);
					final Period waitingTime = departurePeriod.minus(arrivalPeriod);

					final Double distanceInM;
					if (nextDeparture.distanceFromStart == null || previousDeparture.distanceFromStart == null) {
						assert nextDeparture.distanceFromStart == null && previousDeparture.distanceFromStart == null
								: "How to handle one null in two given distances?";
						distanceInM = null;
					} else {
						distanceInM = (nextDeparture.distanceFromStart - previousDeparture.distanceFromStart) *
								gtfsUnitToMetersMultiplier;
					}

					try {
						dataHandler.addDepartures(previousDeparture.stop, nextDeparture.stop, previousDeparture.route,
								previousDeparture.service, previousDeparture.id, trip_headsign, startTime, timePeriod,
								endTime, previousDeparture.isExact, distanceInM, travelTime);
					} catch (final IllegalStateException e) {
						handleParseException("not all necessary information about trip already set: ".concat(id), e);
						tripHasError = true;
					}

					// Remember variables for future arrival.
					frequencyOffset = frequencyOffset.plus(travelTime).plus(waitingTime);
				}

				// Remember variables for future checks and arrival.
				previousDeparture = nextDeparture;
			}
			sqlResult.close();
			sqlStatement.close();
			connection.setAutoCommit(true);
		} catch (final SQLException e) {
			handleSqlException("connection failed during departures loading", e);
		}
	}

	/**
	 * Get {@code Integer} from SQL result set at given column index.
	 *
	 * @param result
	 * 		A result set.
	 * @param columnIndex
	 * 		A column index.
	 *
	 * @return The integer or {@code null} if it is {@code NULL}.
	 *
	 * @throws SQLException
	 * 		If the {@code columnIndex} is not valid, if a database access error occurs or this method is called on a
	 * 		closed
	 * 		result set.
	 */
	private static Integer getInteger(final ResultSet result, final int columnIndex) throws SQLException {
		final int integer = result.getInt(columnIndex);
		return result.wasNull() ? null : integer;
	}

	/**
	 * Get {@code Double} from SQL result set at given column index.
	 *
	 * @param result
	 * 		A result set.
	 * @param columnIndex
	 * 		A column index.
	 *
	 * @return The double or {@code null} if it is {@code NULL}.
	 *
	 * @throws SQLException
	 * 		If the {@code columnIndex} is not valid, if a database access error occurs or this method is called on a
	 * 		closed
	 * 		result set.
	 */
	private static Double getDouble(final ResultSet result, final int columnIndex) throws SQLException {
		final double double_ = result.getDouble(columnIndex);
		return result.wasNull() ? null : double_;
	}

	/**
	 * Get {@code Boolean} from SQL result set at given column index.
	 *
	 * @param result
	 * 		A result set.
	 * @param columnIndex
	 * 		A column index.
	 *
	 * @return The boolean or {@code null} if it is {@code NULL}.
	 *
	 * @throws SQLException
	 * 		If the {@code columnIndex} is not valid, if a database access error occurs or this method is called on a
	 * 		closed
	 * 		result set.
	 */
	private static Boolean getBoolean(final ResultSet result, final int columnIndex) throws SQLException {
		final boolean boolean_ = result.getBoolean(columnIndex);
		return result.wasNull() ? null : boolean_;
	}

	private static EPSGProjection createProjection(int epsgSrid) {
		try {
			return new EPSGProjection(epsgSrid);
		} catch (Exception e) {
			throw new IllegalArgumentException("EPSGProjection could not be instantiated for EPSG=" + epsgSrid, e);
		}
	}

	/**
	 * Structure holding partial information about a departure.
	 *
	 * @author Radek Holy
	 */
	private final static class PartialDeparture {

		/**
		 * An identifier of a trip of the departure.
		 */
		public final String id;

		/**
		 * ID of a stop of the departure.
		 */
		public final String stop;

		/**
		 * Order of the stop in the trip sequence.
		 */
		public final int order;

		/**
		 * ID of a route of the departure.
		 */
		public final String route;

		/**
		 * ID of a dates definition of the departure.
		 */
		public final String service;

		/**
		 * Distance traveled from trip start to the stop. Possibly {@code null}.
		 */
		public final Double distanceFromStart;

		/**
		 * Time of the departure.
		 */
		public final Period departureTime;

		/**
		 * Left endpoint of departure frequency interval (inclusive). Possibly {@code null}.
		 */
		public final Period frequencyStart;

		/**
		 * Right endpoint of departure frequency interval (exclusive). Possibly {@code null}.
		 */
		public final Period frequencyEnd;

		/**
		 * Period of departure frequency. Possibly {@code null}.
		 */
		public final Period headway;

		/**
		 * Flag indicating whether the departure time is exact. Possibly {@code null}.
		 */
		public final Boolean isExact;

		/**
		 * Construct a new instance.
		 *
		 * @param id
		 * 		An identifier of a trip of the departure.
		 * @param stop
		 * 		ID of a stop of the departure.
		 * @param order
		 * 		Order of the stop in the trip sequence.
		 * @param route
		 * 		ID of a route of the departure.
		 * @param service
		 * 		ID of a dates definition of the departure.
		 * @param distanceFromStart
		 * 		Distance traveled from trip start to the stop. Possibly {@code null}.
		 * @param departureTime
		 * 		Time of the departure.
		 * @param frequencyStart
		 * 		Left endpoint of departure frequency interval (inclusive). Possibly {@code null}.
		 * @param frequencyEnd
		 * 		Right endpoint of departure frequency interval (exclusive). Possibly {@code null}.
		 * @param headway
		 * 		Period of departure frequency. Possibly {@code null}.
		 * @param isExact
		 * 		Flag indicating whether the departure time is exact. Possibly {@code null}.
		 */
		public PartialDeparture(final String id, final String stop, final int order, final String route,
								final String service, final Double distanceFromStart,
								final ReadablePeriod departureTime, final ReadablePeriod frequencyStart,
								final ReadablePeriod frequencyEnd, final ReadablePeriod headway,
								final Boolean isExact) {
			if (id == null) {
				throw new NullPointerException("bad identifier");
			}
			if (stop == null) {
				throw new NullPointerException("bad stop");
			}
			if (route == null) {
				throw new NullPointerException("bad route");
			}
			if (service == null) {
				throw new NullPointerException("bad dates");
			}
			if (departureTime == null) {
				throw new NullPointerException("bad time");
			}

			this.id = id;
			this.stop = stop;
			this.order = order;
			this.route = route;
			this.service = service;
			this.distanceFromStart = distanceFromStart;
			this.departureTime = new Period(departureTime);
			this.frequencyStart = frequencyStart != null ? new Period(frequencyStart) : null;
			this.frequencyEnd = frequencyEnd != null ? new Period(frequencyEnd) : null;
			this.headway = headway != null ? new Period(headway) : null;
			this.isExact = isExact;
		}
	}

	/**
	 * Memory of last departures.
	 *
	 * @author Radek Holy
	 */
	private static final class TripMemory {

		/**
		 * ID of trip of the last departure.
		 */
		private String lastId = null;

		/**
		 * Time frequency start of the last departure.
		 */
		private ReadablePeriod lastTime = null;

		/**
		 * All trip IDs of last departures.
		 */
		private Collection<String> lastIds = new HashSet<String>();

		/**
		 * Add last departure.
		 *
		 * @param departure
		 * 		Last departure.
		 */
		public final void add(final PartialDeparture departure) {
			if (departure == null) {
				throw new NullPointerException("bad departure");
			}

			lastId = departure.id;
			lastTime = departure.frequencyStart == null ? null : new Period(departure.frequencyStart);
			lastIds.add(departure.id);
		}

		/**
		 * Test whether the departure is of a new trip.
		 *
		 * @param departure
		 * 		A departure.
		 *
		 * @return The test result.
		 */
		public final boolean isNew(final PartialDeparture departure) {
			if (departure == null) {
				throw new NullPointerException("bad departure");
			}

			if (lastIds.isEmpty()) {
				return true;
			}

			if (!departure.id.equals(lastId)) {
				assert !lastIds.contains(departure.id) : "The SQL result is probably not sorted by trip IDs.";

				return true;
			} else {
				if (departure.frequencyStart == null || lastTime == null) {
					return departure.frequencyStart != lastTime;
				} else {
					return !departure.frequencyStart.equals(lastTime);
				}
			}
		}
	}



	/**
	 * Name of the "agency" table in the database schema.
	 */
	private static final String SCHEMA_AGENCY_TABLE = "gtfs.gtfs_agency";

	/**
	 * Name of the column with agency IDs in the "agency" database table.
	 */
	private static final String SCHEMA_AGENCY_TABLE_ID_COLUMN = SCHEMA_AGENCY_TABLE + ".agency_id";

	/**
	 * Name of the column with agency names in the "agency" database table.
	 */
	private static final String SCHEMA_AGENCY_TABLE_NAME_COLUMN = SCHEMA_AGENCY_TABLE + ".agency_name";

	/**
	 * Name of the column with time zones in the "agency" database table.
	 */
	private static final String SCHEMA_AGENCY_TABLE_TIMEZONE_COLUMN = SCHEMA_AGENCY_TABLE + ".agency_timezone";

	/**
	 * Name of the column with url in the "agency" database table.
	 */
	private static final String SCHEMA_AGENCY_TABLE_URL_COLUMN = SCHEMA_AGENCY_TABLE + ".agency_url";

	/**
	 * Name of the column with url in the "agency" database table.
	 */
	private static final String SCHEMA_AGENCY_TABLE_PHONE_COLUMN = SCHEMA_AGENCY_TABLE + ".agency_phone";

	/**
	 * Name of the "calendar" table in the database schema.
	 */
	private static final String SCHEMA_CALENDAR_TABLE = "gtfs.gtfs_calendar";

	/**
	 * Name of the column with ends in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_END_COLUMN = SCHEMA_CALENDAR_TABLE + ".end_date";

	/**
	 * Name of the column with Fridays in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_FRIDAY_COLUMN = SCHEMA_CALENDAR_TABLE + ".friday";

	/**
	 * Name of the column with Mondays in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_MONDAY_COLUMN = SCHEMA_CALENDAR_TABLE + ".monday";

	/**
	 * Name of the column with Saturdays in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_SATURDAY_COLUMN = SCHEMA_CALENDAR_TABLE + ".saturday";

	/**
	 * Name of the column with service IDs in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_SERVICE_ID_COLUMN = SCHEMA_CALENDAR_TABLE + ".service_id";

	/**
	 * Name of the column with starts in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_START_COLUMN = SCHEMA_CALENDAR_TABLE + ".start_date";

	/**
	 * Name of the column with Sundays in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_SUNDAY_COLUMN = SCHEMA_CALENDAR_TABLE + ".sunday";

	/**
	 * Name of the column with Thursday in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_THURSDAY_COLUMN = SCHEMA_CALENDAR_TABLE + ".thursday";

	/**
	 * Name of the column with Tuesdays in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_TUESDAY_COLUMN = SCHEMA_CALENDAR_TABLE + ".tuesday";

	/**
	 * Name of the column with Wednesday in the "calendar" database table.
	 */
	private static final String SCHEMA_CALENDAR_TABLE_WEDNESDAY_COLUMN = SCHEMA_CALENDAR_TABLE + ".wednesday";

	/**
	 * Name of the "calendar dates" table in the database schema.
	 */
	private static final String SCHEMA_CALENDAR_DATES_TABLE = "gtfs.gtfs_calendar_dates";

	/**
	 * Name of the column with dates in the "calendar dates" database table.
	 */
	private static final String SCHEMA_CALENDAR_DATES_TABLE_DATE_COLUMN = SCHEMA_CALENDAR_DATES_TABLE + ".date";

	/**
	 * Name of the column with service IDs in the "calendar dates" database table.
	 */
	private static final String SCHEMA_CALENDAR_DATES_TABLE_SERVICE_ID_COLUMN = SCHEMA_CALENDAR_DATES_TABLE + "" +
			".service_id";

	/**
	 * Name of the column with exception types in the "calendar dates" database table.
	 */
	private static final String SCHEMA_CALENDAR_DATES_TABLE_TYPE_COLUMN = SCHEMA_CALENDAR_DATES_TABLE + "" +
			".exception_type";

	/**
	 * Name of the "frequencies" table in the database schema.
	 */
	private static final String SCHEMA_FREQUENCIES_TABLE = "gtfs.gtfs_frequencies";

	/**
	 * Name of the column with trip IDs in the "frequencies" database table.
	 */
	private static final String SCHEMA_FREQUENCIES_TABLE_TRIP_ID_COLUMN = SCHEMA_FREQUENCIES_TABLE + ".trip_id";

	/**
	 * Name of the column with starts in the "frequencies" database table.
	 */
	private static final String SCHEMA_FREQUENCIES_TABLE_START_COLUMN = SCHEMA_FREQUENCIES_TABLE + ".start_time";

	/**
	 * Name of the column with ends in the "frequencies" database table.
	 */
	private static final String SCHEMA_FREQUENCIES_TABLE_END_COLUMN = SCHEMA_FREQUENCIES_TABLE + ".end_time";

	/**
	 * Name of the column with headways in the "frequencies" database table.
	 */
	private static final String SCHEMA_FREQUENCIES_TABLE_HEADWAY_COLUMN = SCHEMA_FREQUENCIES_TABLE + ".headway_secs";

	/**
	 * Name of the column with exact flags in the "frequencies" database table.
	 */
	private static final String SCHEMA_FREQUENCIES_TABLE_EXACT_COLUMN = SCHEMA_FREQUENCIES_TABLE + ".exact_times";

	/**
	 * Name of the "routes" table in the database schema.
	 */
	private static final String SCHEMA_ROUTES_TABLE = "gtfs.gtfs_routes";

	/**
	 * Name of the column with agency IDs in the "routes" database table.
	 */
	private static final String SCHEMA_ROUTES_TABLE_AGENCY_ID_COLUMN = SCHEMA_ROUTES_TABLE + ".agency_id";

	/**
	 * Name of the column with IDs in the "routes" database table.
	 */
	private static final String SCHEMA_ROUTES_TABLE_ID_COLUMN = SCHEMA_ROUTES_TABLE + ".route_id";

	/**
	 * Name of the column with types in the "routes" database table.
	 */
	private static final String SCHEMA_ROUTES_TABLE_TYPE_COLUMN = SCHEMA_ROUTES_TABLE + ".route_type";

	/**
	 * Name of the column with types in the "routes" database table.
	 */
	private static final String SCHEMA_ROUTES_TABLE_SHORT_NAME_COLUMN = SCHEMA_ROUTES_TABLE + ".route_short_name";

	/**
	 * Name of the column with types in the "routes" database table.
	 */
	private static final String SCHEMA_ROUTES_TABLE_LONG_NAME_COLUMN = SCHEMA_ROUTES_TABLE + ".route_long_name";

	/**
	 * Name of the column with types in the "routes" database table.
	 */
	private static final String SCHEMA_ROUTES_TABLE_DESCRIPTION_COLUMN = SCHEMA_ROUTES_TABLE + ".route_desc";

	/**
	 * Name of the "stops" table in the database schema.
	 */
	private static final String SCHEMA_STOPS_TABLE = "gtfs.gtfs_stops";

	/**
	 * Name of the column with codes in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_CODE_COLUMN = SCHEMA_STOPS_TABLE + ".stop_code";

	/**
	 * Name of the column with descriptions in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_DESCRIPTION_COLUMN = SCHEMA_STOPS_TABLE + ".stop_desc";

	/**
	 * Name of the column with IDs in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_ID_COLUMN = SCHEMA_STOPS_TABLE + ".stop_id";

	/**
	 * Name of the column with latitudes in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_LATITUDE_COLUMN = SCHEMA_STOPS_TABLE + ".stop_lat";

	/**
	 * Name of the column with longitudes in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_LONGITUDE_COLUMN = SCHEMA_STOPS_TABLE + ".stop_lon";

	/**
	 * Name of the column with names in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_NAME_COLUMN = SCHEMA_STOPS_TABLE + ".stop_name";

	/**
	 * Name of the column with wheelchair boarding possibilities in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_WHEELCHAIR_BOARDING_COLUMN = SCHEMA_STOPS_TABLE + "" +
			".wheelchair_boarding";

	/**
	 * Name of the column with zone IDs in the "stops" database table.
	 */
	private static final String SCHEMA_STOPS_TABLE_ZONE_ID_COLUMN = SCHEMA_STOPS_TABLE + ".zone_id";

	/**
	 * Name of the "stop times" table in the database schema.
	 */
	private static final String SCHEMA_STOP_TIMES_TABLE = "gtfs.gtfs_stop_times";

	/**
	 * Name of the column with arrivals in the "stop times" database table.
	 */
	private static final String SCHEMA_STOP_TIMES_TABLE_ARRIVAL_COLUMN = SCHEMA_STOP_TIMES_TABLE + ".arrival_time";

	/**
	 * Name of the column with departures in the "stop times" database table.
	 */
	private static final String SCHEMA_STOP_TIMES_TABLE_DEPARTURE_COLUMN = SCHEMA_STOP_TIMES_TABLE + ".departure_time";

	/**
	 * Name of the column with traveled distances in the "stop times" database table.
	 */
	private static final String SCHEMA_STOP_TIMES_TABLE_DISTANCE_COLUMN = SCHEMA_STOP_TIMES_TABLE + "" +
			".shape_dist_traveled";

	/**
	 * Name of the column with sequence numbers in the "stop times" database table.
	 */
	private static final String SCHEMA_STOP_TIMES_TABLE_SEQUENCE_COLUMN = SCHEMA_STOP_TIMES_TABLE + ".stop_sequence";

	/**
	 * Name of the column with stop IDs in the "stop times" database table.
	 */
	private static final String SCHEMA_STOP_TIMES_TABLE_STOP_ID_COLUMN = SCHEMA_STOP_TIMES_TABLE + ".stop_id";

	/**
	 * Name of the column with trip IDs in the "stop times" database table.
	 */
	private static final String SCHEMA_STOP_TIMES_TABLE_TRIP_ID_COLUMN = SCHEMA_STOP_TIMES_TABLE + ".trip_id";

	/**
	 * Name of the "trips" table in the database schema.
	 */
	private static final String SCHEMA_TRIPS_TABLE = "gtfs.gtfs_trips";

	/**
	 * Name of the column with IDs in the "trips" database table.
	 */
	private static final String SCHEMA_TRIPS_TABLE_ID_COLUMN = SCHEMA_TRIPS_TABLE + ".trip_id";

	/**
	 * Name of the column with route IDs in the "trips" database table.
	 */
	private static final String SCHEMA_TRIPS_TABLE_ROUTE_ID_COLUMN = SCHEMA_TRIPS_TABLE + ".route_id";

	/**
	 * Name of the column with service IDs in the "trips" database table.
	 */
	private static final String SCHEMA_TRIPS_TABLE_SERVICE_ID_COLUMN = SCHEMA_TRIPS_TABLE + ".service_id";

	/**
	 * Name of the column with trip headsigns in the "trips" database table.
	 */
	private static final String SCHEMA_TRIPS_TABLE_TRIP_HEADSIGN_COLUMN = SCHEMA_TRIPS_TABLE + ".trip_headsign";
}
