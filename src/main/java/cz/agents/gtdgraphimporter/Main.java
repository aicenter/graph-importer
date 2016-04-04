package cz.agents.gtdgraphimporter;

import com.google.common.collect.Sets;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.GTDGraphBuilder.PTSettings;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Marek Cuchý
 */
public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(
			String[] args) throws ParserConfigurationException, SAXException, IOException, FactoryException,
			TransformException, SQLException, GtfsException {
		DOMConfigurator.configure("log4j.xml");

		String osm = "path.osm";
		Connection connection = DriverManager.getConnection("jdbc:postgresql://host:port:port/database", "user", "password");

		//from date, to date - prune settings for PT timetables
		PTSettings ptSettings = new PTSettings("2013-12-02", "2013-12-04");

		//SRID for CZ is 2065
		GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(new Transformer(2065), osm, Sets.immutableEnumSet
				(ModeOfTransport.CAR, ModeOfTransport.BIKE, ModeOfTransport.WALK), ptSettings, connection);

		gtdBuilder.build();
		//		gtdBuilder.buildSimplifiedRoadGraph();
		//		gtdBuilder.buildPtGraph(0);


		printMemory();
	}

	private static void printMemory() {
		int mb = 1024 * 1024;

		//Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();

		LOGGER.debug("##### Heap utilization statistics [MB] #####");

		//Print used memory
		LOGGER.debug("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);

		//Print free memory
		LOGGER.debug("Free Memory:" + runtime.freeMemory() / mb);

		//Print total available memory
		LOGGER.debug("Total Memory:" + runtime.totalMemory() / mb);

		//Print Maximum available memory
		LOGGER.debug("Max Memory:" + runtime.maxMemory() / mb);
	}
}
