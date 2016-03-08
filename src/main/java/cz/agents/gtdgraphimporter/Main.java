package cz.agents.gtdgraphimporter;

import com.google.common.collect.Sets;
import cz.agents.basestructures.Node;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author Marek Cuchý
 */
public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException,
			FactoryException, TransformException {
		DOMConfigurator.configure("log4j.xml");

		URL osmUrl = new File("jmk.osm").toURI().toURL();
		//		URL osmUrl = Main.class.getResource("/Grid/grid.osm");
		//						URL osmUrl = new File("sck.osm").toURI().toURL();

		OsmGraphParser builder = new OsmGraphParser.Builder(osmUrl, new NodeFactory(2065), Sets.immutableEnumSet
				(ModeOfTransport.CAR, ModeOfTransport.BIKE, ModeOfTransport.WALK)).build();
		GraphBuilder<Node, TimeDependentEdge> graphBuilder = builder.readOsmAndCreateGraphBuilder();

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
