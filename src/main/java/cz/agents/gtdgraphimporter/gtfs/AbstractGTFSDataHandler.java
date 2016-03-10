package cz.agents.gtdgraphimporter.gtfs;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import cz.agents.basestructures.GPSLocation;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.additional.WheelchairBoarding;
import org.javatuples.Triplet;
import org.joda.time.*;

import java.util.*;

/**
 * Handler for data that come from the GTFS database.
 */
public abstract class AbstractGTFSDataHandler implements GTFSDataHandler {

	/**
	 * Time zone for agency IDs (possibly {@code null}).
	 */
	final Map<String, DateTimeZone> agenciesTimeZones = new HashMap<>();

	/**
	 * Details for agencies (name, URL, phone) identified by Agency ID
	 */
	final Map<String, Triplet<String, String, String>> agenciesToDetails = new HashMap<>();

	/**
	 * Date interval for service IDs (possibly {@code null}).
	 */
	final Map<String, Dates> servicesToDates = new HashMap<>();

	/**
	 * Dates to be included for service IDs (possibly {@code null}).
	 */
	final Map<String, Collection<LocalDate>> servicesToDatesIncluded = new HashMap<>();

	/**
	 * Dates to be excluded for service IDs (possibly {@code null}).
	 */
	final Map<String, Collection<LocalDate>> servicesToDatesExcluded = new HashMap<>();

	/**
	 * Stops by their IDs.
	 */
	final Map<String, Stop> stops = new HashMap<>();

	/**
	 * Public transport mode for route IDs (possibly {@code null}).
	 */
	final Map<String, Route> routes = new HashMap<>();

	/**
	 * Add new public transport agency.
	 *
	 * @param id
	 * 		An identifier of the agency. Possibly {@code null}, but then only one agency can be set at all.
	 * @param timeZone
	 * 		A time zone of times provided by the agency.
	 */
	@Override
	public final void addAgency(final String id, final String agencyName, final DateTimeZone timeZone,
								final String agencyUrl, final String agencyPhone) {
		if (timeZone == null) {
			throw new NullPointerException("bad time zone");
		}
		if (agenciesTimeZones.containsKey(id) && !agenciesTimeZones.get(id).equals(timeZone)) {
			throw new IllegalStateException("agency already present with different time zone");
		}
		if (id == null && !agenciesTimeZones.isEmpty()) {
			throw new IllegalStateException("if the ID is null, only one agency can be set at all");
		}
		if (agenciesTimeZones.containsKey(null)) {
			throw new IllegalStateException("if an agency with null ID was added, only one agency can be set at all");
		}
		agenciesTimeZones.put(id, timeZone);

		Triplet<String, String, String> details = new Triplet<>(agencyName, agencyUrl, agencyPhone);
		Triplet<String, String, String> inMap = agenciesToDetails.get(id);
		if (inMap == null) {
			agenciesToDetails.put(id, details);
		} else {
			if (!details.equals(inMap)) {
				throw new IllegalStateException(String.format(
						"Agency already present with different details! " + "Agency ID : [%s], in map : [%s], incoming" +
								" : [%s]",
						id, inMap, details));
			}
		}
	}

	/**
	 * Add new stop.
	 *
	 * @param id
	 * 		An identifier of the stop. Possibly {@code null}.
	 * @param code
	 * 		A code of the stop. Possibly {@code null}.
	 * @param name
	 * 		A name of the stop. Possibly {@code null}.
	 * @param description
	 * 		A description of the stop. Possibly {@code null}.
	 * @param location
	 * 		A location of the stop. Possibly {@code null}.
	 * @param zone
	 * 		A zone of the stop. Possibly {@code null}.
	 * @param wheelchairBoarding
	 * 		A wheel chair boarding availability at the stop. Possibly {@code null}.
	 */
	@Override
	public final void addStop(final String id, final String code, final String name, final String description,
							  final GPSLocation location, final String zone,
							  final WheelchairBoarding wheelchairBoarding) {

		Stop newStop = new Stop(id, code, name, description, location, zone, wheelchairBoarding);
		if (stops.containsKey(id)) {
			Stop oldStop = stops.get(id);
			if (!oldStop.equals(newStop)) {
				throw new IllegalStateException("stop already present with different attributes");
			}
		}
		stops.put(id, newStop);
	}

	/**
	 * Add new route.
	 *
	 * @param id
	 * 		An identifier of the route. Possibly {@code null}.
	 * @param agency
	 * 		An agency providing the route. Possibly {@code null}.
	 * @param mode
	 * 		A mode of transport used on the route.
	 * @param routeShortName
	 * 		A short name for the route.
	 * @param routeLongName
	 * 		A long name for the route.
	 * @param routeDescription
	 * 		Description for the route.
	 */
	@Override
	public final void addRoute(final String id, final String agency, final ModeOfTransport mode,
							   final String routeShortName, final String routeLongName, final String
										   routeDescription) {
		if (mode == null) {
			throw new NullPointerException("bad mode");
		}

		Route details = routes.get(id);

		Triplet<String, String, String> agencyDetails = agenciesToDetails.get(agency);
		if (agencyDetails == null) {
			agencyDetails = new Triplet<>(null, null, null);
		}

		Route newData = new Route(agency, agencyDetails.getValue0(), agencyDetails.getValue1(),
				agencyDetails.getValue2(), mode, routeShortName, routeLongName, routeDescription);
		if (details == null) {
			routes.put(id, newData);
		} else {
			if (!details.equals(newData)) {
				throw new IllegalStateException(
						"The route is already present with different data! \n Data before : [" + details.toString() +
								"], new data : [" + newData.toString() + "]");
			} else {
				routes.put(id, newData);
			}
		}
	}

	/**
	 * Add new left-closed date interval.
	 *
	 * @param id
	 * 		An identifier of the interval. Possibly {@code null}.
	 * @param start
	 * 		Left endpoint of the interval.
	 * @param end
	 * 		Right endpoint of the interval.
	 * @param isAvailableInDay
	 * 		Flag for each day in week indicating whether the service is available. Index of each flag plus one is
	 * 		equal to
	 * 		the number of each day as they are defined in {@link org.joda.time.DateTimeConstants}.
	 */
	@Override
	public final void addDateInterval(final String id, final LocalDate start, final LocalDate end,
									  final List<Boolean> isAvailableInDay) {
		if (start == null) {
			throw new NullPointerException("bad start");
		}
		if (end == null) {
			throw new NullPointerException("bad end");
		}
		if (start.isAfter(end)) {
			throw new IllegalArgumentException("start cannot be after end");
		}
		if (isAvailableInDay == null) {
			throw new NullPointerException("bad week flags");
		}
		if (isAvailableInDay.size() != DateTimeConstants.DAYS_PER_WEEK) {
			throw new IllegalArgumentException("bad number of week flags");
		}
		if (isAvailableInDay.contains(null)) {
			throw new NullPointerException("bad week flag");
		}

		final Dates interval = new Dates(start, end, isAvailableInDay);
		if (servicesToDates.containsKey(id) && !servicesToDates.get(id).equals(interval)) {
			throw new IllegalStateException("service already present with different dates");
		}

		servicesToDates.put(id, interval);
	}

	/**
	 * Add positive exception to date interval.
	 *
	 * @param id
	 * 		An identifier of the interval. Possibly {@code null}.
	 * @param date
	 * 		Date of the exception.
	 */
	@Override
	public final void addDate(final String id, final LocalDate date) {
		if (date == null) {
			throw new NullPointerException("bad date");
		}
		if (servicesToDatesExcluded.containsKey(id) && servicesToDatesExcluded.get(id).contains(date)) {
			throw new IllegalStateException("date already marked to be excluded");
		}

		final Collection<LocalDate> dates;
		if (servicesToDatesIncluded.containsKey(id)) {
			dates = servicesToDatesIncluded.get(id);
		} else {
			dates = new HashSet<>();
			servicesToDatesIncluded.put(id, dates);
		}
		dates.add(new LocalDate(date));
	}

	/**
	 * Add negative exception to date interval.
	 *
	 * @param id
	 * 		An identifier of the interval. Possibly {@code null}.
	 * @param date
	 * 		Date of the exception.
	 */
	@Override
	public final void removeDate(final String id, final LocalDate date) {
		if (date == null) {
			throw new NullPointerException("bad date");
		}
		if (servicesToDatesIncluded.containsKey(id) && servicesToDatesIncluded.get(id).contains(date)) {
			throw new IllegalStateException("date already marked to be included");
		}

		final Collection<LocalDate> dates;
		if (servicesToDatesExcluded.containsKey(id)) {
			dates = servicesToDatesExcluded.get(id);
		} else {
			dates = new HashSet<>();
			servicesToDatesExcluded.put(id, dates);
		}
		dates.add(new LocalDate(date));
	}

	/**
	 * Creates a collection of Departures from the given parameters
	 *
	 * @param route
	 * 		An identifier of a route of the journey.
	 * @param service
	 * 		An identifier of a service dates of the journey.
	 * @param tripId
	 * 		An identifier of the journey.
	 * @param tripHeadsign
	 * 		An user-presentable label for the final stop
	 * @param startTime
	 * 		A time instant (inclusively) at which the departures starts.
	 * @param timePeriod
	 * 		A time period after which the departures repeats.
	 * @param endTime
	 * 		A time instant (exclusively) at which the departures ends.
	 * @param travelTime
	 * 		Duration of the journey.
	 *
	 * @return Collection of Departures
	 */
	public final List<Departure> createDepartures(final String route, final String service, final String tripId,
												  final String tripHeadsign, final ReadablePeriod startTime,
												  final ReadablePeriod timePeriod, final ReadablePeriod endTime,
												  final ReadablePeriod travelTime) {

		List<Departure> departures = new ArrayList<>();

		if (tripId == null) {
			throw new NullPointerException("bad trip");
		}

		final DateTimeZone timeZone = getFinalTimeZone(route);
		if (timeZone == null) {
			throw new IllegalStateException("no time zone set for given route");
		}

		final Iterable<LocalDate> dates = getFinalDates(service);

		if (dates == null) {
			throw new IllegalStateException("no dates set for given service");
		}

		if (!dates.iterator().hasNext()) {
			return Collections.emptyList();
		}

		final DateTime aValidDate = dates.iterator().next().toDateTimeAtStartOfDay(timeZone);
		final DateTime aValidStart = aValidDate.plus(startTime);
		final DateTime aValidEnd = aValidDate.plus(endTime);
		if (aValidStart.isAfter(aValidEnd)) {
			throw new IllegalArgumentException("start cannot be after end");
		}
		if (aValidStart.equals(aValidEnd)) {
			// "If [...] a start_time equal to end_time, no trip must be scheduled."
			return Collections.emptyList();
		}
		if (!aValidStart.plus(timePeriod).isAfter(aValidStart)) {
			throw new IllegalArgumentException("time period must be positive");
		}

		for (final LocalDate date : dates) {
			final DateTime midnight = date.toDateTimeAtStartOfDay(timeZone);
			final DateTime startDateTime = midnight.plus(startTime);
			final DateTime endDateTime = midnight.plus(endTime);
			for (DateTime time = startDateTime; time.isBefore(endDateTime); time = time.plus(timePeriod)) {
				departures.add(new Departure(time, travelTime, tripId, WheelchairBoarding.NO_ACCESSIBILITY_INFORMATION,
						BikeBoarding.NO_ACCESSIBILITY_INFORMATION, tripHeadsign));
			}
		}

		return departures;
	}

	/**
	 * Get time zone for given route.
	 * <p>
	 * It takes into account all rules for {@code null} agency IDs.
	 *
	 * @param route
	 * 		An identifier of the route. Possibly {@code null}.
	 *
	 * @return The time zone or {@code null} if no agency is set for the route, no time zone is set for the agency or
	 * the agency is ambiguous.
	 */
	private DateTimeZone getFinalTimeZone(final String route) {
		if (!routes.containsKey(route)) {
			return null;
		}

		final String agency = routes.get(route).agencyId;
		if (agency != null) {
			if (!agenciesTimeZones.containsKey(agency)) {
				return null;
			}
			return agenciesTimeZones.get(agency);
		} else {
			if (agenciesTimeZones.size() != 1) {
				return null;
			}
			return agenciesTimeZones.values().iterator().next();
		}
	}

	/**
	 * Get all date for given service ID.
	 * <p>
	 * It takes into account the date intervals, dates to be included and dates to be excluded.
	 *
	 * @param service
	 * 		An identifier of the dates. Possibly {@code null}.
	 *
	 * @return The dates.
	 */
	private Iterable<LocalDate> getFinalDates(final String service) {
		if (!servicesToDates.containsKey(service) && !servicesToDatesIncluded.containsKey(service)) {
			return null;
		}

		// Remove dates in interval from dates to be included.
		final Iterable<LocalDate> datesIncluded = servicesToDatesIncluded.containsKey(
				service) ? servicesToDatesIncluded.get(service) : Collections.<LocalDate>emptyList();
		final Predicate<LocalDate> notInIntervalFilter = servicesToDates.containsKey(service) ? new DateNotInDates(
				servicesToDates.get(service)) : new DateNotInCollection(Collections.<LocalDate>emptyList());
		final Iterable<LocalDate> datesIncludedNotInInterval = Iterables.filter(datesIncluded, notInIntervalFilter);

		// Remove excluded dates from dates in interval.
		final Iterable<LocalDate> datesInterval = servicesToDates.containsKey(service) ? servicesToDates.get(
				service) : Collections.<LocalDate>emptyList();
		final Predicate<LocalDate> notExcludedFilter = servicesToDatesExcluded.containsKey(
				service) ? new DateNotInCollection(servicesToDatesExcluded.get(service)) : new DateNotInCollection(
				Collections.<LocalDate>emptyList());
		final Iterable<LocalDate> datesIntervalNotExcluded = Iterables.filter(datesInterval, notExcludedFilter);

		return Iterables.concat(datesIncludedNotInInterval, datesIntervalNotExcluded);

	}

	/**
	 * Dates defined by interval and days in week.
	 *
	 * @author Radek Holy
	 */
	private final static class Dates implements Iterable<LocalDate> {

		/**
		 * Left endpoint of the interval (inclusively).
		 */
		public final LocalDate start;

		/**
		 * Right endpoint of the interval (exclusively).
		 */
		public final LocalDate end;

		/**
		 * Flag for each day in week indicating whether the service is available. Index of each flag plus one is equal
		 * to the number of each day as they are defined in {@link org.joda.time.DateTimeConstants}.
		 */
		public final List<Boolean> isAvailableInDay;

		/**
		 * Construct a new instance.
		 *
		 * @param start
		 * 		Left endpoint of the interval (inclusively).
		 * @param end
		 * 		Right endpoint of the interval (exclusively).
		 * @param isAvailableInDay
		 * 		Flag for each day in week indicating whether the service is available. Index of each flag plus one is
		 * 		equal
		 * 		to the number of each day as they are defined in {@link org.joda.time.DateTimeConstants}.
		 */
		public Dates(final LocalDate start, final LocalDate end, final List<Boolean> isAvailableInDay) {
			super();

			if (start == null) {
				throw new NullPointerException("bad start");
			}
			if (end == null) {
				throw new NullPointerException("bad end");
			}
			if (start.isAfter(end)) {
				throw new IllegalArgumentException("start cannot be after end");
			}
			if (isAvailableInDay == null) {
				throw new NullPointerException("bad week flags");
			}
			if (isAvailableInDay.size() != DateTimeConstants.DAYS_PER_WEEK) {
				throw new IllegalArgumentException("flags for some days miss");
			}
			if (isAvailableInDay.contains(null)) {
				throw new NullPointerException("bad week flag");
			}

			this.start = start;
			this.end = end;
			this.isAvailableInDay = ImmutableList.copyOf(isAvailableInDay);
		}

		/**
		 * Test whether a date is one of the dates.
		 *
		 * @param date
		 * 		A date.
		 *
		 * @return The test result.
		 */
		public final boolean contains(final LocalDate date) {
			if (date == null) {
				throw new NullPointerException("bad date");
			}

			final int day = date.getDayOfWeek() - 1;

			return (isAvailableInDay.get(day) && !isBefore(date) && !isAfter(date));
		}

		/**
		 * Test whether all dates are before a date.
		 *
		 * @param date
		 * 		A date.
		 *
		 * @return The test result.
		 */
		public final boolean isBefore(final LocalDate date) {
			if (date == null) {
				throw new NullPointerException("bad date");
			}

			return !date.isBefore(end);
		}

		/**
		 * Test whether all dates are after a date.
		 *
		 * @param date
		 * 		A date.
		 *
		 * @return The test result.
		 */
		public final boolean isAfter(final LocalDate date) {
			if (date == null) {
				throw new NullPointerException("bad date");
			}

			return date.isBefore(start);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final Iterator<LocalDate> iterator() {
			return new DatesIterator(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final boolean equals(final Object obj) {
			if (obj == null) return false;
			if (obj == this) return true;
			if (obj.getClass() != getClass()) return false;

			final Dates rhs = (Dates) obj;
			return Objects.equals(start, rhs.start) && Objects.equals(end, rhs.end) && Objects.equals(isAvailableInDay,
					rhs.isAvailableInDay);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final int hashCode() {
			return Objects.hash(start, end, isAvailableInDay);
		}
	}

	/**
	 * Iterator over dates defined by an interval and days in week.
	 *
	 * @author Radek Holy
	 */
	private final static class DatesIterator implements Iterator<LocalDate> {

		/**
		 * Period between each pair of dates.
		 */
		private static final ReadablePeriod period = Period.days(1);

		/**
		 * The date interval.
		 */
		private final Dates dates;

		/**
		 * Next date to be returned.
		 */
		private LocalDate next;

		/**
		 * Construct a new instance.
		 *
		 * @param dates
		 * 		A date interval.
		 */
		public DatesIterator(final Dates dates) {
			super();

			if (dates == null) {
				throw new NullPointerException("bad dates");
			}

			this.dates = dates;
			this.next = dates.contains(dates.start) ? dates.start : findNext(dates.start);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final boolean hasNext() {
			return next != null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final LocalDate next() {
			if (!hasNext()) {
				throw new NoSuchElementException("the iteration has no more elements");
			}

			final LocalDate current = next;
			next = findNext(next);
			return current;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void remove() {
			throw new UnsupportedOperationException("the remove operation is not supported");
		}

		/**
		 * Find next date in the interval.
		 *
		 * @param start
		 * 		Date from which the search should start. Possibly {@code null}.
		 *
		 * @return The next date or {@code null}.
		 */
		private LocalDate findNext(final LocalDate start) {
			LocalDate date = start != null ? start : dates.start;
			while (!dates.isBefore(date)) {
				date = date.plus(period);
				if (dates.contains(date)) {
					return date;
				}
			}
			return null;
		}
	}

	/**
	 * Predicate testing whether a date is in a collection.
	 *
	 * @author Radek Holy
	 */
	private final static class DateNotInCollection implements Predicate<LocalDate> {

		/**
		 * The collection of dates.
		 */
		private final Collection<LocalDate> collection;

		/**
		 * Construct a new instance.
		 *
		 * @param collection
		 * 		A collection.
		 */
		public DateNotInCollection(final Collection<LocalDate> collection) {
			if (collection == null) {
				throw new NullPointerException("bad collection");
			}

			this.collection = collection;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final boolean apply(final LocalDate date) {
			if (date == null) {
				throw new NullPointerException("bad date");
			}

			return !collection.contains(date);
		}
	}

	/**
	 * Predicate testing whether a date is in a date interval.
	 *
	 * @author Radek Holy
	 */
	private final static class DateNotInDates implements Predicate<LocalDate> {

		/**
		 * The interval.
		 */
		private final Dates dates;

		/**
		 * Construct a new instance.
		 *
		 * @param dates
		 * 		A date interval.
		 */
		public DateNotInDates(final Dates dates) {
			if (dates == null) {
				throw new NullPointerException("bad dates");
			}

			this.dates = dates;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final boolean apply(final LocalDate date) {
			if (date == null) {
				throw new NullPointerException("bad date");
			}

			return !dates.contains(date);
		}
	}

	protected final static class Stop {

		public final String id;
		public final String code;
		public final String name;
		public final String description;
		public final GPSLocation location;
		public final String zoneId;
		public final WheelchairBoarding wheelchairBoarding;

		public Stop(String id, String code, String name, String description, GPSLocation location, String zoneId,
					WheelchairBoarding wheelchairBoarding) {
			this.id = id;
			this.code = code;
			this.name = name;
			this.description = description;
			this.location = location;
			this.zoneId = zoneId;
			this.wheelchairBoarding = wheelchairBoarding;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Stop stop = (Stop) o;

			if (id != null ? !id.equals(stop.id) : stop.id != null) return false;
			if (code != null ? !code.equals(stop.code) : stop.code != null) return false;
			if (name != null ? !name.equals(stop.name) : stop.name != null) return false;
			if (description != null ? !description.equals(stop.description) : stop.description != null) return false;
			if (location != null ? !location.equals(stop.location) : stop.location != null) return false;
			if (zoneId != null ? !zoneId.equals(stop.zoneId) : stop.zoneId != null) return false;
			return wheelchairBoarding == stop.wheelchairBoarding;

		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (code != null ? code.hashCode() : 0);
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (description != null ? description.hashCode() : 0);
			result = 31 * result + (location != null ? location.hashCode() : 0);
			result = 31 * result + (zoneId != null ? zoneId.hashCode() : 0);
			result = 31 * result + (wheelchairBoarding != null ? wheelchairBoarding.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "Stop [" +
					"id='" + id + '\'' +
					", code='" + code + '\'' +
					", name='" + name + '\'' +
					", description='" + description + '\'' +
					", location=" + location +
					", zoneId='" + zoneId + '\'' +
					", wheelchairBoarding=" + wheelchairBoarding +
					']';
		}
	}

	protected final static class Route {

		public final String agencyId;
		public final String agencyName;
		public final String agencyUrl;
		public final String agencyPhone;
		public final ModeOfTransport ptMode;
		public final String routeShortName;
		public final String routeLongName;
		public final String routeDescription;

		public Route(String agencyId, String agencyName, String agencyUrl, String agencyPhone, ModeOfTransport ptMode,
					 String routeShortName, String routeLongName, String routeDescription) {
			this.agencyId = agencyId;
			this.agencyName = agencyName;
			this.agencyUrl = agencyUrl;
			this.agencyPhone = agencyPhone;
			this.ptMode = ptMode;
			this.routeShortName = routeShortName;
			this.routeLongName = routeLongName;
			this.routeDescription = routeDescription;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Route route = (Route) o;

			if (agencyId != null ? !agencyId.equals(route.agencyId) : route.agencyId != null) return false;
			if (agencyName != null ? !agencyName.equals(route.agencyName) : route.agencyName != null) return false;
			if (agencyUrl != null ? !agencyUrl.equals(route.agencyUrl) : route.agencyUrl != null) return false;
			if (agencyPhone != null ? !agencyPhone.equals(route.agencyPhone) : route.agencyPhone != null) return false;
			if (ptMode != route.ptMode) return false;
			if (routeShortName != null ? !routeShortName.equals(route.routeShortName) : route.routeShortName != null)
				return false;
			if (routeLongName != null ? !routeLongName.equals(route.routeLongName) : route.routeLongName != null)
				return false;
			return routeDescription != null ? routeDescription.equals(
					route.routeDescription) : route.routeDescription == null;

		}

		@Override
		public int hashCode() {
			int result = agencyId != null ? agencyId.hashCode() : 0;
			result = 31 * result + (agencyName != null ? agencyName.hashCode() : 0);
			result = 31 * result + (agencyUrl != null ? agencyUrl.hashCode() : 0);
			result = 31 * result + (agencyPhone != null ? agencyPhone.hashCode() : 0);
			result = 31 * result + (ptMode != null ? ptMode.hashCode() : 0);
			result = 31 * result + (routeShortName != null ? routeShortName.hashCode() : 0);
			result = 31 * result + (routeLongName != null ? routeLongName.hashCode() : 0);
			result = 31 * result + (routeDescription != null ? routeDescription.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "Route [" +
					"agencyId='" + agencyId + '\'' +
					", agencyName='" + agencyName + '\'' +
					", agencyUrl='" + agencyUrl + '\'' +
					", agencyPhone='" + agencyPhone + '\'' +
					", ptMode=" + ptMode +
					", routeShortName='" + routeShortName + '\'' +
					", routeLongName='" + routeLongName + '\'' +
					", routeDescription='" + routeDescription + '\'' +
					']';
		}
	}

	protected static class Departure {

		public final DateTime departureTime;
		public final ReadablePeriod travelTime;
		public final String tripId;
		public final WheelchairBoarding wheelchairBoarding;
		public final BikeBoarding bikeBoarding;
		public final String tripHeadsign;

		public Departure(DateTime departureTime, ReadablePeriod travelTime, String tripId,
						 WheelchairBoarding wheelchairBoarding, BikeBoarding bikeBoarding, String tripHeadsign) {
			this.departureTime = departureTime;
			this.travelTime = travelTime;
			this.tripId = tripId;
			this.wheelchairBoarding = wheelchairBoarding;
			this.bikeBoarding = bikeBoarding;
			this.tripHeadsign = tripHeadsign;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Departure departure1 = (Departure) o;

			if (departureTime != null ? !departureTime.equals(
					departure1.departureTime) : departure1.departureTime != null) return false;
			if (travelTime != null ? !travelTime.equals(departure1.travelTime) : departure1.travelTime != null)
				return false;
			if (tripId != null ? !tripId.equals(departure1.tripId) : departure1.tripId != null) return false;
			if (wheelchairBoarding != departure1.wheelchairBoarding) return false;
			if (bikeBoarding != departure1.bikeBoarding) return false;
			return tripHeadsign != null ? tripHeadsign.equals(
					departure1.tripHeadsign) : departure1.tripHeadsign == null;

		}

		@Override
		public int hashCode() {
			int result = departureTime != null ? departureTime.hashCode() : 0;
			result = 31 * result + (travelTime != null ? travelTime.hashCode() : 0);
			result = 31 * result + (tripId != null ? tripId.hashCode() : 0);
			result = 31 * result + (wheelchairBoarding != null ? wheelchairBoarding.hashCode() : 0);
			result = 31 * result + (bikeBoarding != null ? bikeBoarding.hashCode() : 0);
			result = 31 * result + (tripHeadsign != null ? tripHeadsign.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "Departure [" +
					"departureTime=" + departureTime +
					", arrivalTime=" + travelTime +
					", tripId='" + tripId + '\'' +
					", wheelchairBoarding=" + wheelchairBoarding +
					", bikeBoarding=" + bikeBoarding +
					", tripHeadsign='" + tripHeadsign + '\'' +
					']';
		}
	}
}
