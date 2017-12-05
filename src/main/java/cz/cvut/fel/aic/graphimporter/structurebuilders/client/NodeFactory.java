/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter.structurebuilders.client;

import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;
import cz.cvut.fel.aic.geographtools.Node;

/**
 *
 * @author fido
 */
public interface NodeFactory<N extends Node>{
    
    public N createNode(InternalNode internalNode);
}
