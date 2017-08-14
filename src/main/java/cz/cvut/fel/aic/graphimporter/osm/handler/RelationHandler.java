package cz.cvut.fel.aic.graphimporter.osm.handler;

import cz.cvut.fel.aic.graphimporter.osm.OsmElementConsumer;
import cz.cvut.fel.aic.graphimporter.osm.element.OsmRelation;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * @author Marek Cuchý
 */
public class RelationHandler extends OsmElementHandler<OsmRelation> {

	private static final Logger LOGGER = Logger.getLogger(RelationHandler.class);

	public RelationHandler(OsmElementConsumer consumer) {
		super(consumer);
	}

	@Override
	public void startElement(String qName, Attributes attributes) {
		switch (qName) {
			case "relation":
				handleNewRelation(attributes);
				break;
			case "tag":
				handleTag(attributes);
				break;
			case "member":
				handleMember(attributes);
				break;
			default:
				LOGGER.debug("Ignoring start of relation subelement '" + qName + "'");
		}
	}

	private void handleMember(Attributes attributes) {
		long memberId = Long.parseLong(attributes.getValue("ref"));

		switch (attributes.getValue("type")) {
			case "node":
				currentElement.addNode(memberId);
				break;
			case "way":
				currentElement.addWay(memberId);
				break;
			case "relation":
				currentElement.addRelation(memberId);
				break;
			default:
				throw new IllegalArgumentException("Illegal 'type' value for relation member: " + attributes.getValue
						("type"));
		}
	}

	private void handleNewRelation(Attributes attributes) {
		currentElement = new OsmRelation(parseId(attributes));
	}

	@Override
	public void endElement(String qName) {
		switch (qName) {
			case "member":
			case "tag":
				return;
			case "relation":
				addCurrentElement();
				return;
			default:
				LOGGER.debug("Ignoring end of relation subelement '" + qName + "'");
		}
	}

	@Override
	protected void addCurrentElement() {
		consumer.accept(currentElement);
	}
}
