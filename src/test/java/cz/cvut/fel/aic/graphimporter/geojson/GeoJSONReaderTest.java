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
package cz.cvut.fel.aic.graphimporter.geojson;

import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;
import org.junit.Test;

public class GeoJSONReaderTest {


    @Test
    public void test1() throws Exception {
        GeoJSONReader roadImporter = new GeoJSONReader(getClass().getResource("test1/graph_edges.geojson").getPath(), getClass().getResource("test1/graph_nodes.geojson").getPath(), new Transformer(3857));
        TmpGraphBuilder<InternalNode, InternalEdge> graph = roadImporter.loadGraph();
    }

    @Test
    public void test2() throws Exception {
        GeoJSONReader roadImporter = new GeoJSONReader(getClass().getResource("test2/graph_edges.geojson").getPath(), getClass().getResource("test2/graph_nodes.geojson").getPath(), new Transformer(3857));
        TmpGraphBuilder<InternalNode, InternalEdge> graph = roadImporter.loadGraph();
    }
}
