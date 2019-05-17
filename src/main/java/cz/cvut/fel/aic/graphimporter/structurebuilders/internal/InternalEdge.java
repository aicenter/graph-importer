/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter.structurebuilders.internal;

import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.Map;

/**
 *
 * @author fido
 */
public class InternalEdge extends Edge{
	
	private final Map<String,Object> otherParams;
	
	public InternalEdge(Node fromNode, Node toNode, int length, Map<String,Object> otherParams) {
		super(fromNode, toNode, length);
		this.otherParams = otherParams;
	}
	
	public <T> T get(String key){
		return (T) otherParams.get(key);
	}
}
