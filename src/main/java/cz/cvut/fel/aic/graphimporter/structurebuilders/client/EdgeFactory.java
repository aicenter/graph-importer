/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter.structurebuilders.client;

import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.GraphBuilder;
import cz.cvut.fel.aic.geographtools.Node;

/**
 *
 * @author fido
 */
public interface EdgeFactory<N extends Node, E extends Edge> {
    
    public abstract E createEdge(InternalEdge internalEdge, GraphBuilder<N, E> graphBuilder);
    
}
