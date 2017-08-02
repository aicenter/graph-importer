/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.gtdgraphimporter.structurebuilders.client;

import cz.agents.gtdgraphimporter.structurebuilders.internal.InternalNode;
import cz.cvut.fel.aic.geographtools.Node;

/**
 *
 * @author fido
 */
public abstract class NodeFactory<N extends Node>{
    
    public abstract N createNode(InternalNode internalNode);
}
