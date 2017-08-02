/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.gtdgraphimporter.structurebuilders.client;

import cz.agents.gtdgraphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.geographtools.Edge;

/**
 *
 * @author fido
 */
public abstract class EdgeFactory<E extends Edge> {
    
    public abstract E createEdge(InternalEdge internalEdge);
    
}
