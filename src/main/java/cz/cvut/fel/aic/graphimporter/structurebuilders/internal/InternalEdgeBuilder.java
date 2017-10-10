/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter.structurebuilders.internal;

import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.TransportMode;
import cz.cvut.fel.aic.graphimporter.structurebuilders.EdgeBuilder;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author fido
 */
public class InternalEdgeBuilder extends EdgeBuilder<InternalEdge> {

    private final Map<String, Object> otherParams;

    private Set<TransportMode> modeOfTransports = EnumSet.noneOf(TransportMode.class);

    private int uniqueWayID;

    private int oppositeWayUniqueId; // -1 if does not exists,otherwise uniqueWayId of the direction edge

    private float allowedMaxSpeedInMpS;

    private int lanesCount;

    private List<GPSLocation> coordinateList;

    private List<Map<String, Integer>> lanesTurn;


    public InternalEdgeBuilder(int tmpFromId, int tmpToId, int uniqueWayId, int oppositeWayUniqueId, int length,
                               Set<TransportMode> modeOfTransports, float allowedMaxSpeedInMpS, Integer lanesCount,
                               List<GPSLocation> coordinateList, List<Map<String, Integer>> lanesTurn) {
        super(tmpFromId, tmpToId, length);

        this.modeOfTransports = EnumSet.copyOf(modeOfTransports);
        this.uniqueWayID = uniqueWayId;
        this.oppositeWayUniqueId = oppositeWayUniqueId;

        //extras
        this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
        this.lanesCount = lanesCount;
        this.coordinateList = coordinateList;
        this.lanesTurn = lanesTurn;

        otherParams = new HashMap<>();
    }

    public InternalEdgeBuilder addModeOfTransports(Set<TransportMode> ModeOfTransports) {
        this.modeOfTransports.addAll(ModeOfTransports);
        return this;
    }

    @Override
    public InternalEdge build(int fromId, int toId) {
        otherParams.put("uniqueWayID", uniqueWayID);
        otherParams.put("oppositeWayUniqueId", oppositeWayUniqueId);
        otherParams.put("modeOfTransports", modeOfTransports);
        otherParams.put("allowedMaxSpeedInMpS", allowedMaxSpeedInMpS);
        otherParams.put("lanesCount", lanesCount);
        otherParams.put("coordinateList", coordinateList);
        otherParams.put("lanesTurn", lanesTurn);
        return new InternalEdge(fromId, toId, getLength(), otherParams);
    }

    @Override
    public boolean checkFeasibility(TransportMode mode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InternalEdgeBuilder copy(int tmpFromId, int tmpToId, int length) {
        return new InternalEdgeBuilder(tmpFromId, tmpToId, uniqueWayID, oppositeWayUniqueId,
                length, modeOfTransports, allowedMaxSpeedInMpS, lanesCount, coordinateList,lanesTurn);
    }

    public boolean equalAttributes(InternalEdgeBuilder that) {
        return  lanesCount == that.lanesCount
                && (modeOfTransports != null ? modeOfTransports.equals(that.modeOfTransports)
                && Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) == 0 :
                that.modeOfTransports == null);
    }

}
