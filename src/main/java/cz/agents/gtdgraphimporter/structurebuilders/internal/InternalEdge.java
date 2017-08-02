/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.gtdgraphimporter.structurebuilders.internal;

import cz.agents.basestructures.Edge;
import java.util.Map;

/**
 *
 * @author fido
 */
public class InternalEdge extends Edge{
    
    private final Map<String,Object> otherParams;
    
    public InternalEdge(int fromId, int toId, int length, Map<String,Object> otherParams) {
        super(fromId, toId, length);
        this.otherParams = otherParams;
    }
    
    public <T> T get(String key){
        return (T) otherParams.get(key);
    }
}
