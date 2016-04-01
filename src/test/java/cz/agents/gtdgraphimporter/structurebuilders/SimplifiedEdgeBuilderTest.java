package cz.agents.gtdgraphimporter.structurebuilders;

import cz.agents.basestructures.EdgeId;
import cz.agents.basestructures.GPSLocation;
import cz.agents.gtdgraphimporter.structurebuilders.edge.RoadEdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.edge.SimplifiedEdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.node.RoadNodeBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.Assert.*;

/**
 * @author Marek Cuchý
 */
public class SimplifiedEdgeBuilderTest {

	private static final Set<ModeOfTransport> PM1 = EnumSet.of(ModeOfTransport.BUS, ModeOfTransport.WALK);
	private static final Set<ModeOfTransport> PM2 = EnumSet.of(ModeOfTransport.CAR);

	private SimplifiedEdgeBuilder b1;
	public static final Comparator<RoadEdgeBuilder> EDGE_COMPARATOR = (o1, o2) -> {
		int result = Integer.compare(o1.getTmpFromId(), o2.getTmpFromId());
		return result != 0 ? result : Integer.compare(o1.getTmpToId(), o2.getTmpToId());
	};

	@Before
	public void init() {
		b1 = SimplifiedEdgeBuilder.createTwoWay(0, 1, 2);
	}

	@Test
	public void testAdd() throws Exception {
		b1.add(0, -1);
		assertEquals(-1, b1.getTmpFromId());
		assertEquals(2, b1.getTmpToId());
		assertEquals(Arrays.asList(0, 1), b1.getRemovedNodes());

		b1.add(2, 3);
		assertEquals(-1, b1.getTmpFromId());
		assertEquals(3, b1.getTmpToId());
		assertEquals(Arrays.asList(0, 1, 2), b1.getRemovedNodes());
	}

	@Test
	public void testAppend() throws Exception {
		SimplifiedEdgeBuilder b2 = SimplifiedEdgeBuilder.createTwoWay(2, 3, 4);
		b2.add(4, 5);
		b1.append(b2);
		assertEquals(0, b1.getTmpFromId());
		assertEquals(5, b1.getTmpToId());
		assertEquals(Arrays.asList(1, 2, 3, 4), b1.getRemovedNodes());
	}

	@Test
	public void testAppendReversed() throws Exception {
		SimplifiedEdgeBuilder b2 = SimplifiedEdgeBuilder.createTwoWay(4, 3, 2);
		b2.add(4, 5);
		b1.appendReversed(b2);
		assertEquals(0, b1.getTmpFromId());
		assertEquals(5, b1.getTmpToId());
		assertEquals(Arrays.asList(1, 2, 3, 4), b1.getRemovedNodes());
	}

	@Test
	public void testPrependReversed() throws Exception {
		SimplifiedEdgeBuilder b2 = SimplifiedEdgeBuilder.createTwoWay(-1, -2, -3);
		b2.add(-1, 0);
		b1.prependReversed(b2);
		assertEquals(-3, b1.getTmpFromId());
		assertEquals(2, b1.getTmpToId());
		assertEquals(Arrays.asList(-2, -1, 0, 1), b1.getRemovedNodes());
	}

	@Test
	public void testBuildEven() throws Exception {
		SimplifiedEdgeBuilder b2 = SimplifiedEdgeBuilder.createTwoWay(-1, -2, -3);
		b2.add(-1, 0);
		b1.prependReversed(b2);

		TmpGraphBuilder<RoadNode, RoadEdge> graph = createTestGraph(-3, 2);
		Map<EdgeId, RoadEdgeBuilder> removedEdges = new HashMap<>();
		graph.getAllEdges().forEach(e -> removedEdges.put(e.getEdgeId(), (RoadEdgeBuilder) e));
		List<RoadEdgeBuilder> onePartResult = b1.build(1, graph, removedEdges);

		//sort by from ID
		Collections.sort(onePartResult, EDGE_COMPARATOR);

		List<RoadEdgeBuilder> expected = new ArrayList<>();
		expected.add(new RoadEdgeBuilder(-3, 2, 5, 1.0f, 15L, PM1));
		expected.add(new RoadEdgeBuilder(2, -3, 5, 2.0f, 15L, PM2));
		assertEquals(expected, onePartResult);

		expected.clear();
		expected.add(new RoadEdgeBuilder(-3, 0, 3, 1.0f, 15L, PM1));
		expected.add(new RoadEdgeBuilder(0, -3, 3, 2.0f, 15L, PM2));
		expected.add(new RoadEdgeBuilder(0, 2, 2, 1.0f, 15L, PM1));
		expected.add(new RoadEdgeBuilder(2, 0, 2, 2.0f, 15L, PM2));

		List<RoadEdgeBuilder> twoPartResult = b1.build(2, graph, removedEdges);
		Collections.sort(twoPartResult, EDGE_COMPARATOR);
		assertEquals(expected, twoPartResult);
	}

	@Test
	public void testBuildOdd() throws Exception {
		b1.add(0, -1);
		b1.add(-1, -2);
		b1.add(-2, -3);
		b1.add(-3, -4);

		TmpGraphBuilder<RoadNode, RoadEdge> graph = createTestGraph(-4, 2);
		Map<EdgeId, RoadEdgeBuilder> removedEdges = new HashMap<>();
		graph.getAllEdges().forEach(e -> removedEdges.put(e.getEdgeId(), (RoadEdgeBuilder) e));
		List<RoadEdgeBuilder> onePartResult = b1.build(1, graph, removedEdges);

		//sort by from ID
		Collections.sort(onePartResult, EDGE_COMPARATOR);

		List<RoadEdgeBuilder> expected = new ArrayList<>();
		expected.add(new RoadEdgeBuilder(-4, 2, 6, 1.0f, 15L, PM1));
		expected.add(new RoadEdgeBuilder(2, -4, 6, 2.0f, 15L, PM2));
		assertEquals(expected, onePartResult);

		expected.clear();
		expected.add(new RoadEdgeBuilder(-4, -1, 3, 1.0f, 15L, PM1));
		expected.add(new RoadEdgeBuilder(-1, -4, 3, 2.0f, 15L, PM2));
		expected.add(new RoadEdgeBuilder(-1, 2, 3, 1.0f, 15L, PM1));
		expected.add(new RoadEdgeBuilder(2, -1, 3, 2.0f, 15L, PM2));

		List<RoadEdgeBuilder> twoPartResult = b1.build(2, graph, removedEdges);
		Collections.sort(twoPartResult, EDGE_COMPARATOR);
		assertEquals(expected, twoPartResult);
	}

	private TmpGraphBuilder<RoadNode, RoadEdge> createTestGraph(int lb, int ub) {
		TmpGraphBuilder<RoadNode, RoadEdge> graph = new TmpGraphBuilder<>();

		for (int i = lb; i <= ub; i++) {
			graph.addNode(new RoadNodeBuilder(i, i + 100, new GPSLocation(0, 0, 0, i)));
		}
		for (int i = lb; i < ub; i++) {
			graph.addEdge(new RoadEdgeBuilder(i, i + 1, 0, 1.0f, 15L, PM1));
		}
		for (int i = ub; i > lb; i--) {
			graph.addEdge(new RoadEdgeBuilder(i, i - 1, 1, 2.0f, 15L, PM2));
		}
		return graph;
	}
}