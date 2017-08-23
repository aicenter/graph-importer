package cz.cvut.fel.aic.graphimporter.geojson;

import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;
import org.junit.Test;

public class GeoJSONReaderTest {


    @Test
    public void testGetNode() throws Exception {
        GeoJSONReader roadImporter = new GeoJSONReader(getClass().getResource("test1/graph_edges.geojson").getPath(), getClass().getResource("test1/graph_nodes.geojson").getPath(), new Transformer(3857));
        TmpGraphBuilder<InternalNode, InternalEdge> graph = roadImporter.loadGraph();
    }
}
