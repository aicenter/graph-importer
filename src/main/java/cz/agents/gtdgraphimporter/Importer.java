/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.gtdgraphimporter;

import cz.agents.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.internal.InternalEdge;
import cz.agents.gtdgraphimporter.structurebuilders.internal.InternalNode;

/**
 *
 * @author fido
 */
public abstract class Importer {
    public abstract String getSerializationName();
    
    public abstract TmpGraphBuilder<InternalNode,InternalEdge> loadGraph();
}
