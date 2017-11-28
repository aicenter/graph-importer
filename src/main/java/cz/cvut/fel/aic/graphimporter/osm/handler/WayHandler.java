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
import cz.cvut.fel.aic.graphimporter.osm.element.OsmWay;
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
