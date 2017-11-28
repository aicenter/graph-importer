/* 
 * Copyright (C) 2017 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
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
