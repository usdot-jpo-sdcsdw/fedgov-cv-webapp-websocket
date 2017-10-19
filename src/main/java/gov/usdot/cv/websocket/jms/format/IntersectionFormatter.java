package gov.usdot.cv.websocket.jms.format;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.oss.asn1.INTEGER;

import gov.usdot.asn1.generated.j2735.dsrc.AllowedManeuvers;
import gov.usdot.asn1.generated.j2735.dsrc.ApproachID;
import gov.usdot.asn1.generated.j2735.dsrc.Connection;
import gov.usdot.asn1.generated.j2735.dsrc.ConnectsToList;
import gov.usdot.asn1.generated.j2735.dsrc.GenericLane;
import gov.usdot.asn1.generated.j2735.dsrc.IntersectionGeometry;
import gov.usdot.asn1.generated.j2735.dsrc.IntersectionGeometryList;
import gov.usdot.asn1.generated.j2735.dsrc.IntersectionID;
import gov.usdot.asn1.generated.j2735.dsrc.IntersectionState;
import gov.usdot.asn1.generated.j2735.dsrc.LaneList;
import gov.usdot.asn1.generated.j2735.dsrc.MovementEventList;
import gov.usdot.asn1.generated.j2735.dsrc.MovementList;
import gov.usdot.asn1.generated.j2735.dsrc.MovementPhaseState;
import gov.usdot.asn1.generated.j2735.dsrc.MovementState;
import gov.usdot.asn1.generated.j2735.dsrc.NodeListXY;
import gov.usdot.asn1.generated.j2735.dsrc.NodeOffsetPointXY;
import gov.usdot.asn1.generated.j2735.dsrc.NodeSetXY;
import gov.usdot.asn1.generated.j2735.dsrc.NodeXY;
import gov.usdot.asn1.generated.j2735.dsrc.Node_LLmD_64b;
import gov.usdot.asn1.generated.j2735.dsrc.Node_XY_20b;
import gov.usdot.asn1.generated.j2735.dsrc.Node_XY_22b;
import gov.usdot.asn1.generated.j2735.dsrc.Node_XY_24b;
import gov.usdot.asn1.generated.j2735.dsrc.Node_XY_26b;
import gov.usdot.asn1.generated.j2735.dsrc.Node_XY_28b;
import gov.usdot.asn1.generated.j2735.dsrc.Node_XY_32b;
import gov.usdot.asn1.generated.j2735.dsrc.SignalGroupID;
import gov.usdot.asn1.generated.j2735.semi.IntersectionSituationData;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.common.asn1.model.GPSCoordinate;
import gov.usdot.cv.common.asn1.model.Intersection;
import gov.usdot.cv.common.asn1.model.IntersectionLane;
import net.sf.json.JSONObject;

public class IntersectionFormatter {

	private static Logger logger = Logger.getLogger(IntersectionFormatter.class);
	
	private static 
		Map<IntersectionID, Map<ApproachID, ApproachCacheElement>> intersectionIngressApproachCache
												= new HashMap<IntersectionID, Map<ApproachID, ApproachCacheElement>>();
	
	// Linking the SignalGroupIDs to all ApproachIDs they apply to saves iterations for every message later
	private static
		Map<IntersectionID, Map<SignalGroupID, Set<ApproachID>>> intersectionSignalGroupIDCache
												= new HashMap<IntersectionID, Map<SignalGroupID, Set<ApproachID>>>();
			
	public static JSONObject formatMessage(IntersectionSituationData intersectionData) {
		IntersectionGeometryList mapIntersections = intersectionData.getIntersectionRecord().getMapData().getIntersections();
		IntersectionState spatIntersectionStates = intersectionData.getIntersectionRecord().getSpatData().getIntersections();
		
		logger.debug("Received ISD : " + intersectionData);
		
		// do some basic validation, may not be ASN.1 required, but we need these to build the JSON
		if (mapIntersections == null || mapIntersections.getSize() == 0) {
			logger.error("Number of intersections is 0");
			return null;
		}
		// not sure if there will ever be multiple intersections in one of these messages, for now we just handle the first 
		IntersectionGeometry mapIntersection = mapIntersections.get(0);

		// create the intersection model object and set the reference point
		Intersection intersection = new Intersection();
		Double lat = J2735Util.convertGeoCoordinateToDouble(mapIntersection.getRefPoint().getLat().intValue());
		Double lon = J2735Util.convertGeoCoordinateToDouble(mapIntersection.getRefPoint().get_long().intValue());
		GPSCoordinate intersectionRefPoint = new GPSCoordinate(lat, lon);
		intersection.setRefPoint(intersectionRefPoint);
		
		// Check if we've already cached a map of the intersection lanes
		IntersectionID intersectionID = mapIntersection.getId().getId();
		if(!intersectionIngressApproachCache.containsKey(intersectionID)) {
			logger.debug("Intersection with ID " + intersectionID.intValue() + " is not yet cached.");
			addToCache(mapIntersection, intersectionRefPoint);
		}

		Map<ApproachID, IntersectionLane> intersectionLanes = new HashMap<ApproachID, IntersectionLane>();
		
		// Grab the cached map info to compare against the SPaT states to determine light's colors
		Map<ApproachID, ApproachCacheElement> ingressApproaches = intersectionIngressApproachCache.get(intersectionID);
		MovementList states = spatIntersectionStates.getStates(); 
		MovementState movementState = null;
		for(int i = 0; i < states.getSize(); i++) {
			movementState = states.get(i);
			SignalGroupID stateSignalGroupID = movementState.getSignalGroup();
			logger.debug("State with Signal Group ID " + stateSignalGroupID.intValue() + " found.");
			
			// Get the color of the signal based on the event state in the SPaT
			SignalColor stateSignalColor = SignalColor.UNKNOWN;
			MovementEventList eventList = movementState.getState_time_speed();
			// Not sure what to do here since there can be multiple event states in the event list.
			// For now: just grab info for the first event state.
			// Keeping the loop for now although it's not needed at the moment.
			for(int j = 0; j < eventList.getSize(); j++) {
				MovementPhaseState mps = eventList.get(j).getEventState();
				logger.debug("State contains Movement Phase State of " + mps);
				
				stateSignalColor = SignalColor.getByMovementPhaseState(mps);
				logger.debug("Movement Phase State maps to light color " + stateSignalColor);
				
				break;
			}
			
			// Work through all the approaches that the SignalGroupID is linked to
			if(intersectionSignalGroupIDCache.get(intersectionID).containsKey(stateSignalGroupID)) {
				for(ApproachID ingressApproachID : intersectionSignalGroupIDCache.get(intersectionID).get(stateSignalGroupID)) {
					ApproachCacheElement approachCacheElement = ingressApproaches.get(ingressApproachID);
					if(approachCacheElement.containsSignalGroupID(stateSignalGroupID)) {
						logger.debug("Ingress Approach with ID " + ingressApproachID.shortValue() +
									 " has Signal Group with ID " + stateSignalGroupID.shortValue());
						// Ensure that the cached element has the correct signal color for the signalGroupID
						approachCacheElement.ensureSignalGroupID(stateSignalGroupID, stateSignalColor);
						
						IntersectionLane intersectionLane = approachCacheElement.getIntersectionLane();
						intersectionLane.setLightColor(stateSignalColor.getColor());
						
						intersectionLanes.put(ingressApproachID, intersectionLane);
					}
					else {
						logger.warn(
								String.format(
									"Approach ID %d for Intersection ID %d has Signal Group ID linked to it, " +
									"but does not have Signal Group ID cached under the Approach.",
									ingressApproachID.intValue(), intersectionID.intValue()));
					}
				}
			}
			else {
				logger.debug("Unknown Signal Group ID " + stateSignalGroupID.shortValue() + " found in state.");
			}
		}
		
		for(IntersectionLane intersectionLane :  intersectionLanes.values()) {
			intersection.addLane(intersectionLane);
		}
		
		logger.debug("Final Intersection: " + intersection.toJSON().toString());
		
		return intersection.toJSON();
	}
	
	private static void addToCache(final IntersectionGeometry mapIntersection, final GPSCoordinate intersectionRefPoint) {
		IntersectionID intersectionID = mapIntersection.getId().getId();
		logger.debug("Adding Map Intersection with ID " + intersectionID.intValue() + " to Cache.");
		
		// Map for building the cache for the ingress approaches
		Map<ApproachID, ApproachCacheElement> ingressApproaches = new HashMap<ApproachID, ApproachCacheElement>();
		Map<ApproachID, Boolean> ingressApproachHasStraightLane = new HashMap<ApproachID, Boolean>();
		
		// Map for linking SignalGroupID to ApproachIDs
		Map<SignalGroupID, Set<ApproachID>> signalGroupIDsToApproachIDs = new HashMap<SignalGroupID, Set<ApproachID>>();
		
		LaneList laneList = mapIntersection.getLaneSet();
		for(int i = 0; i < laneList.getSize(); i++) {
			GenericLane lane = laneList.get(i);
			
			if(lane.hasIngressApproach()) {
				ApproachID approachID = lane.getIngressApproach();
				logger.debug("Intersection with ID " + intersectionID.intValue() + 
								": Found Ingress Aproach with ID " + approachID.shortValue());
				
				if(!ingressApproaches.containsKey(approachID)) {
					ingressApproaches.put(approachID, new ApproachCacheElement(approachID));
					ingressApproachHasStraightLane.put(approachID, false);
				}
				
				ApproachCacheElement approachCacheElement = ingressApproaches.get(approachID);
				
				// We want the GPS Coordinates of a straight lane to be the default lane representing an approach.
				// If no straight lane exists, than the last seen lane will be used.
				if(!ingressApproachHasStraightLane.get(approachID)) {
					// The approach isn't currently set with a straight lane, overwrite the current coordinates
					// to use this lane's coordinates
					GPSCoordinate gpsCoordinate = getGPSCoordinate(intersectionRefPoint, lane);
					approachCacheElement.getIntersectionLane().setCoordinate(gpsCoordinate);
					
					// Set if the lane is straight to prevent overwriting it with following lanes
					final boolean isStraightLane;
					if (lane.hasManeuvers()) {
						AllowedManeuvers maneuvers = lane.getManeuvers();
						String hexString = maneuvers.toHexString();
						if(hexString != null) {
							isStraightLane = hexString.equals("0000")  ?
												true :
												maneuvers.getBit(AllowedManeuvers.maneuverStraightAllowed);
						} else {
							// No maneuvers defined, assume the lane is straight
							isStraightLane = true;
						}
					}
					else {
						// No maneuvers defined, assume the lane is straight
						isStraightLane = true;
					}
					
					ingressApproachHasStraightLane.put(approachID, isStraightLane);
				}
				
				// Scan what lanes this lane connects to and see if there are any signal group IDs defined
				ConnectsToList connectsTo = lane.getConnectsTo();
				for(int j = 0; j < connectsTo.getSize(); j++) {
					Connection connection = connectsTo.get(j);
					if(connection.hasSignalGroup()) {
						SignalGroupID signalGroupID = connection.getSignalGroup();
						approachCacheElement.addSignalGroupID(signalGroupID);
						
						// Link the SignalGroupID to the approach
						if(!signalGroupIDsToApproachIDs.containsKey(signalGroupID)) {
							signalGroupIDsToApproachIDs.put(signalGroupID, new HashSet<ApproachID>());
						}
						signalGroupIDsToApproachIDs.get(signalGroupID).add(approachID);
						logger.debug("Intersection with ID " + intersectionID.intValue() +
										": Linked Signal Group ID " + signalGroupID.intValue() +
										" to Approach ID " + approachID.shortValue());
					}
				}
			}
			else if(lane.hasEgressApproach()) {
				logger.debug("Ignoring lane with ID " + lane.getLaneID().shortValue() + " because it is part of Egress Approach.");
			}
			else {
				logger.warn("Lane with ID " + lane.getLaneID().shortValue() + " does not have Egress or Ingress Approach ID specified.");
			}
		}
		
		intersectionIngressApproachCache.put(intersectionID, ingressApproaches);
		intersectionSignalGroupIDCache.put(intersectionID, signalGroupIDsToApproachIDs);
	}
	
	private static GPSCoordinate getGPSCoordinate(final GPSCoordinate intersectionRefPoint, final GenericLane genLane) {
		NodeListXY nodeList = genLane.getNodeList();
		GPSCoordinate nodeCoordinate = null;
		
		if (nodeList.hasNodes()) {
			NodeSetXY nodeSet = nodeList.getNodes();
			if (nodeSet.getSize() > 0) {
				NodeXY node = nodeSet.get(0);
				NodeOffsetPointXY delta = node.getDelta();

				// Explicit Lat/Lon used
				if(delta.hasNode_LatLon()) {
					Node_LLmD_64b nodeLatLon = delta.getNode_LatLon();
					double lat = J2735Util.convertGeoCoordinateToDouble(nodeLatLon.getLat().intValue());
					double lon = J2735Util.convertGeoCoordinateToDouble(nodeLatLon.getLon().intValue());
					nodeCoordinate = new GPSCoordinate(lat, lon);
				}
				// Lat/Lon to be calculated from offsets
				else if(delta.hasNode_XY6()) {
					Node_XY_32b nodeXY = delta.getNode_XY6();
					nodeCoordinate = calculateGPSCoordinateFromNodeXYCoordinates(intersectionRefPoint, nodeXY.getX(), nodeXY.getY());
				}
				else if(delta.hasNode_XY5()) {
					Node_XY_28b nodeXY = delta.getNode_XY5();
					nodeCoordinate = calculateGPSCoordinateFromNodeXYCoordinates(intersectionRefPoint, nodeXY.getX(), nodeXY.getY());
				}
				else if(delta.hasNode_XY4()) {
					Node_XY_26b nodeXY = delta.getNode_XY4();
					nodeCoordinate = calculateGPSCoordinateFromNodeXYCoordinates(intersectionRefPoint, nodeXY.getX(), nodeXY.getY());
				}
				else if(delta.hasNode_XY3()) {
					Node_XY_24b nodeXY = delta.getNode_XY3();
					nodeCoordinate = calculateGPSCoordinateFromNodeXYCoordinates(intersectionRefPoint, nodeXY.getX(), nodeXY.getY());
				}
				else if(delta.hasNode_XY2()) {
					Node_XY_22b nodeXY = delta.getNode_XY2();
					nodeCoordinate = calculateGPSCoordinateFromNodeXYCoordinates(intersectionRefPoint, nodeXY.getX(), nodeXY.getY());
				}
				else if(delta.hasNode_XY1()) {
					Node_XY_20b nodeXY = delta.getNode_XY1();
					nodeCoordinate = calculateGPSCoordinateFromNodeXYCoordinates(intersectionRefPoint, nodeXY.getX(), nodeXY.getY());
				}
				else {
					logger.error("Unexpected node encoding type");
				}
			}
		}
		else if(nodeList.hasComputed()) {
			// add code here -- not highlighted in the spreadsheet
			logger.error("Unhandled node list format. Add code here.");
		}
		
		if (nodeCoordinate == null) {
			nodeCoordinate = intersectionRefPoint;
		}
		
		return nodeCoordinate;
	}
	
	private static GPSCoordinate calculateGPSCoordinateFromNodeXYCoordinates(
											GPSCoordinate intersectionRefPoint, INTEGER xCoordinate, INTEGER yCoordinate) {
		double xOffsetInMeters = xCoordinate.intValue()/100.;
		double yOffsetInMeters = yCoordinate.intValue()/100.;
		
		return intersectionRefPoint.fromOffsets(xOffsetInMeters, yOffsetInMeters);
	}
	
	private enum SignalColor {
		GREEN("green",
				MovementPhaseState.pre_Movement,
				MovementPhaseState.permissive_Movement_Allowed,
				MovementPhaseState.protected_Movement_Allowed),
		YELLOW("yellow",
				MovementPhaseState.permissive_clearance,
				MovementPhaseState.protected_clearance,
				MovementPhaseState.caution_Conflicting_Traffic),
		RED("red",
				MovementPhaseState.stop_Then_Proceed,
				MovementPhaseState.stop_And_Remain),
		DARK("dark",
				MovementPhaseState.dark),
		UNAVAILABLE("unavailable",
				MovementPhaseState.unavailable),
		UNKNOWN(null);
		
		private Set<MovementPhaseState> movementPhaseStates;
		private String color;
		
		private SignalColor(String color, MovementPhaseState... movementPhaseStates) {
			this.color = color;
			this.movementPhaseStates = new HashSet<MovementPhaseState>();
			for(MovementPhaseState mps : movementPhaseStates) {
				this.movementPhaseStates.add(mps);
			}
		}
		
		public static SignalColor getByMovementPhaseState(MovementPhaseState mps) {
			for(SignalColor sc : values()) {
				if(sc.movementPhaseStates.contains(mps)) {
					return sc;
				}
			}
			
			return SignalColor.UNKNOWN;
		}
		
		public String getColor() {
			return color;
		}
	}
	
	private static class ApproachCacheElement {
		private Map<SignalGroupID, SignalColor> signalGroupIDs;
		private IntersectionLane intLane;
		
		public ApproachCacheElement(ApproachID approachID) {
			this.signalGroupIDs = new HashMap<SignalGroupID, SignalColor>();
			this.intLane = new IntersectionLane();
			this.intLane.setId(String.valueOf(approachID.shortValue()));
			this.intLane.setSignalType("straight");
		}
		
		public void addSignalGroupID(SignalGroupID signalGroupID) {
			// Like a set, only add if we haven't already
			if(!signalGroupIDs.containsKey(signalGroupID)) {
				signalGroupIDs.put(signalGroupID, SignalColor.UNKNOWN);
			}
		}
		
		public void ensureSignalGroupID(SignalGroupID signalGroupID, SignalColor color) {
			// Only update the signal color if it is currently unknown, otherwise assume
			// original value is correct
			if(signalGroupIDs.containsKey(signalGroupID) &&
					signalGroupIDs.get(signalGroupID) == SignalColor.UNKNOWN) {
				signalGroupIDs.put(signalGroupID, color);
			}
			else {
				signalGroupIDs.put(signalGroupID, color);
			}
		}
	
		public boolean containsSignalGroupID(SignalGroupID signalGroupID) {
			return signalGroupIDs.containsKey(signalGroupID);
		}
		
		public IntersectionLane getIntersectionLane() {
			return intLane;
		}
	}
}
