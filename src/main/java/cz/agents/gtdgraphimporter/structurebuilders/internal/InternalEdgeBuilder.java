/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.gtdgraphimporter.structurebuilders.internal;

import cz.agents.gtdgraphimporter.structurebuilders.edge.EdgeBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author fido
 */
public class InternalEdgeBuilder extends EdgeBuilder<InternalEdge>{
    
    private final Map<String,Object> otherParams;
    
    private Set<ModeOfTransport> modeOfTransports = EnumSet.noneOf(ModeOfTransport.class);
    
    public long wayID; // OsmWay ID
    
    public int uniqueWayID;
    
    public int oppositeWayUniqueId; // -1 if does not exists,otherwise uniqueWayId of the direction edge
    
    public float allowedMaxSpeedInMpS;
    
    public int lanesCount;
    

    public InternalEdgeBuilder(int tmpFromId, int tmpToId, long osmWayId, int uniqueWayId, int oppositeWayUniqueId, int length, 
            Set<ModeOfTransport> modeOfTransports, float allowedMaxSpeedInMpS, Integer lanesCount) {
        super(tmpFromId, tmpToId, length);
        
        this.wayID = osmWayId;
        this.modeOfTransports = EnumSet.copyOf(modeOfTransports);
        this.uniqueWayID = uniqueWayId;
        this.oppositeWayUniqueId = oppositeWayUniqueId;

        //extras
        this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
        this.lanesCount = lanesCount;
        
        otherParams = new HashMap<>();
    }
    
    public InternalEdgeBuilder addModeOfTransports(Set<ModeOfTransport> ModeOfTransports) {
        this.modeOfTransports.addAll(ModeOfTransports);
        return this;
    }

    @Override
    public InternalEdge build(int fromId, int toId) {
        otherParams.put("wayID", wayID);
        otherParams.put("uniqueWayID", uniqueWayID);
        otherParams.put("oppositeWayUniqueId", oppositeWayUniqueId);
        otherParams.put("modeOfTransports", modeOfTransports);
        otherParams.put("allowedMaxSpeedInMpS", allowedMaxSpeedInMpS);
        otherParams.put("lanesCount", lanesCount);
        return new InternalEdge(fromId, toId, getLength(), otherParams);
    }

    @Override
    public boolean checkFeasibility(ModeOfTransport mode) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InternalEdgeBuilder copy(int tmpFromId, int tmpToId, int length) {
        return new InternalEdgeBuilder(tmpFromId, tmpToId, wayID, uniqueWayID, oppositeWayUniqueId,
                length, modeOfTransports, allowedMaxSpeedInMpS, lanesCount);
    }
    
    public boolean equalAttributes(InternalEdgeBuilder that) {
        return wayID == that.wayID
                && lanesCount == that.lanesCount
                && (modeOfTransports != null ? modeOfTransports.equals(that.modeOfTransports)
                && Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) == 0 :
                that.modeOfTransports == null);
    }
    
}