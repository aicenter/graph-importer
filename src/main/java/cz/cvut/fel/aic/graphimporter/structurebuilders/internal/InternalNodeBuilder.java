/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter.structurebuilders.internal;

import cz.cvut.fel.aic.graphimporter.structurebuilders.NodeBuilder;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.Map;



/**
 *
 * @author fido
 */
public class InternalNodeBuilder extends NodeBuilder<InternalNode>{
	
	private final Map<String, Object> otherParams;

	public InternalNodeBuilder(int tmpId, long sourceId, GPSLocation location, Map<String, Object> otherParams) {
		super(tmpId, sourceId, location);
		this.otherParams = otherParams;
	}

	@Override
	public InternalNode buildNode(int id) {
		return new InternalNode(id, sourceId, location.latE6, location.lonE6, location.getLatitudeProjected1E2(), 
				location.getLongitudeProjected1E2(), location.elevation, otherParams);
	}
	
}
