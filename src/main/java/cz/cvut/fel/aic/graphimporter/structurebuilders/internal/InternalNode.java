/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter.structurebuilders.internal;

import cz.cvut.fel.aic.geographtools.Node;
import java.util.Map;


/**
 *
 * @author fido
 */
public class InternalNode extends Node{
	
	private final Map<String,Object> otherParams;
	
	public InternalNode(int id, long sourceId, int latE6, int lonE6, int latProjected, int lonProjected, int elevation,
			Map<String,Object> otherParams) {
		super(id, sourceId, latE6, lonE6, latProjected, lonProjected, elevation);
		this.otherParams = otherParams;
	}
	
	public <T> T get(String key){
		return (T) otherParams.get(key);
	}
}
