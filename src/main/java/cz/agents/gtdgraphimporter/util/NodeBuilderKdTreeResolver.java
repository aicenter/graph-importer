package cz.agents.gtdgraphimporter.util;

import cz.agents.geotools.DistanceUtil;
import cz.agents.geotools.KDTreeResolver;
import cz.agents.gtdgraphimporter.structurebuilders.node.NodeBuilder;

/**
 * @author Marek Cuchý
 */
public class NodeBuilderKdTreeResolver<T extends NodeBuilder<?>> implements KDTreeResolver<T> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double computeDistance(T v1, double[] coords) {
		return DistanceUtil.computeEuclideanDistance(v1.location.latProjected, v1.location.lonProjected, coords[1],
				coords[0]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double[] getCoordinates(T v1) {
		return getCoords(v1);
	}

	/**
	 * Static version of getCoordinates to be used without class initialization.
	 */
	public static double[] getCoords(NodeBuilder<?> v1) {
		return new double[]{v1.location.lonProjected, v1.location.latProjected};
	}
}