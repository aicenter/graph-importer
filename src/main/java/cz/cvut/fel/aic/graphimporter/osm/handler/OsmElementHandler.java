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
import cz.cvut.fel.aic.graphimporter.osm.element.OsmElement;
import org.xml.sax.Attributes;

/**
 * @author Marek Cuchý
 */
public abstract class OsmElementHandler<TElement extends OsmElement> {

	protected final OsmElementConsumer consumer;

	protected TElement currentElement;

	public OsmElementHandler(OsmElementConsumer consumer) {
		this.consumer = consumer;
	}

	public abstract void startElement(String qName, Attributes attributes);

	public abstract void endElement(String qName);

	protected abstract void addCurrentElement();

	protected long parseId(Attributes attributes) {
		return Long.parseLong(attributes.getValue("id"));
	}

	protected void handleTag(Attributes attributes) {
		String key = attributes.getValue("k");
		String value = attributes.getValue("v");
		currentElement.addTag(key, value);
	}
}
