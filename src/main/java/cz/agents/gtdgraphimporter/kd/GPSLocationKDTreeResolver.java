/* This code is owned by Umotional s.r.o. (IN: 03974618). All Rights Reserved. */
package cz.agents.gtdgraphimporter.kd;

import cz.agents.basestructures.GPSLocation;
import cz.agents.geotools.EdgeUtil;

/**
 * Implementation of <code>KDTreeResolver</code> for GPSLocation and its subclasses.
 */
public class GPSLocationKDTreeResolver<T extends GPSLocation> implements KDTreeResolver<T> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double computeDistance(T v1, double[] coords) {
		return EdgeUtil.computeEuclideanDistance(v1.latProjected, v1.lonProjected, coords[1], coords[0]);
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
	public static double[] getCoords(GPSLocation v1) {
		return new double[] { v1.lonProjected, v1.latProjected };
	}
}
