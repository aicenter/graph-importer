package cz.cvut.fel.aic.gtdgraphimporter.osm.handler;

import cz.cvut.fel.aic.gtdgraphimporter.osm.OsmElementConsumer;
import cz.cvut.fel.aic.gtdgraphimporter.osm.element.OsmElement;
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
