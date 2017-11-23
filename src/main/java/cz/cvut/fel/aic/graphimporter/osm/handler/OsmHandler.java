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

	private OsmElementHandler<?> currentElementHandler;

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
