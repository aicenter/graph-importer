package cz.cvut.fel.aic.graphimporter.osm.handler;

import cz.cvut.fel.aic.graphimporter.osm.OsmElementConsumer;
import cz.cvut.fel.aic.graphimporter.osm.element.OsmNode;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * @author Marek Cuchý
 */
public class NodeHandler extends OsmElementHandler<OsmNode> {

	private static final Logger LOGGER = Logger.getLogger(NodeHandler.class);

	public NodeHandler(OsmElementConsumer consumer) {
		super(consumer);
	}

	@Override
	public void startElement(String qName, Attributes attributes) {
		switch (qName) {
			case "node":
				handleNewNode(attributes);
				break;
			case "tag":
				handleTag(attributes);
				break;
			default:
				LOGGER.debug("Ignoring start of node subelement " + qName);
		}
	}

	private void handleNewNode(Attributes attributes) {
		long id = parseId(attributes);
		double lat = Double.parseDouble(attributes.getValue("lat"));
		double lon = Double.parseDouble(attributes.getValue("lon"));
		currentElement = new OsmNode(id, lat, lon);
	}



	@Override
	public void endElement(String qName) {
		switch (qName) {
			case "tag":
				return;
			case "node":
				addCurrentElement();
				return;
			default:
				LOGGER.debug("Ignoring end of node subelement " + qName);
		}
	}

	@Override
	protected void addCurrentElement() {
		consumer.accept(currentElement);
	}

}
