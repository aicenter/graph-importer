package cz.agents.gtdgraphimporter.gtfs;

import com.google.common.collect.ImmutableList;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.GraphBuilder;
import cz.agents.basestructures.Node;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.additional.WheelchairBoarding;
import cz.agents.multimodalstructures.edges.InnerEdge;
import cz.agents.multimodalstructures.edges.RouteConstantEdge;
import cz.agents.multimodalstructures.edges.RouteEdge;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
import cz.agents.multimodalstructures.nodes.RouteNode;
import cz.agents.multimodalstructures.nodes.StopNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

/**
 * Unit test suite of public transport graphs builder. WARNING: most of the tests are implementation specific - they
 * don't count with isomorphism therefore they have to create expected nodes with appropriate IDs
 *
 * @author Radek Holy
 * @author Marek Cuchý
 */
public final class GTFSGraphBuilderTest {

	/**
	 * Time zone of the testing scenario.
	 * <p>
	 * It should be a time zone of a country using daylight saving time and it should have different offset than {@link
	 * #CURRENT_TIMEZONE}. The testing scenario (mainly the test {@link #flushToGraphDstDeparture()}) expects the
	 * Helsinki time zone.
	 */
	private static final String TEST_TIMEZONE = "Europe/Helsinki";

	/**
	 * Time zone of the test runner.
	 * <p>
	 * It should have different offset than {@link #TEST_TIMEZONE}. The testing scenario (mainly the test {@link
	 * #flushToGraphDstDeparture()}) expects the Prague time zone.
	 */
	private static final String CURRENT_TIMEZONE = "Europe/Prague";

	/**
	 * Default ID of new nodes.
	 */
	private static final int INITIAL_NODE_ID = 0;

	/**
	 * Default source ID of new nodes.
	 */
	private static final long INITIAL_SOURCE_NODE_ID = 111111111111L;

	/**
	 * Duration of getting on in seconds.
	 */
	private static final short GET_ON_DURATION_IN_SECONDS = 20;

	/**
	 * Duration of getting off in seconds.
	 */
	private static final short GET_OFF_DURATION_IN_SECONDS = 10;

	private static final LocalDate EPOCH_START = LocalDate.of(2002, 1, 1);

	/**
	 * Backup of the default time zone.
	 */
	private static TimeZone defaultTimeZone;

	private Graph<Node, TimeDependentEdge> expected;

	//	private TimetableStorage timetableStorage;

	/**
	 * Set the appropriate time zone for the test runner.
	 * <p>
	 * It is mainly important for the test {@link #flushToGraphDstDeparture()}.
	 */
	@BeforeClass
	public static void setTimezone() {
		// Backup.
		defaultTimeZone = TimeZone.getDefault();

		// Configure.
		TimeZone.setDefault(TimeZone.getTimeZone(CURRENT_TIMEZONE));
	}

	@Before
	public void setUp() {
		expected = createBasicExpectedGraph();
	}

	/**
	 * Restore the default time zone.
	 */
	@AfterClass
	public static void restoreTimezone() {
		TimeZone.setDefault(defaultTimeZone);
	}

	/**
	 * Test the agency adding.
	 * <p>
	 * The testing scenario contains two agencies with same ID. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addAgencyTwoSameIds() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency("i", null, ZoneOffset.ofHours(0), null, null);
		actualBuilder.addAgency("i", null, ZoneOffset.ofHours(1), null, null);
	}

	/**
	 * Test the agency adding.
	 * <p>
	 * The testing scenario contains two agencies with null ID. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addAgencyTwoNullIds() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneOffset.ofHours(0), null, null);
		actualBuilder.addAgency(null, null, ZoneOffset.ofHours(1), null, null);
	}

	/**
	 * Test the agency adding.
	 * <p>
	 * The testing scenario contains one agency and then one agency with null ID. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addAgencyIdBeforeNullId() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency("i0", null, ZoneOffset.ofHours(0), null, null);
		actualBuilder.addAgency(null, null, ZoneOffset.ofHours(1), null, null);
	}

	/**
	 * Test the agency adding.
	 * <p>
	 * The testing scenario contains one agency with null ID and then one another agency. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addAgencyNullIdBeforeId() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneOffset.ofHours(0), null, null);
		actualBuilder.addAgency("i1", null, ZoneOffset.ofHours(1), null, null);
	}

	/**
	 * Test the stop adding.
	 * <p>
	 * The testing scenario contains two stops with same ID. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addStopTwoSameIds() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addStop("i", "c0", "n0", "d0",
							  new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 + INITIAL_NODE_ID + 0, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("i", "c1", "n1", "d1",
							  new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 + INITIAL_NODE_ID + 1, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
	}

	/**
	 * Test the route adding.
	 * <p>
	 * The testing scenario contains two routes with same ID. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addRouteTwoSameIds() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addRoute("i", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addRoute("i", "a1", ModeOfTransport.FERRY, "", "", "");
	}

	/**
	 * Test the date interval adding.
	 * <p>
	 * The testing scenario contains two intervals with same ID. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addDateIntervalTwoSameIds() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addDateInterval("i", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDateInterval("i", LocalDate.of(2003, 1, 1), LocalDate.of(2003, 12, 31),
									  ImmutableList.of(true, true, true, true, true, false, false));
	}

	/**
	 * Test the date interval adding.
	 * <p>
	 * The testing scenario contains one interval with start date after end date. This tests adding failure.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void addDateIntervalBadDates() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addDateInterval("i", LocalDate.of(2002, 12, 31), LocalDate.of(2002, 1, 1),
									  ImmutableList.of(true, true, true, true, true, true, true));
	}

	/**
	 * Test the date interval adding.
	 * <p>
	 * The testing scenario contains one interval with bad number of week flags. This tests adding failure.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void addDateIntervalBadFlags() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addDateInterval("i", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true));
	}

	/**
	 * Test the positive date exception adding.
	 * <p>
	 * The testing scenario contains one negative exception and then same positive exception. This tests adding
	 * failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addDateAfterNegative() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.removeDate("i", LocalDate.of(2002, 1, 1));
		actualBuilder.addDate("i", LocalDate.of(2002, 1, 1));
	}

	/**
	 * Test the negative date exception adding.
	 * <p>
	 * The testing scenario contains one positive exception and then same negative exception. This tests adding
	 * failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void removeDateAfterPositive() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addDate("i", LocalDate.of(2002, 1, 1));
		actualBuilder.removeDate("i", LocalDate.of(2002, 1, 1));
	}

	/**
	 * Test the departures adding.
	 * <p>
	 * The testing scenario contains one departure from undefined stop. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addDeparturesBadStop() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("s0", "c0", "n0", "de0", new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 +
																								  INITIAL_NODE_ID + 0,
																	   0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("s1", "c1", "n1", "de1", new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 +
																								  INITIAL_NODE_ID + 1,
																	   0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("d0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDate("d0", LocalDate.of(2003, 7, 23));
		actualBuilder.removeDate("d0", LocalDate.of(2002, 7, 23));
		actualBuilder.addDepartures("s00", "s1", "r0", "d0", "t0", "h0", Duration.ofHours(1), Duration.ofHours(1),
									Duration.ofHours(47).plusMinutes(59).plusSeconds(59).plusMillis(999), true, 111.,
									Duration.ofSeconds(11));
	}

	/**
	 * Test the departures adding.
	 * <p>
	 * The testing scenario contains one departure of undefined route. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addDeparturesBadRoute() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("s0", "c0", "n0", "d0",
							  new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 + INITIAL_NODE_ID + 0, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("s1", "c1", "n1", "d1",
							  new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 + INITIAL_NODE_ID + 1, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("d0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDate("d0", LocalDate.of(2003, 7, 23));
		actualBuilder.removeDate("d0", LocalDate.of(2002, 7, 23));
		actualBuilder.addDepartures("s0", "s1", "r1", "d0", "t0", "h0", Duration.ofHours(1), Duration.ofHours(1),
									Duration.ofHours(47).plusMinutes(59).plusSeconds(59).plusMillis(999), true, 111.,
									Duration.ofSeconds(11));
	}

	/**
	 * Test the departures adding.
	 * <p>
	 * The testing scenario contains one departure of ambiguous route. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addDeparturesAmbiguousRoute() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency("a0", null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addAgency("a1", null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("s0", "c0", "n0", "d0",
							  new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 + INITIAL_NODE_ID + 0, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("s1", "c1", "n1", "d1",
							  new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 + INITIAL_NODE_ID + 1, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("d0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDate("d0", LocalDate.of(2003, 7, 23));
		actualBuilder.removeDate("d0", LocalDate.of(2002, 7, 23));
		actualBuilder.addDepartures("s0", "s1", "r0", "d0", "t0", "h0", Duration.ofHours(1), Duration.ofHours(1),
									Duration.ofHours(47).plusMinutes(59).plusSeconds(59).plusMillis(999), true, 111.,
									Duration.ofSeconds(11));
	}

	/**
	 * Test the departures adding.
	 * <p>
	 * The testing scenario contains one departure of undefined dates. This tests adding failure.
	 */
	@Test(expected = IllegalStateException.class)
	public final void addDeparturesBadDates() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("s0", "c0", "n0", "de0", new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 +
																								  INITIAL_NODE_ID + 0,
																	   0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("s1", "c1", "n1", "de1", new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 +
																								  INITIAL_NODE_ID + 1,
																	   0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("d0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDate("d0", LocalDate.of(2003, 7, 23));
		actualBuilder.removeDate("d0", LocalDate.of(2002, 7, 23));
		actualBuilder.addDepartures("s0", "s1", "r0", "d1", "t0", "h0", Duration.ofHours(1), Duration.ofHours(1),
									Duration.ofHours(47).plusMinutes(59).plusSeconds(59).plusMillis(999), true, 111.,
									Duration.ofSeconds(11));
	}

	/**
	 * Test the departures adding.
	 * <p>
	 * The testing scenario contains one departure with start time after end time. This tests adding failure.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void addDeparturesBadTimes() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("s0", "c0", "n0", "de0", new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 +
																								  INITIAL_NODE_ID + 0,
																	   0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("s1", "c1", "n1", "de1", new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 +
																								  INITIAL_NODE_ID + 1,
																	   0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("d0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDate("d0", LocalDate.of(2003, 7, 23));
		actualBuilder.removeDate("d0", LocalDate.of(2002, 7, 23));
		actualBuilder.addDepartures("s0", "s1", "r0", "d0", "t0", "h0",
									Duration.ofHours(47).plusMinutes(59).plusSeconds(59).plusMillis(999),
									Duration.ofHours(1), Duration.ofHours(1), true, 111., Duration.ofSeconds(11));

	}

	/**
	 * Test the departures adding.
	 * <p>
	 * The testing scenario contains one departure with negative time period. This tests adding failure.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void addDeparturesNegativePeriod() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("s0", "c0", "n0", "de0", new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 +
																								  INITIAL_NODE_ID + 0,
																	   0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("s1", "c1", "n1", "de1", new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 +
																								  INITIAL_NODE_ID + 1,
																	   0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("d0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDate("d0", LocalDate.of(2003, 7, 23));
		actualBuilder.removeDate("d0", LocalDate.of(2002, 7, 23));
		actualBuilder.addDepartures("s0", "s1", "r0", "d0", "t0", "h0", Duration.ofHours(1), Duration.ofHours(-1),
									Duration.ofHours(47).plusMinutes(59).plusSeconds(59).plusMillis(999), true, 111.,
									Duration.ofSeconds(11));

	}

	/**
	 * Test the departures adding.
	 * <p>
	 * The testing scenario contains one departure with zero time period. This tests adding failure.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void addDeparturesZeroPeriod() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("s0", "c0", "n0", "de0", new GPSLocation(100 + INITIAL_NODE_ID + 0, 100 +
																								  INITIAL_NODE_ID + 0,
																	   0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("s1", "c1", "n1", "de1", new GPSLocation(100 + INITIAL_NODE_ID + 1, 100 +
																								  INITIAL_NODE_ID + 1,
																	   0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("d0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31),
									  ImmutableList.of(true, true, true, true, true, true, true));
		actualBuilder.addDate("d0", LocalDate.of(2003, 7, 23));
		actualBuilder.removeDate("d0", LocalDate.of(2002, 7, 23));
		actualBuilder.addDepartures("s0", "s1", "r0", "d0", "t0", "h0", Duration.ofHours(1), Duration.ZERO,
									Duration.ofHours(47).plusMinutes(59).plusSeconds(59).plusMillis(999), true, 111.,
									Duration.ofSeconds(11));
	}

	/**
	 * Test the graph construction and builder cleaning.
	 * <p>
	 * The testing scenario contains no graph. This tests building of empty graph.
	 */
	@Test
	public final void flushToGraphNothing() {
		final Graph<Node, TimeDependentEdge> expected = createEmptyGraph();

		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();

		assertEquals("bad nodes imported", new HashSet<>(expected.getAllNodes()), new HashSet<>(actual.getAllNodes()));
		assertEquals("bad edges imported", new HashSet<>(expected.getAllEdges()), new HashSet<>(actual.getAllEdges()));
	}

	/**
	 * Test the graph construction.
	 * <p>
	 * The testing scenario contains departure defined by two dates and two times. This tests appropriate handling of
	 * single dates and single times.
	 */
	@Test
	public final void flushToGraphTwoDatesTwoTimesDepartures() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("i0", "c0", "n0", "d0", new GPSLocation(10, 10, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("i1", "c1", "n1", "d1", new GPSLocation(11, 11, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDate("s0", LocalDate.of(2002, 1, 1));
		actualBuilder.addDate("s0", LocalDate.of(2002, 1, 6));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t0", "h0", Duration.ofHours(12), Duration.ofHours(1),
									Duration.ofHours(13), true, 111., Duration.ofSeconds(11));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t1", "h0", Duration.ofHours(14), Duration.ofHours(1),
									Duration.ofHours(15), true, 111., Duration.ofSeconds(11));
		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();

		assertEquals("bad nodes imported", new HashSet<>(expected.getAllNodes()), new HashSet<>(actual.getAllNodes()));
		assertEquals("bad edges imported", new HashSet<>(expected.getAllEdges()), new HashSet<>(actual.getAllEdges()));
	}

	private static int getSeconds(int days, int hours, int minutes, int seconds) {
		return days * 24 * 3600 + hours * 3600 + minutes * 60 + seconds;
	}

	/**
	 * Test the graph construction and builder cleaning.
	 * <p>
	 * The testing scenario contains departure defined by two-dates interval and two-times interval. This tests
	 * appropriate handling of date itervals and time intervals.
	 */
	@Test
	public final void flushToGraphTwoDatesIntervalTwoTimesIntervalDepartures() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("i0", "c0", "n0", "d0", new GPSLocation(10, 10, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("i1", "c1", "n1", "d1", new GPSLocation(11, 11, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("s0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 1, 8),
									  ImmutableList.of(false, true, false, false, false, false, true));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t0", "h0", Duration.ofHours(12), Duration.ofHours(2),
									Duration.ofHours(15), true, 111., Duration.ofSeconds(11));

		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();

		assertEquals("bad nodes imported", new HashSet<>(expected.getAllNodes()), new HashSet<>(actual.getAllNodes()));
		assertEquals("bad edges imported", new HashSet<>(expected.getAllEdges()), new HashSet<>(actual.getAllEdges()));
	}

	/**
	 * Test the graph construction and builder cleaning.
	 * <p>
	 * The testing scenario contains departure defined by two-dates interval, two exceptions and two-times interval.
	 * This tests appropriate handling of date interval exceptions.
	 */
	@Test
	public final void flushToGraphTwoDatesIntervalWithExceptionsTwoTimesIntervalDepartures() {
		final Graph<Node, TimeDependentEdge> expected = createBasicExpectedGraph();

		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("i0", "c0", "n0", "d0", new GPSLocation(10, 10, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("i1", "c1", "n1", "d1", new GPSLocation(11, 11, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDateInterval("s0", LocalDate.of(2002, 1, 1), LocalDate.of(2002, 1, 8),
									  ImmutableList.of(false, true, false, true, false, false, false));
		actualBuilder.addDate("s0", LocalDate.of(2002, 1, 6));
		actualBuilder.removeDate("s0", LocalDate.of(2002, 1, 3));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t0", "h0", Duration.ofHours(12), Duration.ofHours(2),
									Duration.ofHours(15), true, 111., Duration.ofSeconds(11));
		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();

		assertEquals("bad nodes imported", new HashSet<>(expected.getAllNodes()), new HashSet<>(actual.getAllNodes()));
		assertEquals("bad edges imported", new HashSet<>(expected.getAllEdges()), new HashSet<>(actual.getAllEdges()));
	}

	/**
	 * Test the graph construction and builder cleaning.
	 * <p>
	 * The testing scenario contains departure that span multiple days. This tests appropriate handling of times
	 * greater
	 * than 24 hours.
	 */
	@Test
	public final void flushToGraphLongDeparture() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("i0", "c0", "n0", "d0", new GPSLocation(10, 10, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("i1", "c1", "n1", "d1", new GPSLocation(11, 11, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDate("s0", LocalDate.of(2002, 1, 1));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t0", "h0", Duration.ofHours(12), Duration.ofHours(1),
									Duration.ofHours(15), true, 111., Duration.ofHours(23));

		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();
		assertEquals(1, actual.getAllEdges().stream().filter(e -> e instanceof RouteEdge).count());
		for (TimeDependentEdge edge : actual.getAllEdges()) {
			if (edge instanceof RouteEdge) {
				assertEquals(getSeconds(1, 11, 0, 0), edge.getArrival(0, 0));
			}
		}
	}

	/**
	 * Test the graph construction.
	 * <p>
	 * The testing scenario contains varying travel times between stations.
	 */
	@Test
	public final void flushToGraphVaryingTravelTimes() {
		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("i0", "c0", "n0", "d0", new GPSLocation(10, 10, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("i1", "c1", "n1", "d1", new GPSLocation(11, 11, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDate("s0", LocalDate.of(2002, 1, 1));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t0", "h0", Duration.ofHours(12), Duration.ofHours(1),
									Duration.ofHours(13), true, 111., Duration.ofHours(23));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t1", "h0", Duration.ofHours(13), Duration.ofHours(1),
									Duration.ofHours(14), true, 111., Duration.ofHours(24));

		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();
		List<TimeDependentEdge> routeEdges = actual.getAllEdges()
												   .stream()
												   .filter(e -> e instanceof RouteEdge)
												   .collect(toList());
		assertEquals(1, routeEdges.size());
		TimeDependentEdge edge = routeEdges.get(0);
		assertEquals(getSeconds(0, 35, 0, 0), edge.getArrival(0, 0));
		assertEquals(getSeconds(0, 37, 0, 0), edge.getArrival(getSeconds(0, 12, 30, 0), 0));
	}

	/**
	 * Test the graph construction and builder cleaning.
	 * <p>
	 * The testing scenario contains departure which does not exist in Prague time zone (due to daylight savings) and
	 * exists in Helsinki time zone. This tests appropriate handling of time zones.
	 * <p>
	 * Appropriate time zone is set by {@link #setTimezone()}.
	 */
	@Test
	public final void flushToGraphDstDeparture() {
		final StopNode[] stopNodes = new StopNode[]{
				createStopNode(0, 10, 10, WheelchairBoarding.AT_LEAST_SOME_VEHICLES),
				createStopNode(1, 11, 11, WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION)};
		final RouteNode[] routeNodes = new RouteNode[]{
				createRouteNode(2, stopNodes[0], "r0"), createRouteNode(3, stopNodes[1], "r0")};

		final List<TimeDependentEdge> innerEdges = new ArrayList<>();
		innerEdges.addAll(createInnerEdges(stopNodes[0], routeNodes[0]));
		innerEdges.addAll(createInnerEdges(stopNodes[1], routeNodes[1]));

		int[] departures = new int[]{
				getSeconds(0, 2, 15, 0)};

		final RouteEdge[] routeEdges = new RouteEdge[]{
				new RouteConstantEdge(routeNodes[0].id, routeNodes[1].id, ModeOfTransport.BUS, departures,
									  getSeconds(0, 0, 15, 0), 111)};

		final GraphBuilder<Node, TimeDependentEdge> expectedBuilder = new GraphBuilder<>();
		expectedBuilder.addNodes(Arrays.asList(stopNodes));
		expectedBuilder.addNodes(Arrays.asList(routeNodes));
		expectedBuilder.addEdges(Arrays.asList(routeEdges));
		expectedBuilder.addEdges(innerEdges);
		final Graph<Node, TimeDependentEdge> expected = expectedBuilder.createGraph();

		final GTFSGraphBuilder actualBuilder = new GTFSGraphBuilder(INITIAL_SOURCE_NODE_ID, GET_ON_DURATION_IN_SECONDS,
																	GET_OFF_DURATION_IN_SECONDS,
																	LocalDate.of(2013, 3, 31));
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addStop("i0", "c0", "n0", "d0", new GPSLocation(10, 10, 0, 0), "z0",
							  WheelchairBoarding.AT_LEAST_SOME_VEHICLES);
		actualBuilder.addStop("i1", "c1", "n1", "d1", new GPSLocation(11, 11, 0, 0), "z1",
							  WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDate("s0", LocalDate.of(2013, 3, 31));
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t0", "h0", Duration.ofHours(2).plusMinutes(15),
									Duration.ofHours(1), Duration.ofHours(3).plusMinutes(15), true, 111.,
									Duration.ofMinutes(15));
		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();

		assertEquals("bad nodes imported", new HashSet<>(expected.getAllNodes()), new HashSet<>(actual.getAllNodes()));
		assertEquals("bad edges imported", new HashSet<>(expected.getAllEdges()), new HashSet<>(actual.getAllEdges()));
	}

	/**
	 * Test the graph construction and builder cleaning.
	 * <p>
	 * The testing scenario contains departure whose start time is equal to end time. This tests that no departure is
	 * created.
	 */
	@Test
	public final void flushToGraphEqualStartEndDeparture() {
		final Graph<Node, TimeDependentEdge> expected = createEmptyGraph();

		final GTFSGraphBuilder actualBuilder = createDefaultBuilder();
		actualBuilder.addAgency(null, null, ZoneId.of(TEST_TIMEZONE), null, null);
		actualBuilder.addRoute("r0", null, ModeOfTransport.BUS, "", "", "");
		actualBuilder.addDate("s0", LocalDate.of(2013, 3, 31));
		actualBuilder.addStop("i0", null, null, null, null, null, null);
		actualBuilder.addStop("i1", null, null, null, null, null, null);
		actualBuilder.addDepartures("i0", "i1", "r0", "s0", "t0", "h0", Duration.ofHours(2).plusMinutes(15),
									Duration.ofHours(1), Duration.ofHours(3).plusMinutes(-45), true, 111.,
									Duration.ofMinutes(15));
		final Graph<Node, TimeDependentEdge> actual = actualBuilder.flushToGraph();

		assertEquals("bad nodes imported", new HashSet<>(expected.getAllNodes()), new HashSet<>(actual.getAllNodes()));
		assertEquals("bad edges imported", new HashSet<>(expected.getAllEdges()), new HashSet<>(actual.getAllEdges()));
	}

	private static List<InnerEdge> createInnerEdges(StopNode stopNode, RouteNode routeNode) {
		return Arrays.asList(createOnInnerEdge(stopNode, routeNode), createOffInnerEdge(stopNode, routeNode));
	}

	private static InnerEdge createOffInnerEdge(StopNode stopNode, RouteNode routeNode) {
		return new InnerEdge(routeNode.id, stopNode.id, GET_OFF_DURATION_IN_SECONDS);
	}

	private static InnerEdge createOnInnerEdge(StopNode stopNode, RouteNode routeNode) {
		return new InnerEdge(stopNode.id, routeNode.id, GET_ON_DURATION_IN_SECONDS);
	}

	private static RouteNode createRouteNode(int id, StopNode stopNode, String routeId) {
		return new RouteNode(INITIAL_NODE_ID + id, INITIAL_SOURCE_NODE_ID + id, stopNode.latE6, stopNode.lonE6,
							 stopNode.latProjected, stopNode.lonProjected, stopNode.elevation, stopNode, routeId);
	}

	private static StopNode createStopNode(int id, int latE6, int lonE6, WheelchairBoarding wheelchairBoarding) {
		return new StopNode(INITIAL_NODE_ID + id, INITIAL_SOURCE_NODE_ID + id, latE6, lonE6, 0, 0, 0, "i" + id,
							"n" + id, "z" + id, wheelchairBoarding);
	}

	/**
	 * Construct new default instance of the builder.
	 *
	 * @return The instance.
	 */
	private static GTFSGraphBuilder createDefaultBuilder() {
		return new GTFSGraphBuilder(INITIAL_SOURCE_NODE_ID, GET_ON_DURATION_IN_SECONDS, GET_OFF_DURATION_IN_SECONDS,
									EPOCH_START);
	}

	/**
	 * Construct new instance of empty graph.
	 *
	 * @return The graph.
	 */
	private static Graph<Node, TimeDependentEdge> createEmptyGraph() {
		return new GraphBuilder<Node, TimeDependentEdge>().createGraph();
	}

	private static Graph<Node, TimeDependentEdge> createBasicExpectedGraph() {
		final StopNode[] stopNodes = new StopNode[]{
				createStopNode(0, 10, 10, WheelchairBoarding.AT_LEAST_SOME_VEHICLES),
				createStopNode(1, 11, 11, WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION)};
		final RouteNode[] routeNodes = new RouteNode[]{
				createRouteNode(2, stopNodes[0], "r0"), createRouteNode(3, stopNodes[1], "r0")};

		final List<TimeDependentEdge> innerEdges = new ArrayList<>();
		innerEdges.addAll(createInnerEdges(stopNodes[0], routeNodes[0]));
		innerEdges.addAll(createInnerEdges(stopNodes[1], routeNodes[1]));

		int[] departures = new int[]{
				getSeconds(0, 12, 0, 0), getSeconds(0, 14, 0, 0), getSeconds(5, 12, 0, 0), getSeconds(5, 14, 0, 0)};

		final RouteEdge[] routeEdges = new RouteEdge[]{
				new RouteConstantEdge(routeNodes[0].id, routeNodes[1].id, ModeOfTransport.BUS, departures, 11, 111)};

		final GraphBuilder<Node, TimeDependentEdge> expectedBuilder = new GraphBuilder<>();
		expectedBuilder.addNodes(Arrays.asList(stopNodes));
		expectedBuilder.addNodes(Arrays.asList(routeNodes));
		expectedBuilder.addEdges(Arrays.asList(routeEdges));
		expectedBuilder.addEdges(innerEdges);
		return expectedBuilder.createGraph();
	}
}
