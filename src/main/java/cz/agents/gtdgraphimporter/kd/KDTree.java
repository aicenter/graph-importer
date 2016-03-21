/* This code is owned by Umotional s.r.o. (IN: 03974618). All Rights Reserved. */
package cz.agents.gtdgraphimporter.kd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Wrapper class, which allows to use <code>net.sf.javaml.core.kdtree</code> with any object.
 */
public class KDTree<V> {

	private final net.sf.javaml.core.kdtree.KDTree kdTree;
	private final KDTreeResolver<V> treeResolver;

	private HashSet<List<Double>> usedCoords;

	/**
	 * if two elements with the same key are entered, that to do
	 * 0 - throw an exception
	 * 1 - use new
	 * -1 - use old;
	 */
	private final int conflictResolverMode;

	/**
	 * Constructs KDTree.
	 *
	 * @param kdTreeDim            Dimension of the tree
	 * @param treeResolver         Implementation of the interface for objects to be stored in the tree, which computes necesarry data
	 * @param conflictResolverMode if element with key, which already exists in tree, is entered, what to do : 0 - throw an exception, 1 - use new, -1 - use old;
	 */
	public KDTree(int kdTreeDim, KDTreeResolver<V> treeResolver, int conflictResolverMode) {
		this.kdTree = new net.sf.javaml.core.kdtree.KDTree(kdTreeDim);
		this.treeResolver = treeResolver;
		this.usedCoords = new HashSet<>();
		this.conflictResolverMode = conflictResolverMode;

	}

	/**
	 * Constructs KDTree.
	 *
	 * @param kdTreeDim    Dimension of the tree
	 * @param treeResolver Implementation of the interface for objects to be stored in the tree, which computes necesarry data
	 */
	public KDTree(int kdTreeDim, KDTreeResolver<V> treeResolver) {
		this(kdTreeDim, treeResolver, 0);
	}

	public KDTreeResolver<V> getTreeResolver() {
		return treeResolver;
	}

	/**
	 * Returns all objects in the tree that are closer then specified distance from the specified coordinates
	 */
	public ArrayList<V> getNearestNodesCloserThan(double[] coordinates, double desiredDistance) {

		int numOfNeighbors = Math.min(10, size());
		Object[] nearestNodes = kdTree.nearest(coordinates, numOfNeighbors);

		while (numOfNeighbors < size()
				&& treeResolver.computeDistance((V) nearestNodes[nearestNodes.length - 1], coordinates)
				< desiredDistance) {
			//while the most distant element is still too close, or i got all the neighbors
			//increasing the number of neighbors, to reach desired distance (the coeficient can be estimated better using distance of the furthest and the amount of ndoes
			numOfNeighbors = Math.min(numOfNeighbors * 4, size());
			nearestNodes = kdTree.nearest(coordinates, Math.min(numOfNeighbors, size()));
		}

		int upperBoundForNodes = nearestNodes.length;

		//larger because the roadNodes really close to each other, and we want to merge them, so we add at least NUM_OF_POINTS_IN_CLUSTER to cluster, but we also add the ones realy close
		while (upperBoundForNodes > 0
				&& treeResolver.computeDistance((V) nearestNodes[upperBoundForNodes - 1], coordinates)
				> desiredDistance)
			upperBoundForNodes--;

		ArrayList<V> prunnedConvertedList = new ArrayList<>();
		for (int i = 0; i < upperBoundForNodes; i++) {
			prunnedConvertedList.add((V) nearestNodes[i]);
		}

		return prunnedConvertedList;

	}

	/**
	 * Returns maximum N closest objects in the tree from the specified coordinates. Returns only objects closer than specified distance
	 */
	public ArrayList<V> getNNearestNodesWithMaxDistance(double[] coords, int n, double maxDistance) {

		int numOfNeighbors = Math.min(n, size());
		Object[] nearestNodes = kdTree.nearest(coords, numOfNeighbors);

		int upperBoundForNodes = nearestNodes.length;

		//prunes those too far away
		while (upperBoundForNodes > 0
				&& treeResolver.computeDistance((V) nearestNodes[upperBoundForNodes - 1], coords) > maxDistance)
			upperBoundForNodes--;

		ArrayList<V> prunnedConvertedList = new ArrayList<>();
		for (int i = 0; i < upperBoundForNodes; i++) {
			prunnedConvertedList.add((V) nearestNodes[i]);
		}

		return prunnedConvertedList;

	}

	/**
	 * Returns minimum N objects from the tree, closest from the specified coords.
	 * Nodes closer than <code>distanceToForceMerge</code> are returned, even if there is more than N nodes.
	 * Nodes further than <code>maxDistance</code> are never returned, even if there is less than N nodes.
	 */
	public ArrayList<V> getNNearestNodesWithForcedDistanceToMergeAndMaxDistance(double[] coords, int n,
			double distanceToForceMerge, double maxDistance) {

		if (distanceToForceMerge > maxDistance) {
			throw new IllegalArgumentException(
					"distance to force merge must be smaller or equal to maximalDisance allowed to merge");
		}
		ArrayList<V> nodes = getNearestNodesCloserThan(coords, maxDistance); //maxDistance fulfilled

		//check Nth node
		if (nodes.size() < n) {
			return nodes;
		} else {
			double distanceToNthNode = treeResolver.computeDistance(nodes.get(n - 1), coords);

			if (distanceToNthNode > distanceToForceMerge) { //all force merged included, return sublist of length N
				return new ArrayList<>(nodes.subList(0, n));
			} else { //not all to force merge included, search for the boundary

				for (int i = n; i < nodes.size(); i++) {
					if (treeResolver.computeDistance(nodes.get(n), coords) > distanceToForceMerge) {
						//return without current
						return new ArrayList<>(nodes.subList(0, i));
					}
				}

				return nodes; //exactly all nodes need to be returned

			}
		}

	}

	/**
	 * Returns nearest N nodes from specified coordinates
	 */
	public ArrayList<V> getNNearestNodes(double[] coords, int n) {
		return getNNearestNodesWithMaxDistance(coords, n, Double.POSITIVE_INFINITY);
	}

	/**
	 * Return neerest node in the tree from specified coordinates
	 */
	public V getNearestNode(double[] coords) {
		return getNNearestNodes(coords, 1).get(0);
	}

	/**
	 * Inserts object to tree.
	 */
	public void insert(V object) throws IllegalArgumentException {

		double[] coords = treeResolver.getCoordinates(object);
		List<Double> coordsList = new ArrayList<>();

		for (int i = 0; i < coords.length; i++) {
			coordsList.add(coords[i]);
		}

		if (!usedCoords.contains(coordsList)) {

			usedCoords.add(coordsList);
			kdTree.insert(coords, object);

		} else {

            /*
			 * if two elements with the same key are entered, that to do
             * 0 - throw an exception
             * 1 - use new
             * -1 - use old;
             */

			if (conflictResolverMode == 1) {
				kdTree.delete(coords);
				kdTree.insert(coords, object);
			} else if (conflictResolverMode == -1) {
				//doing nothing,i.e., throwing away the new item
			} else {
				throw new IllegalArgumentException(
						"KDTree already contains element with this key : " + coordsList.toString());
			}
		}
	}

	/**
	 * Deletes object from tree.
	 */
	public void delete(V object) {
		delete(treeResolver.getCoordinates(object));
	}

	/**
	 * Deletes object specified by coordinates from tree.
	 */
	public void delete(double[] coords) {

		List<Double> coordsList = new ArrayList<>();

		for (int i = 0; i < coords.length; i++) {
			coordsList.add(coords[i]);
		}

		if (usedCoords.contains(coordsList)) {
			usedCoords.remove(coordsList);
			kdTree.delete(coords);

		} else {
			throw new IllegalArgumentException("Element not in KDTree!");
		}

	}

	/**
	 * Returns num of elements in the tree.
	 */
	public int size() {
		return usedCoords.size();
	}

}
