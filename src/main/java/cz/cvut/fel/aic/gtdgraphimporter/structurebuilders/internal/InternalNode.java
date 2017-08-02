/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.internal;

import cz.cvut.fel.aic.geographtools.Node;


/**
 *
 * @author fido
 */
public class InternalNode extends Node{
    
    public InternalNode(int id, long sourceId, int latE6, int lonE6, int latProjected, int lonProjected, int elevation) {
        super(id, sourceId, latE6, lonE6, latProjected, lonProjected, elevation);
    }
    
}
