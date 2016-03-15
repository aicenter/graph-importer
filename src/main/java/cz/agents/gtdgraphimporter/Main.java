package cz.agents.gtdgraphimporter;

import com.google.common.collect.Sets;
import cz.agents.geotools.EPSGProjection;
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

/**
 * @author Marek Cuchý
 */
public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(
			String[] args) throws ParserConfigurationException, SAXException, IOException, FactoryException,
			TransformException {
		DOMConfigurator.configure("log4j.xml");

		URL osmUrl = new File("jmk.osm").toURI().toURL();
//		Main.class.getResource("/Grid/grid.osm").
		//				URL osmUrl = Main.class.getResource("/Grid/grid.osm");
		//						URL osmUrl = new File("sck.osm").toURI().toURL();

//		OsmGraphBuilder builder = new OsmGraphBuilder.Builder(osmUrl, new EPSGProjection(2065), Sets.immutableEnumSet
//				(ModeOfTransport.CAR, ModeOfTransport.BIKE, ModeOfTransport.WALK)).build();
//		//		Graph<RoadNode, RoadEdge> graphBuilder = builder.readOsmAndCreateGraph();
//		TmpGraphBuilder<RoadNode, RoadEdge> osmBuilder = builder.readOsmAndGetGraphBuilder();

		GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(new EPSGProjection(2065), "jmk.osm", Sets
				.immutableEnumSet(ModeOfTransport.CAR, ModeOfTransport.BIKE, ModeOfTransport.WALK), null, null);

		gtdBuilder.buildOsmGraph();
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
