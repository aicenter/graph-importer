/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.gtdgraphimporter.structurebuilders.internal;

import cz.agents.basestructures.GPSLocation;
import cz.agents.gtdgraphimporter.structurebuilders.node.NodeBuilder;



/**
 *
 * @author fido
 */
public class InternalNodeBuilder extends NodeBuilder<InternalNode>{

    public InternalNodeBuilder(int tmpId, long sourceId, GPSLocation location) {
        super(tmpId, sourceId, location);
    }

    @Override
    public InternalNode buildNode(int id) {
        return new InternalNode(id, sourceId, location.latE6, location.lonE6, location.latProjected, location.lonProjected, 
                location.elevation);
    }
    
}
