package cz.cvut.fel.aic.gtdgraphimporter.osm.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Marek Cuchý
 */
public class OsmWay extends OsmElement {

	private final List<Long> nodes = new ArrayList<>();

	public OsmWay(long id) {
		super(id);
	}

	public void addNode(long nodeId) {
		nodes.add(nodeId);
	}

	public List<Long> getNodes() {
		return nodes;
	}

	/**
	 * Remove all nodes not contained in {@code nodeIds} from the way nodes.
	 *
	 * @param nodeIds
	 */
	public void removeMissingNodes(Set<Long> nodeIds) {
		nodes.removeIf(l -> !nodeIds.contains(l));
	}

	@Override
	public String toString() {
		return "OsmWay{" +
				"nodes=" + nodes +
				'}';
	}
}
