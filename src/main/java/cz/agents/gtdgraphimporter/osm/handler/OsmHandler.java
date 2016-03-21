package cz.agents.gtdgraphimporter.osm.handler;

import cz.agents.gtdgraphimporter.osm.OsmElementConsumer;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler for SAX parsing of OSM XMLs. It passes parsed OSM elements to an {@link OsmElementConsumer}.
 *
 * @author Marek Cuchý
 */
public class OsmHandler extends DefaultHandler {

	private static final Logger LOGGER = Logger.getLogger(OsmHandler.class);

	private final NodeHandler nodeHandler;
	private final WayHandler wayHandler;
	private final RelationHandler relationHandler;

	private OsmElementHandler currentElementHandler;

	public OsmHandler(OsmElementConsumer consumer) {
		this(new NodeHandler(consumer), new WayHandler(consumer), new RelationHandler(consumer));
	}

	public OsmHandler(NodeHandler nodeHandler, WayHandler wayHandler, RelationHandler relationHandler) {
		this.nodeHandler = nodeHandler;
		this.wayHandler = wayHandler;
		this.relationHandler = relationHandler;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		switch (qName) {
			case "node":
				currentElementHandler = nodeHandler;
				break;
			case "way":
				currentElementHandler = wayHandler;
				break;
			case "relation":
				currentElementHandler = relationHandler;
				break;
		}
		if (currentElementHandler == null) {
			LOGGER.debug("Ignoring start of '" + qName + "' element.");
		} else {
			currentElementHandler.startElement(qName, attributes);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (qName) {
			case "node":
			case "way":
			case "relation":
				currentElementHandler.endElement(qName);
				currentElementHandler = null;
				return;
		}
		if (currentElementHandler == null) {
			LOGGER.debug("Ignoring end of '" + qName + "' element.");
		} else {
			currentElementHandler.endElement(qName);
		}
	}
}
