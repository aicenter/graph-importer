package cz.cvut.fel.aic.gtdgraphimporter.osm.handler;

import cz.cvut.fel.aic.gtdgraphimporter.osm.OsmElementConsumer;
import cz.cvut.fel.aic.gtdgraphimporter.osm.element.OsmWay;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * @author Marek Cuchý
 */
public class WayHandler extends OsmElementHandler<OsmWay> {

	private static final Logger LOGGER = Logger.getLogger(WayHandler.class);

	public WayHandler(OsmElementConsumer consumer) {
		super(consumer);
	}

	@Override
	public void startElement(String qName, Attributes attributes) {
		switch (qName) {
			case "way":
				handleNewWay(attributes);
				break;
			case "tag":
				handleTag(attributes);
				break;
			case "nd":
				handleNode(attributes);
				break;
			default:
				LOGGER.debug("Ignoring start of way subelement '" + qName + "'");
		}
	}

	private void handleNode(Attributes attributes) {
		currentElement.addNode(Long.parseLong(attributes.getValue("ref")));
	}

	private void handleNewWay(Attributes attributes) {
		currentElement = new OsmWay(parseId(attributes));
	}

	@Override
	public void endElement(String qName) {
		switch (qName) {
			case "nd":
			case "tag":
				return;
			case "way":
				addCurrentElement();
				return;
			default:
				LOGGER.debug("Ignoring end of node subelement '" + qName + "'");
		}
	}

	@Override
	protected void addCurrentElement() {
		consumer.accept(currentElement);
	}
}
