package cz.agents.gtdgraphimporter.gtfs;

import com.google.common.collect.ImmutableList;
import cz.agents.basestructures.GPSLocation;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsParseException;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.additional.WheelchairBoarding;
import org.hamcrest.Description;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test suite of GTFS database parser.
 * <p>
 * The suite is not complete. Tests against some extreme GTFS data and bad database schemas are missing.
 *
 * @author Radek Holy
 */
// TODO: see JavaDoc
public final class GTFSDatabaseLoaderTest {

	/**
	 * SQL command querying all agencies.
	 */
	private static final String AGENCIES_SQL_QUERY = "SELECT DISTINCT gtfs.gtfs_agency.agency_id, gtfs.gtfs_agency" +
			".agency_timezone FROM gtfs.gtfs_agency;";

	/**
	 * SQL command querying all stops.
	 */
	private static final String STOPS_SQL_QUERY = "SELECT DISTINCT gtfs.gtfs_stops.stop_id, gtfs.gtfs_stops" +
			".stop_lat," +
			" " +
			"gtfs.gtfs_stops.stop_lon, gtfs.gtfs_stops.stop_desc, gtfs.gtfs_stops.zone_id, gtfs.gtfs_stops" +
			".stop_code," +
			" " +
			"gtfs.gtfs_stops.stop_name, gtfs.gtfs_stops.wheelchair_boarding FROM gtfs.gtfs_stops;";

	/**
	 * SQL command querying all routes.
	 */
	private static final String ROUTES_SQL_QUERY = "SELECT DISTINCT gtfs.gtfs_routes.route_id, gtfs.gtfs_routes" + "" +
			".agency_id, gtfs.gtfs_routes.route_type FROM gtfs.gtfs_routes;";

	/**
	 * SQL command querying all date intervals.
	 */
	private static final String DATE_INTERVALS_SQL_QUERY = "SELECT DISTINCT gtfs.gtfs_calendar.service_id, gtfs" +
			".gtfs_calendar.start_date, gtfs.gtfs_calendar.end_date, gtfs.gtfs_calendar.monday, gtfs.gtfs_calendar" +
			".tuesday, gtfs.gtfs_calendar.wednesday, gtfs.gtfs_calendar.thursday, gtfs.gtfs_calendar.friday, gtfs" +
			".gtfs_calendar.saturday, gtfs.gtfs_calendar.sunday FROM gtfs.gtfs_calendar WHERE gtfs.gtfs_calendar" +
			".start_date < '9999-12-31' AND gtfs.gtfs_calendar.end_date >= '1000-01-01';";

	/**
	 * SQL command querying all date intervals using {@link #ANOTHER_PRUNE_BEFORE_DATE} and {@link
	 * #ANOTHER_PRUNE_AFTER_DATE}.
	 */
	private static final String DATE_INTERVALS_SQL_QUERY_WITH_PRUNING = "SELECT DISTINCT gtfs.gtfs_calendar" +
			".service_id, gtfs.gtfs_calendar.start_date, gtfs.gtfs_calendar.end_date, gtfs.gtfs_calendar.monday, " +
			"gtfs" +
			".gtfs_calendar.tuesday, gtfs.gtfs_calendar.wednesday, gtfs.gtfs_calendar.thursday, gtfs.gtfs_calendar" +
			".friday, gtfs.gtfs_calendar.saturday, gtfs.gtfs_calendar.sunday FROM gtfs.gtfs_calendar WHERE gtfs" +
			".gtfs_calendar.start_date < '2015-07-24' AND gtfs.gtfs_calendar.end_date >= '2010-07-24';";

	/**
	 * SQL command querying all dates.
	 */
	private static final String DATES_SQL_QUERY = "SELECT DISTINCT gtfs.gtfs_calendar_dates.service_id, gtfs" +
			".gtfs_calendar_dates.date, gtfs.gtfs_calendar_dates.exception_type FROM gtfs.gtfs_calendar_dates WHERE " +
			"gtfs.gtfs_calendar_dates.date >= '1000-01-01' AND gtfs.gtfs_calendar_dates.date < '9999-12-31';";

	/**
	 * SQL command querying all dates using {@link #ANOTHER_PRUNE_BEFORE_DATE} and {@link #ANOTHER_PRUNE_AFTER_DATE}.
	 */
	private static final String DATES_SQL_QUERY_WITH_PRUNING = "SELECT DISTINCT gtfs.gtfs_calendar_dates.service_id," +
			" " +
			"gtfs.gtfs_calendar_dates.date, gtfs.gtfs_calendar_dates.exception_type FROM gtfs.gtfs_calendar_dates " +
			"WHERE gtfs.gtfs_calendar_dates.date >= '2010-07-24' AND gtfs.gtfs_calendar_dates.date < '2015-07-24';";

	/**
	 * SQL command querying all departures.
	 */
	private static final String DEPARTURES_SQL_QUERY = "SELECT DISTINCT gtfs.gtfs_trips.trip_id, gtfs" +
			".gtfs_stop_times" +
			".stop_sequence, gtfs.gtfs_stop_times.stop_id, gtfs.gtfs_trips.route_id, gtfs.gtfs_trips.service_id, " +
			"gtfs" +
			".gtfs_stop_times.shape_dist_traveled, gtfs.gtfs_stop_times.arrival_time, gtfs.gtfs_stop_times" +
			".departure_time, gtfs.gtfs_frequencies.start_time, gtfs.gtfs_frequencies.end_time, gtfs" +
			".gtfs_frequencies" +
			".headway_secs, gtfs.gtfs_frequencies.exact_times FROM gtfs.gtfs_stop_times JOIN gtfs.gtfs_trips ON gtfs" +
			".gtfs_stop_times.trip_id = gtfs.gtfs_trips.trip_id LEFT JOIN gtfs.gtfs_frequencies ON gtfs" +
			".gtfs_stop_times.trip_id = gtfs.gtfs_frequencies.trip_id ORDER BY gtfs.gtfs_trips.trip_id ASC, gtfs" +
			".gtfs_frequencies.start_time ASC, gtfs.gtfs_stop_times.stop_sequence ASC;";

	/**
	 * Default EPSG SRID.
	 */
	private static final int DEFAULT_EPSG_SRID = 2065;

	/**
	 * Unknown/bad EPSG SRID.
	 */
	private static final int BAD_EPSG_SRID = -1;

	/**
	 * Default number used to convert from GTFS units to meters.
	 */
	private static final double DEFAULT_GTFS_UNIT_TO_METERS_MULTIPLIER = 1;

	/**
	 * A date before which GTFS data should be pruned in some test cases.
	 */
	private static final Date ANOTHER_PRUNE_BEFORE_DATE = Date.valueOf("2010-7-24");

	/**
	 * A date after which GTFS data should be pruned in some test cases.
	 */
	private static final Date ANOTHER_PRUNE_AFTER_DATE = Date.valueOf("2015-7-24");

	/**
	 * Test the loading of the public transport agencies information.
	 * <p>
	 * The testing scenario contains no data. This tests appropriate handling of no data.
	 */
	@Ignore // TODO : Mock changed
	public final void loadGtfsNoAgency() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadAgencies(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verifyZeroInteractions(handler);
	}

	/**
	 * Test the loading of the public transport agencies information.
	 * <p>
	 * The testing scenario contains only three agencies. This tests appropriate handling of agencies (including {@code
	 * null} instead of some values).
	 */
	@Ignore // TODO : Query changed
	public final void loadGtfsThreeAgencies() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();

			final ResultSet agenciesResult = Mockito.mock(ResultSet.class);
			when(agenciesResult.next()).thenReturn(true, true, true, false);
			// ID
			when(agenciesResult.getString(1)).thenReturn(null, "id1", "id2").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// time zone
			when(agenciesResult.getString(2)).thenReturn("Europe/Prague", "Europe/Helsinki", "Europe/Prague")
					.thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(AGENCIES_SQL_QUERY, agenciesResult);

			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadAgencies(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addAgency(null, null, ZoneId.of("Europe/Prague"), null, null);
		verify(handler).addAgency("id1", null, ZoneId.of("Europe/Helsinki"), null, null);
		verify(handler).addAgency("id2", null, ZoneId.of("Europe/Prague"), null, null);
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport stops information.
	 * <p>
	 * The testing scenario contains no data. This tests appropriate handling of no data.
	 */
	@Test
	public final void loadGtfsNoStop() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadStops(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verifyZeroInteractions(handler);
	}

	/**
	 * Test the loading of the public transport stops information.
	 * <p>
	 * The testing scenario contains only three stops. This tests appropriate handling of stops (including {@code null}
	 * instead of some values).
	 */
	@Test
	public final void loadGtfsThreeStops() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet stopsResult = Mockito.mock(ResultSet.class);
			when(stopsResult.next()).thenReturn(true, true, true, false);
			// ID
			when(stopsResult.getString(1)).thenReturn("i1", "i2", "i3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Latitude
			when(stopsResult.getDouble(2)).thenReturn(49., 50., 51.).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Longitude
			when(stopsResult.getDouble(3)).thenReturn(13., 14., 15.).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Description
			when(stopsResult.getString(4)).thenReturn(null, "d2", "d3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Zone
			when(stopsResult.getString(5)).thenReturn(null, "z2", "z3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Code
			when(stopsResult.getString(6)).thenReturn(null, "c2", "c3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Name
			when(stopsResult.getString(7)).thenReturn("n1", "n2", "n3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Wheel chair boarding
			when(stopsResult.getInt(8)).thenReturn(0, 1, 2).thenThrow(
					new UnsupportedOperationException("unexpected " + "stub call"));
			// Latitude, longitude
			when(stopsResult.wasNull()).thenReturn(//
					false, false, // 1st stop
					false, false, // 2nd stop
					false, false) // 3rd stop
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(STOPS_SQL_QUERY, stopsResult);

			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadStops(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addStop(eq("i1"), Matchers.eq(null), eq("n1"), Matchers.eq(null),
				projectedEq(new GPSLocation(49, 13., 1147674, 862186)), Matchers.eq(null),
				eq(WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION));
		verify(handler).addStop(eq("i2"), eq("c2"), eq("n2"), eq("d2"),
				projectedEq(new GPSLocation(50., 14., 1048448, 774041)), eq("z2"),
				eq(WheelchairBoarding.AT_LEAST_SOME_VEHICLES));
		verify(handler).addStop(eq("i3"), eq("c3"), eq("n3"), eq("d3"),
				projectedEq(new GPSLocation(51., 15., 947833, 688702)), eq("z3"), eq(WheelchairBoarding
						.NOT_POSSIBLE));
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport stops information.
	 * <p>
	 * The testing scenario contains bad EPSG SRID. This tests appropriate handling of bad EPSG SRIDs.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void loadGtfsThreeStopsWithBadEpsg() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet stopsResult = Mockito.mock(ResultSet.class);
			when(stopsResult.next()).thenReturn(true, true, true, false);
			// ID
			when(stopsResult.getString(1)).thenReturn("i1", "i2", "i3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Latitude
			when(stopsResult.getDouble(2)).thenReturn(11., 12., 13.).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Longitude
			when(stopsResult.getDouble(3)).thenReturn(21., 22., 23.).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Description
			when(stopsResult.getString(4)).thenReturn(null, "d2", "d3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Zone
			when(stopsResult.getString(5)).thenReturn(null, "z2", "z3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Code
			when(stopsResult.getString(6)).thenReturn(null, "c2", "c3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Name
			when(stopsResult.getString(7)).thenReturn("n1", "n2", "n3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Wheel chair boarding
			when(stopsResult.getInt(8)).thenReturn(0, 1, 2).thenThrow(
					new UnsupportedOperationException("unexpected " + "stub call"));
			// Latitude, longitude
			when(stopsResult.wasNull()).thenReturn(//
					false, false, // 1st stop
					false, false, // 2nd stop
					false, false) // 3rd stop
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(STOPS_SQL_QUERY, stopsResult);

			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoaderWithBadEpsg(connection);
		try {
			loader.loadStops(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addStop(eq("i1"), Matchers.<String>eq(null), eq("n1"), Matchers.eq(null),
				projectedEq(new GPSLocation(11., 21., 0, 0)), Matchers.eq(null),
				eq(WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION));
		verify(handler).addStop(eq("i2"), eq("c2"), eq("n2"), eq("d2"), projectedEq(new GPSLocation(12., 22., 0, 0)),
				eq("z2"), eq(WheelchairBoarding.AT_LEAST_SOME_VEHICLES));
		verify(handler).addStop(eq("i3"), eq("c3"), eq("n3"), eq("d3"), projectedEq(new GPSLocation(13., 23., 0, 0)),
				eq("z3"), eq(WheelchairBoarding.NOT_POSSIBLE));
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport routes information.
	 * <p>
	 * The testing scenario contains no data. This tests appropriate handling of no data.
	 */
	// TODO : update mock
	@Ignore
	public final void loadGtfsNoRoute() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadRoutes(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verifyZeroInteractions(handler);
	}

	/**
	 * Test the loading of the public transport routes information.
	 * <p>
	 * The testing scenario contains only three routes. This tests appropriate handling of routes (including {@code
	 * null} instead of some values).
	 */
	// TODO : update mock
	@Ignore
	public final void loadGtfsThreeRoutes() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet routesResult = Mockito.mock(ResultSet.class);
			when(routesResult.next()).thenReturn(true, true, true, false);
			// ID
			when(routesResult.getString(1)).thenReturn("i1", "i2", "i3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Agency
			when(routesResult.getString(2)).thenReturn(null, "a2", "a3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Type
			when(routesResult.getInt(3)).thenReturn(1, 2, 3).thenThrow(
					new UnsupportedOperationException("unexpected " + "stub call"));
			// Type
			when(routesResult.wasNull()).thenReturn(//
					false, // 1st route
					false, // 2nd route
					false) // 3rd route
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(ROUTES_SQL_QUERY, routesResult);
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadRoutes(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addRoute("i1", null, ModeOfTransport.UNDERGROUND, "", "", "");
		verify(handler).addRoute("i2", "a2", ModeOfTransport.TRAIN, "", "", "");
		verify(handler).addRoute("i3", "a3", ModeOfTransport.BUS, "", "", "");
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport trip date intervals information.
	 * <p>
	 * The testing scenario contains no data. This tests appropriate handling of no data.
	 */
	@Test
	public final void loadGtfsNoDateInterval() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadDateIntervals(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verifyZeroInteractions(handler);
	}

	/**
	 * Test the loading of the public transport trip date intervals information.
	 * <p>
	 * The testing scenario contains only three date intervals. This tests appropriate handling of date intervals.
	 */
	@Test
	public final void loadGtfsThreeDateIntervals() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet dateIntervalsResult = Mockito.mock(ResultSet.class);
			when(dateIntervalsResult.next()).thenReturn(true, true, true, false);
			// ID
			when(dateIntervalsResult.getString(1)).thenReturn("i1", "i2", "i3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Start
			when(dateIntervalsResult.getDate(2)).thenReturn(Date.valueOf("2010-01-01"), Date.valueOf("2010-07-24"),
					Date.valueOf("2010-12-31")).thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// End
			when(dateIntervalsResult.getDate(3)).thenReturn(Date.valueOf("2015-01-01"), Date.valueOf("2015-07-24"),
					Date.valueOf("2015-12-31")).thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Monday
			when(dateIntervalsResult.getBoolean(4)).thenReturn(false, false, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Tuesday
			when(dateIntervalsResult.getBoolean(5)).thenReturn(false, false, true).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Wednesday
			when(dateIntervalsResult.getBoolean(6)).thenReturn(false, true, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Thursday
			when(dateIntervalsResult.getBoolean(7)).thenReturn(false, true, true).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Friday
			when(dateIntervalsResult.getBoolean(8)).thenReturn(true, false, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Saturday
			when(dateIntervalsResult.getBoolean(9)).thenReturn(true, false, true).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Sunday
			when(dateIntervalsResult.getBoolean(10)).thenReturn(true, true, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
			when(dateIntervalsResult.wasNull()).thenReturn(//
					// 1st interval
					false, false, false, false, false, false, false,
					// 2nd interval
					false, false, false, false, false, false, false,
					// 3rd interval
					false, false, false, false, false, false, false)//
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, dateIntervalsResult);
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadDateIntervals(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addDateInterval("i1", LocalDate.of(2010, 1, 1), LocalDate.of(2015, 1, 2),
										Arrays.asList(false, false, false, false, true, true, true));
		verify(handler).addDateInterval("i2", LocalDate.of(2010, 7, 24), LocalDate.of(2015, 7, 25),
				Arrays.asList(false, false, true, true, false, false, true));
		verify(handler).addDateInterval("i3", LocalDate.of(2010, 12, 31), LocalDate.of(2016, 1, 1),
				Arrays.asList(false, true, false, true, false, true, false));
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport trip date intervals information.
	 * <p>
	 * The testing scenario contains only three date intervals and date pruning. This tests appropriate handling of
	 * date
	 * pruning.
	 */
	@Test
	public final void loadGtfsThreeDateIntervalsWithPruning() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet dateIntervalsResult = Mockito.mock(ResultSet.class);
			when(dateIntervalsResult.next()).thenReturn(true, true, true, false);
			// ID
			when(dateIntervalsResult.getString(1)).thenReturn("i1", "i2", "i3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Start
			when(dateIntervalsResult.getDate(2)).thenReturn(Date.valueOf("2010-01-01"), Date.valueOf("2010-07-24"),
					Date.valueOf("2010-12-31")).thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// End
			when(dateIntervalsResult.getDate(3)).thenReturn(Date.valueOf("2015-01-01"), Date.valueOf("2015-07-24"),
					Date.valueOf("2015-12-31")).thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Monday
			when(dateIntervalsResult.getBoolean(4)).thenReturn(false, false, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Tuesday
			when(dateIntervalsResult.getBoolean(5)).thenReturn(false, false, true).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Wednesday
			when(dateIntervalsResult.getBoolean(6)).thenReturn(false, true, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Thursday
			when(dateIntervalsResult.getBoolean(7)).thenReturn(false, true, true).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Friday
			when(dateIntervalsResult.getBoolean(8)).thenReturn(true, false, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Saturday
			when(dateIntervalsResult.getBoolean(9)).thenReturn(true, false, true).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Sunday
			when(dateIntervalsResult.getBoolean(10)).thenReturn(true, true, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
			when(dateIntervalsResult.wasNull()).thenReturn(//
					// 1st interval
					false, false, false, false, false, false, false,
					// 2nd interval
					false, false, false, false, false, false, false,
					// 3rd interval
					false, false, false, false, false, false, false)//
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY_WITH_PRUNING, dateIntervalsResult);
			builder.stubQuery(DATES_SQL_QUERY_WITH_PRUNING, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoaderWithPruning(connection);
		try {
			loader.loadDateIntervals(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addDateInterval("i1", LocalDate.of(2010, 7, 24), LocalDate.of(2015, 1, 2),
				Arrays.asList(false, false, false, false, true, true, true));
		verify(handler).addDateInterval("i2", LocalDate.of(2010, 7, 24), LocalDate.of(2015, 7, 24),
				Arrays.asList(false, false, true, true, false, false, true));
		verify(handler).addDateInterval("i3", LocalDate.of(2010, 12, 31), LocalDate.of(2015, 7, 24),
				Arrays.asList(false, true, false, true, false, true, false));
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport trip dates information.
	 * <p>
	 * The testing scenario contains no data. This tests appropriate handling of no data.
	 */
	@Test
	public final void loadGtfsNoDate() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadDates(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verifyZeroInteractions(handler);
	}

	/**
	 * Test the loading of the public transport trip dates information.
	 * <p>
	 * The testing scenario contains only three dates (two positive and one negative). This tests appropriate handling
	 * of dates.
	 */
	@Test
	public final void loadGtfsThreeDates() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet datesResult = Mockito.mock(ResultSet.class);
			when(datesResult.next()).thenReturn(true, true, true, false);
			// ID
			when(datesResult.getString(1)).thenReturn("i1", "i2", "i3").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Date
			when(datesResult.getDate(2)).thenReturn(Date.valueOf("2010-01-01"), Date.valueOf("2013-07-24"),
					Date.valueOf("2015-12-31")).thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Type
			when(datesResult.getInt(3)).thenReturn(1, 1, 2).thenThrow(
					new UnsupportedOperationException("unexpected " + "stub call"));
			// Type
			when(datesResult.wasNull()).thenReturn(//
					false, // 1st date
					false, // 2nd date
					false) // 3rd date
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(DATES_SQL_QUERY, datesResult);
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadDates(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addDate("i1", LocalDate.of(2010, 1, 1));
		verify(handler).addDate("i2", LocalDate.of(2013, 7, 24));
		verify(handler).removeDate("i3", LocalDate.of(2015, 12, 31));
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport departures information.
	 * <p>
	 * The testing scenario contains no data. This tests appropriate handling of no data.
	 */
	// TODO : update mock
	@Ignore
	public final void loadGtfsNoDeparture() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DEPARTURES_SQL_QUERY, Mockito.mock(ResultSet.class));
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadDepartures(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verifyZeroInteractions(handler);
	}

	/**
	 * Test the loading of the public transport departures information.
	 * <p>
	 * The testing scenario contains only five departures. This tests appropriate handling of departures (including
	 * {@code null} instead of some values, frequencies and times greater than 24).
	 */
	// TODO : update mock
	@Ignore
	public final void loadGtfsFiveDepartures() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet departuresResult = Mockito.mock(ResultSet.class);
			when(departuresResult.next()).thenReturn(true, true, true, true, true, true, true, true, true, false);
			// ID
			when(departuresResult.getString(1)).thenReturn("i1", "i1", "i2", "i2", "i3", "i3", "i3", "i3",
					"i3").thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Order
			when(departuresResult.getInt(2)).thenReturn(0, 1, 0, 4, 3, 6, 6, 7, 8).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Stop
			when(departuresResult.getString(3)).thenReturn("s11", "s12", "s21", "s22", "s31", "s32", "s41", "s42",
					"s43").thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Route
			when(departuresResult.getString(4)).thenReturn("r1", "r1", "r2", "r2", "r3", "r3", "r3", "r3",
					"r3").thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Service
			when(departuresResult.getString(5)).thenReturn("d1", "d1", "d2", "d2", "d3", "d3", "d3", "d3",
					"d3").thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Distance (1st, 2nd, 5th and 6th value is NULL)
			when(departuresResult.getDouble(6)).thenReturn(0., 0., 4., 4., 0., 0., 0., 1., 8.).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Arrival time
			when(departuresResult.getString(7)).thenReturn("01:01:00", "02:01:00", "23:02:00", "54:02:00", "05:03:00",
					"06:03:00", "07:04:00", "38:04:00", "39:04:00").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Departure time
			when(departuresResult.getString(8)).thenReturn("01:01:01", "02:01:01", "23:02:01", "54:02:01", "05:03:01",
					"06:03:01", "07:04:02", "38:04:52", "39:04:01").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Frequency start
			when(departuresResult.getString(9)).thenReturn(null, null, null, null, "00:00:00", "00:00:00", "00:00:01",
					"00:00:01", "00:00:01").thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Frequency end
			when(departuresResult.getString(10)).thenReturn(null, null, null, null, "25:00:00", "25:00:00", "05:00:00",
					"05:00:00", "05:00:00").thenThrow(new UnsupportedOperationException("unexpected stub " + "call"));
			// Headway in seconds (1st, 2nd, 3rd and 4th value is NULL)
			when(departuresResult.getInt(11)).thenReturn(0, 0, 0, 0, 7200, 7200, 3600, 3600, 3600).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Exact times (1st, 2nd, 3rd and 4th value is NULL)
			when(departuresResult.getBoolean(12)).thenReturn(false, false, false, false, true, true, true, true,
					true).thenThrow(new UnsupportedOperationException("unexpected stub call"));
			// Order, distance, headway, exact times
			when(departuresResult.wasNull()).thenReturn(//
					false, true, true, true, // 1st stop time
					false, true, true, true, // 2nd stop time
					false, false, true, true, // 3rd stop time
					false, false, true, true, // 4th stop time
					false, true, false, false, // 5th stop time
					false, true, false, false, // 6th stop time
					false, false, false, false, // 7th stop time
					false, false, false, false, // 8th stop time
					false, false, false, false) // 9th stop time
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(DEPARTURES_SQL_QUERY, departuresResult);
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoader(connection);
		try {
			loader.loadDepartures(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addDepartures("s11", "s12", "r1", "d1", "i1", "h1",
				Duration.ofHours(1).plusMinutes(1).withSeconds(1), Duration.ofMillis(1),
				Duration.ofHours(1).plusMinutes(1).withSeconds(1).plusMillis(1), null, null,
				Duration.ofHours(1).withSeconds(-1));
		verify(handler).addDepartures("s21", "s22", "r2", "d2", "i2", "h2",
				Duration.ofHours(23).plusMinutes(2).withSeconds(1), Duration.ofMillis(1),
				Duration.ofHours(23).plusMinutes(2).withSeconds(1).plusMillis(1), null, 0.,
				Duration.ofHours(31).withSeconds(-1));
		verify(handler).addDepartures("s31", "s32", "r3", "d3", "i3", "h3", Duration.ofHours(0), Duration.ofSeconds(7200),
				Duration.ofHours(25), true, null, Duration.ofHours(1).withSeconds(-1));
		verify(handler).addDepartures("s41", "s42", "r3", "d3", "i3", "h3", Duration.ofSeconds(1), Duration.ofSeconds(3600),
				Duration.ofHours(5), true, 1., Duration.ofHours(31).withSeconds(-2));
		verify(handler).addDepartures("s42", "s43", "r3", "d3", "i3", "h3", Duration.ofHours(31).withSeconds(51),
									  Duration.ofSeconds(3600), Duration.ofHours(36).withSeconds(50), true, 7., Duration.ofHours(1).withSeconds(-52));
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Test the loading of the public transport departures information.
	 * <p>
	 * The testing scenario contains only two departures with different units conversion. This tests appropriate
	 * handling of units conversion.
	 */
	// TODO : change mock
	@Ignore
	public final void loadGtfsTwoDeparturesWithAnotherUnits() {
		final Connection connection = mock(Connection.class);
		try {
			final StatementStubBuilder builder = new StatementStubBuilder();
			builder.stubQuery(AGENCIES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(STOPS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(ROUTES_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATE_INTERVALS_SQL_QUERY, Mockito.mock(ResultSet.class));
			builder.stubQuery(DATES_SQL_QUERY, Mockito.mock(ResultSet.class));

			final ResultSet departuresResult = Mockito.mock(ResultSet.class);
			when(departuresResult.next()).thenReturn(true, true, true, false);
			// ID
			when(departuresResult.getString(1)).thenReturn("i1", "i1", "i1").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Order
			when(departuresResult.getInt(2)).thenReturn(0, 1, 8).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Stop
			when(departuresResult.getString(3)).thenReturn("s11", "s12", "s13").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Route
			when(departuresResult.getString(4)).thenReturn("r1", "r1", "r1").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Service
			when(departuresResult.getString(5)).thenReturn("d1", "d1", "d1").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Distance
			when(departuresResult.getDouble(6)).thenReturn(0., 1., 4.).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Arrival time
			when(departuresResult.getString(7)).thenReturn("01:01:00", "02:01:00", "03:01:00").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Departure time
			when(departuresResult.getString(8)).thenReturn("01:01:01", "02:01:01", "03:01:01").thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Frequency start
			when(departuresResult.getString(9)).thenReturn(null, null, null).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Frequency end
			when(departuresResult.getString(10)).thenReturn(null, null, null).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Headway in seconds (1st, 2nd and 3rd value is NULL)
			when(departuresResult.getInt(11)).thenReturn(0, 0, 0).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Exact times (1st, 2nd and 3rd value is NULL)
			when(departuresResult.getBoolean(12)).thenReturn(false, false, false).thenThrow(
					new UnsupportedOperationException("unexpected stub call"));
			// Order, distance, headway, exact times
			when(departuresResult.wasNull()).thenReturn(//
					false, false, true, true, // 1st stop time
					false, false, true, true, // 2nd stop time
					false, false, true, true) // 3rd stop time
					.thenThrow(new UnsupportedOperationException("unexpected stub call"));
			builder.stubQuery(DEPARTURES_SQL_QUERY, departuresResult);
			final Statement statement = builder.flushToStatement();

			when(connection.createStatement()).thenReturn(statement);
		} catch (final SQLException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		final GTFSDataHandler handler = mock(GTFSDataHandler.class);

		final GTFSDatabaseLoader loader = createDefaultLoaderWithAnotherUnits(connection);
		try {
			loader.loadDepartures(handler);
		} catch (final GtfsException ex) {
			ex.printStackTrace();
			fail(String.format("unexpected exception: %s", ex));
			return;
		}

		verify(handler).addDepartures("s11", "s12", "r1", "d1", "i1", "h1",
				Duration.ofHours(1).plusMinutes(1).withSeconds(1), Duration.ofMillis(1),
				Duration.ofHours(1).plusMinutes(1).withSeconds(1).plusMillis(1), null, 3., Duration.ofHours(1).withSeconds
						(-1));
		verify(handler).addDepartures("s12", "s13", "r1", "d1", "i1", "h1",
				Duration.ofHours(2).plusMinutes(1).withSeconds(1), Duration.ofMillis(1),
				Duration.ofHours(2).plusMinutes(1).withSeconds(1).plusMillis(1), null, 9., Duration.ofHours(1).withSeconds
						(-1));
		verifyNoMoreInteractions(handler);
	}

	/**
	 * Get argument matcher of projected GPS location equality.
	 *
	 * @param location
	 * 		Target projected GPS location.
	 *
	 * @return The argument matcher.
	 */
	private static GPSLocation projectedEq(final GPSLocation location) {
		if (location == null) {
			throw new NullPointerException("bad location");
		}
		return argThat(new ProjectedLocationMatcher(location));
	}

	/**
	 * Create new default instance of database loader.
	 *
	 * @param connection
	 * 		Connection to a database.
	 *
	 * @return The instance.
	 */
	private static GTFSDatabaseLoader createDefaultLoader(final Connection connection) {
		if (connection == null) {
			throw new NullPointerException("bad connection");
		}
		return new GTFSDatabaseLoaderDefaultImpl(connection, DEFAULT_EPSG_SRID,
				DEFAULT_GTFS_UNIT_TO_METERS_MULTIPLIER);
	}

	/**
	 * Create new instance of database loader with bad EPSG SRID.
	 *
	 * @param connection
	 * 		Connection to a database.
	 *
	 * @return The instance.
	 */
	private static GTFSDatabaseLoader createDefaultLoaderWithBadEpsg(final Connection connection) {
		if (connection == null) {
			throw new NullPointerException("bad connection");
		}
		return new GTFSDatabaseLoaderDefaultImpl(connection, BAD_EPSG_SRID, DEFAULT_GTFS_UNIT_TO_METERS_MULTIPLIER);
	}

	/**
	 * Create new instance of database loader with configured pruning.
	 *
	 * @param connection
	 * 		Connection to a database.
	 *
	 * @return The instance.
	 */
	private static GTFSDatabaseLoader createDefaultLoaderWithPruning(final Connection connection) {
		if (connection == null) {
			throw new NullPointerException("bad connection");
		}
		return new GTFSDatabaseLoaderDefaultImpl(connection, DEFAULT_EPSG_SRID, DEFAULT_GTFS_UNIT_TO_METERS_MULTIPLIER,
				ANOTHER_PRUNE_BEFORE_DATE, ANOTHER_PRUNE_AFTER_DATE);
	}

	/**
	 * Create new instance of database loader with another GTFS units.
	 *
	 * @param connection
	 * 		Connection to a database.
	 *
	 * @return The instance.
	 */
	private static GTFSDatabaseLoader createDefaultLoaderWithAnotherUnits(final Connection connection) {
		if (connection == null) {
			throw new NullPointerException("bad connection");
		}
		return new GTFSDatabaseLoaderDefaultImpl(connection, DEFAULT_EPSG_SRID,
				DEFAULT_GTFS_UNIT_TO_METERS_MULTIPLIER + 2);
	}

	/**
	 * Builder of SQL statement stubs.
	 * <p>
	 * Constructed statements throws readable exceptions in case of unexpected SQL queries.
	 *
	 * @author Radek Holy
	 */
	private static final class StatementStubBuilder {

		/**
		 * The constructed statement stub.
		 */
		private Statement statement = mock(Statement.class);

		/**
		 * All SQL queries with known responses.
		 */
		private final Collection<String> stubbedQueries = new HashSet<>();

		/**
		 * Add response for given SQL query.
		 *
		 * @param query
		 * 		SQL query.
		 * @param resultSet
		 * 		Response to be returned.
		 */
		public final void stubQuery(final String query, final ResultSet resultSet) {
			try {
				when(statement.executeQuery(query)).thenReturn(resultSet);
			} catch (final SQLException ex) {
				ex.printStackTrace();
				fail(String.format("unexpected exception: %s", ex));
			}
			stubbedQueries.add(query);
		}

		/**
		 * Get the stub and clear the builder.
		 *
		 * @return The statement stub.
		 */
		// The state must be reset, because external modifications of returned
		// structures can cause inconsistencies in the builder.
		public final Statement flushToStatement() {
			final Statement returnValue = statement;

			final String notAnyStubbedQuery = argThat(new NotAnyOfMatcher<>(stubbedQueries));
			final Throwable exception = new UnsupportedOperationException(
					"Unexpected SQL query. Check whether the " + "loader's queries have not changed.");

			try {
				when(returnValue.executeQuery(notAnyStubbedQuery)).thenThrow(exception);
			} catch (final SQLException ex) {
				ex.printStackTrace();
				fail(String.format("Unexpected exception: %s.", ex));
			}

			statement = mock(Statement.class);
			stubbedQueries.clear();

			return returnValue;
		}
	}

	/**
	 * Matcher of arguments not in given collection.
	 *
	 * @param <T>
	 * 		Type of arguments.
	 *
	 * @author Radek Holy
	 */
	private static final class NotAnyOfMatcher<T> extends ArgumentMatcher<T> {

		/**
		 * The collection.
		 */
		private final Collection<T> values;

		/**
		 * Construct a new instance.
		 *
		 * @param values
		 * 		A collection.
		 */
		public NotAnyOfMatcher(final Collection<T> values) {
			if (values == null) {
				throw new NullPointerException("bad values");
			}
			this.values = ImmutableList.copyOf(values);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final boolean matches(final Object argument) {
			return !this.values.contains(argument);
		}
	}

	/**
	 * Matcher of arguments equal to given projected GPS location.
	 *
	 * @author Radek Holy
	 */
	private static final class ProjectedLocationMatcher extends ArgumentMatcher<GPSLocation> {

		/**
		 * The projected GPS location.
		 */
		private final GPSLocation location;

		/**
		 * Construct a new instance.
		 *
		 * @param location
		 * 		A projected GPS location.
		 */
		public ProjectedLocationMatcher(final GPSLocation location) {
			if (location == null) {
				throw new NullPointerException("bad location");
			}
			this.location = location;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final boolean matches(final Object argument) {
			if (argument == null) return false;
			if (argument == location) return true;
			if (argument.getClass() != location.getClass()) return false;

			final GPSLocation rhs = (GPSLocation) argument;
			return Objects.equals(rhs, location) && Objects.equals(rhs.latProjected,
					location.latProjected) && Objects.equals(rhs.lonProjected, location.lonProjected);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void describeTo(final Description description) {
			if (description == null) {
				throw new NullPointerException("bad description");
			}
			description.appendText(location.toString());
		}
	}

	/**
	 * Default implementation of loader.
	 *
	 * @author Radek Holy
	 */
	private static final class GTFSDatabaseLoaderDefaultImpl extends GTFSDatabaseLoader {

		/**
		 * Construct a new instance.
		 *
		 * @param connection
		 * 		Connection to a database.
		 * @param epsgSrid
		 * 		EPSG SRID of coordinates projection.
		 * @param gtfsUnitToMetersMultiplier
		 * 		A number used to multiply traveled distance specified in GTFS data to convert it to meters.
		 */
		public GTFSDatabaseLoaderDefaultImpl(final Connection connection, final int epsgSrid,
											 final double gtfsUnitToMetersMultiplier) {
			super(connection, epsgSrid, gtfsUnitToMetersMultiplier, 0, Date.valueOf("1000-01-01"),
					Date.valueOf("9999-12-31"));
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
		 * @param pruneBeforeDate
		 * 		Min. allowed loaded date (inclusive). This setting should be used rarely to accelerate GTFS loading
		 * 		time.
		 * 		In other cases, a GTFS graph filtering mechanism should be used.
		 * @param pruneAfterDate
		 * 		Max. allowed loaded date (exclusive). This setting should be used rarely to accelerate GTFS loading
		 * 		time.
		 * 		In other cases, a GTFS graph filtering mechanism should be used.
		 */
		public GTFSDatabaseLoaderDefaultImpl(final Connection connection, final int epsgSrid,
											 final double gtfsUnitToMetersMultiplier, final Date pruneBeforeDate,
											 final Date pruneAfterDate) {
			super(connection, epsgSrid, gtfsUnitToMetersMultiplier, 0, pruneBeforeDate, pruneAfterDate);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void handleParseException(String message, Throwable exception) throws GtfsParseException {
			throw new GtfsParseException(message, exception);
		}
	}
}
