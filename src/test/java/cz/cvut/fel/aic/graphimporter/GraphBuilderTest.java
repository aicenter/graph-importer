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
package cz.cvut.fel.aic.graphimporter;

import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.GraphBuilder;
import cz.cvut.fel.aic.geographtools.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GraphBuilderTest {

	List<Node> nodes;
	List<Edge> edges;

	@Before
	public void setUp() throws Exception {
		Node n0 = new Node(0, 10, 0, 0, 0, 0, 0);
		Node n1 = new Node(1, 11, 0, 0, 0, 0, 0);
		Node n2 = new Node(2, 12, 0, 0, 0, 0, 0);
		Node n3 = new Node(3, 13, 0, 0, 0, 0, 0);
		Node n4 = new Node(4, 14, 0, 0, 0, 0, 0);
		Node n5 = new Node(5, 15, 0, 0, 0, 0, 0);
		Node n6 = new Node(6, 16, 0, 0, 0, 0, 0);
		Node n7 = new Node(7, 17, 0, 0, 0, 0, 0);

		Edge e1 = new Edge(n1, n2, 100);
		Edge e2 = new Edge(n2, n3, 100);
		Edge e3 = new Edge(n3, n1, 100);
		Edge e4 = new Edge(n3, n4, 100);
		Edge e5 = new Edge(n3, n5, 100);
		Edge e6 = new Edge(n4, n5, 100);
		Edge e7 = new Edge(n5, n4, 100);
		Edge e8 = new Edge(n5, n6, 100);

		nodes = new ArrayList<>();
		edges = new ArrayList<>();

		nodes.add(n0);
		nodes.add(n1);
		nodes.add(n2);
		nodes.add(n3);
		nodes.add(n4);
		nodes.add(n5);
		nodes.add(n6);
		nodes.add(n7);

		edges.add(e1);
		edges.add(e2);
		edges.add(e3);
		edges.add(e4);
		edges.add(e5);
		edges.add(e6);
		edges.add(e7);
		edges.add(e8);

	}

	@Test
	public void testGetNode() throws Exception {

		GraphBuilder<Node, Edge> builder = new GraphBuilder<>();
		builder.addNodes(nodes);
		Assert.assertEquals(11, builder.getNode(1).sourceId);
		Assert.assertEquals(13, builder.getNode(3).sourceId);

	}

	@Test
	public void testGetAllNodes() throws Exception {
		GraphBuilder<Node, Edge> builder = new GraphBuilder<>();
		builder.addNodes(nodes);
		Assert.assertEquals(nodes.size(), builder.getAllNodes().size());
	}

	@Test
	public void testGetInEdges() throws Exception {
		GraphBuilder<Node, Edge> builder = new GraphBuilder<>();
		builder.addNodes(nodes);
		builder.addEdges(edges);

		Assert.assertEquals(2, builder.getInEdges(5).size());
	}

	@Test
	public void testGetOutEdges() throws Exception {
		GraphBuilder<Node, Edge> builder = new GraphBuilder<>();
		builder.addNodes(nodes);
		builder.addEdges(edges);

		Assert.assertEquals(3, builder.getOutEdges(3).size());
	}

	@Test
	public void testGetAllEdges() throws Exception {
		GraphBuilder<Node, Edge> builder = new GraphBuilder<>();
		builder.addNodes(nodes);
		builder.addEdges(edges);

		Assert.assertEquals(edges.size(), builder.getAllEdges().size());
	}

	@Test
	public void testCreateGraph() throws Exception {
		GraphBuilder<Node, Edge> builder = new GraphBuilder<>();
		builder.addNodes(nodes);
		builder.addEdges(edges);

		Graph<Node, Edge> graph = builder.createGraph();
		Assert.assertNotNull(graph);
		Assert.assertEquals(0, builder.getAllNodes().size());
		Assert.assertEquals(0, builder.getAllEdges().size());

		Assert.assertEquals(edges.size(), graph.getAllEdges().size());
		Assert.assertEquals(edges.size(), graph.getAllEdges().size());
	}

	@Test
	public void testDumpCurrentGraph() throws Exception {
		GraphBuilder<Node, Edge> builder = new GraphBuilder<>();
		builder.addNodes(nodes);
		builder.addEdges(edges);

		Graph<Node, Edge> graph = builder.dumpCurrentGraph();
		Assert.assertNotNull(graph);
		Assert.assertEquals(nodes.size(), builder.getAllNodes().size());
		Assert.assertEquals(edges.size(), builder.getAllEdges().size());

		Assert.assertEquals(edges.size(), graph.getAllEdges().size());
		Assert.assertEquals(edges.size(), graph.getAllEdges().size());
	}
}

