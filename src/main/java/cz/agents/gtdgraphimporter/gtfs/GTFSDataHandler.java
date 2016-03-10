package cz.agents.gtdgraphimporter.gtfs;

import cz.agents.basestructures.GPSLocation;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.additional.WheelchairBoarding;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.ReadablePeriod;

import java.util.List;

/**
 * Handler of data loaded by GTFS loaders.
 * 
 * @author Radek Holy
 */
public interface GTFSDataHandler {

	/**
	 * Add new public transport agency.
     * @param id
     *            An identifier of the agency. Possibly {@code null}, but then only one agency can be set at all.
     * @param agencyName
     * @param timeZone
     * @param agencyUrl
     * @param agencyPhone
     */
	public abstract void addAgency(final String id, String agencyName, final DateTimeZone timeZone, String agencyUrl, String agencyPhone);

	/**
	 * Add new stop.
	 * 
	 * @param id
	 *            An identifier of the stop. Possibly {@code null}.
	 * @param code
	 *            A code of the stop. Possibly {@code null}.
	 * @param name
	 *            A name of the stop. Possibly {@code null}.
	 * @param description
	 *            A description of the stop. Possibly {@code null}.
	 * @param location
	 *            A location of the stop. Possibly {@code null}.
	 * @param zone
	 *            A zone of the stop. Possibly {@code null}.
	 * @param wheelchairBoarding
	 *            A wheel chair boarding availability at the stop. Possibly {@code null}.
	 */
	public abstract void addStop(final String id, final String code, final String name, final String description,
								 final GPSLocation location, final String zone, final WheelchairBoarding wheelchairBoarding);

	/**
	 * Add new route.
	 * 
	 * @param id
	 *            An identifier of the route. Possibly {@code null}.
	 * @param agency
	 *            An agency providing the route. Possibly {@code null}.
	 * @param mode
	 *            A mode of transport used on the route.
	 */
	public abstract void addRoute(final String id, final String agency, final ModeOfTransport mode, final String
                                  routeShortName, final String routeLongName, final String routeDescription);

	/**
	 * Add new left-closed date interval.
	 * 
	 * @param id
	 *            An identifier of the interval. Possibly {@code null}.
	 * @param start
	 *            Left endpoint of the interval.
	 * @param end
	 *            Right endpoint of the interval.
	 * @param isAvailableInDay
	 *            Flag for each day in week indicating whether the service is available. Index of each flag plus one is
	 *            equal to the number of each day as they are defined in {@link org.joda.time.DateTimeConstants}.
	 */
	public abstract void addDateInterval(final String id, final LocalDate start, final LocalDate end,
			final List<Boolean> isAvailableInDay);

	/**
	 * Add positive exception to date interval.
	 * 
	 * @param id
	 *            An identifier of the interval. Possibly {@code null}.
	 * @param date
	 *            Date of the exception.
	 */
	public abstract void addDate(final String id, final LocalDate date);

	/**
	 * Add negative exception to date interval.
	 * 
	 * @param id
	 *            An identifier of the interval. Possibly {@code null}.
	 * @param date
	 *            Date of the exception.
	 */
	public abstract void removeDate(final String id, final LocalDate date);

	/**
	 * Add departures from a stop.
	 * 
	 * @param origin
	 *            An identifier of a stop of the journey origin.
	 * @param destination
	 *            An identifier of a stop of the journey destination.
	 * @param route
	 *            An identifier of a route of the journey.
	 * @param service
	 *            An identifier of a service dates of the journey.
	 * @param tripId
	 *            An identifier of the journey.
	 * @param startTime
	 *            A time instant (inclusively) at which the departures starts.
	 * @param timePeriod
	 *            A time period after which the departures repeats.
	 * @param endTime
	 *            A time instant (exclusively) at which the departures ends.
	 * @param isExact
	 *            A flag indicating whether the departure frequency is specified exactly.
	 * @param distanceInM
	 *            Length of the journey in meters.
	 * @param travelTime
	 *            Duration of the journey.
	 */
	public abstract void addDepartures(final String origin, final String destination, final String route,
			final String service, final String tripId, final String tripHeadsign, final ReadablePeriod startTime, final ReadablePeriod timePeriod,
			final ReadablePeriod endTime, final Boolean isExact, final Double distanceInM,
			final ReadablePeriod travelTime);

}