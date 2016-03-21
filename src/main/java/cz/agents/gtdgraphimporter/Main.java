package cz.agents.gtdgraphimporter;

import com.google.common.collect.Sets;
import cz.agents.basestructures.Graph;
import cz.agents.geotools.EPSGProjection;
import cz.agents.gtdgraphimporter.GTDGraphBuilder.PTSettings;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;
import cz.agents.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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

		String osm = "pathToOsmFile";
		Connection connection = DriverManager.getConnection("jdbc:postgresql://its.felk.cvut" +
				".cz:5432/mobility_model_prague", "user", "password");
		PTSettings ptSettings = new PTSettings("2015-06-22", "2015-06-23");

		GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(new EPSGProjection(2065), osm, Sets.immutableEnumSet
				(ModeOfTransport.CAR, ModeOfTransport.BIKE, ModeOfTransport.WALK), ptSettings, connection);

		gtdBuilder.build();
		//		gtdBuilder.buildPtGraph(0);
		//		gtdBuilder.buildSimplifiedRoadGraph();

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
