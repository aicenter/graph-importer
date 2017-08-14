/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter;

import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;

/**
 *
 * @author fido
 */
public abstract class Importer {
    public abstract String getSerializationName();
    
    public abstract TmpGraphBuilder<InternalNode,InternalEdge> loadGraph();
}
