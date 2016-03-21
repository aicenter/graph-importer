/* This code is owned by Umotional s.r.o. (IN: 03974618). All Rights Reserved. */
package cz.agents.gtdgraphimporter.kd;

/**
 * Interface for using <code>KDTree</code> with any object.
 */
public interface KDTreeResolver<V> {

	/**
	 * Computed distance between object and given coordinates.
	 */
	public double computeDistance(V v1, double[] coords);

	/**
	 * Returns coordinates for specified object.
	 */
	public double[] getCoordinates(V v1);

}
