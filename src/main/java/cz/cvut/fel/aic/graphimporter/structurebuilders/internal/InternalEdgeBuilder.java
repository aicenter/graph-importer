/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.graphimporter.structurebuilders.internal;

import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.GraphBuilder;
import cz.cvut.fel.aic.geographtools.TransportMode;
import cz.cvut.fel.aic.graphimporter.structurebuilders.EdgeBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author fido
 */
public class InternalEdgeBuilder extends EdgeBuilder<InternalEdge, InternalNode> {

	private final Map<String, Object> otherParams;

	private Set<TransportMode> modeOfTransports = EnumSet.noneOf(TransportMode.class);

	public int uniqueWayID;

	public int oppositeWayUniqueId; // -1 if does not exists,otherwise uniqueWayId of the direction edge

	public int allowedMaxSpeedInKmh;

	public int lanesCount;

	public List<GPSLocation> coordinateList;
	
	public <T> T getParam(String key){
		return (T) otherParams.get(key);
	}
	
	public boolean containsParam(String key){
		return otherParams.containsKey(key);
	}

	public InternalEdgeBuilder(int tmpFromId, int tmpToId, int uniqueWayId, int oppositeWayUniqueId,
			int lengthCm, Set<TransportMode> modeOfTransports, int allowedMaxSpeedInKmh, Integer lanesCount,
			List<GPSLocation> coordinateList, Map<String,Object> otherParams) {
		super(tmpFromId, tmpToId, lengthCm);

		this.modeOfTransports = EnumSet.copyOf(modeOfTransports);
		this.uniqueWayID = uniqueWayId;
		this.oppositeWayUniqueId = oppositeWayUniqueId;

		// extras
		this.allowedMaxSpeedInKmh = allowedMaxSpeedInKmh;
		this.lanesCount = lanesCount;
		this.coordinateList = coordinateList;

		this.otherParams = otherParams;
	}

	public InternalEdgeBuilder addModeOfTransports(Set<TransportMode> ModeOfTransports) {
		this.modeOfTransports.addAll(ModeOfTransports);
		return this;
	}

	@Override
	public InternalEdge build(int fromId, int toId, GraphBuilder<InternalNode, InternalEdge> builder) {
		otherParams.put("uniqueWayID", uniqueWayID);
		otherParams.put("oppositeWayUniqueId", oppositeWayUniqueId);
		otherParams.put("modeOfTransports", modeOfTransports);
		otherParams.put("allowedMaxSpeedInMpS", allowedMaxSpeedInKmh);
		otherParams.put("lanesCount", lanesCount);
		otherParams.put("coordinateList", coordinateList);
		return new InternalEdge(builder.getNode(fromId), builder.getNode(toId), getLengthCm(), otherParams);
	}

	@Override
	public boolean checkFeasibility(TransportMode mode) {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	public InternalEdgeBuilder copy(int tmpFromId, int tmpToId, int length) {
		return new InternalEdgeBuilder(tmpFromId, tmpToId, uniqueWayID, oppositeWayUniqueId, length,
				modeOfTransports, allowedMaxSpeedInKmh, lanesCount, coordinateList, otherParams);
	}

	public InternalEdgeBuilder copy(int tmpFromId, int tmpToId, int length, List<GPSLocation> coordinateList) {
		return new InternalEdgeBuilder(tmpFromId, tmpToId, uniqueWayID, oppositeWayUniqueId, length,
				modeOfTransports, allowedMaxSpeedInKmh, lanesCount, coordinateList, otherParams);
	}

	public boolean equalAttributes(InternalEdgeBuilder that) {
		return lanesCount == that.lanesCount
				&& (modeOfTransports != null
						? modeOfTransports.equals(that.modeOfTransports)
								&& Float.compare(that.allowedMaxSpeedInKmh, allowedMaxSpeedInKmh) == 0
						: that.modeOfTransports == null);
	}

	@Override
	public String toString() {
		return "{Edge id:" + uniqueWayID + ", id: " + otherParams.get("id") + '}';
	}
	
	

}
