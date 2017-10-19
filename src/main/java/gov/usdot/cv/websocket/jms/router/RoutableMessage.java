package gov.usdot.cv.websocket.jms.router;

import java.util.Calendar;

import org.apache.log4j.Logger;

import com.oss.asn1.AbstractData;
import com.oss.asn1.Coder;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Rectangle;
import com.spatial4j.core.shape.SpatialRelation;

import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.generated.j2735.dsrc.DDateTime;
import gov.usdot.asn1.generated.j2735.dsrc.Position3D;
import gov.usdot.asn1.generated.j2735.semi.IntersectionSituationData;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.asn1.generated.j2735.semi.VehSitRecord;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.websocket.jms.filter.Filter;

public class RoutableMessage {

	private static Logger logger = Logger.getLogger(RoutableMessage.class);
	private final static SpatialContext ctx = SpatialContext.GEO;
	
	private static Coder coder;
	
	static {
		try {
			J2735.initialize();
			coder = J2735.getPERUnalignedCoder();
		} catch (Exception e) {
			logger.error("Error initializing OSS parser", e);
		}
	}
	
	private int dialogId;
	private int vsmType;
	private double nwLat;
	private double nwLon;
	private double seLat;
	private double seLon;
	private AbstractData message;
	private byte[] rawMessage;
	private long timestamp;
	
	public RoutableMessage(byte[] message) throws DecodeFailedException, DecodeNotSupportedException {
		this.rawMessage = message;
		this.message = J2735Util.decode(coder, message);
		
		if (this.message instanceof VehSitDataMessage) {
			VehSitDataMessage vsd = (VehSitDataMessage)this.message;
			if (vsd.getType().byteArrayValue().length > 0) {
				vsmType = vsd.getType().byteArrayValue()[0];
			}
			
			if (vsd.getBundle() != null && vsd.getBundle().getSize() > 0) {
				VehSitRecord vsr = vsd.getBundle().get(0);
				Position3D pos = vsr.getPos();
				nwLat = J2735Util.convertGeoCoordinateToDouble(pos.getLat().intValue());
				nwLon = J2735Util.convertGeoCoordinateToDouble(pos.get_long().intValue());
				timestamp = getTime(vsr.getTime());
			} else {
				nwLat = nwLon = Double.NaN;
				timestamp = System.currentTimeMillis();
			}
			
			this.dialogId = (int)vsd.getDialogID().longValue();
			
		} else if (this.message instanceof IntersectionSituationData) {
			IntersectionSituationData isd = (IntersectionSituationData)this.message;
			nwLat = J2735Util.convertGeoCoordinateToDouble(isd.getServiceRegion().getNwCorner().getLat().intValue());
			nwLon = J2735Util.convertGeoCoordinateToDouble(isd.getServiceRegion().getNwCorner().get_long().intValue());
			seLat = J2735Util.convertGeoCoordinateToDouble(isd.getServiceRegion().getSeCorner().getLat().intValue());
			seLon = J2735Util.convertGeoCoordinateToDouble(isd.getServiceRegion().getSeCorner().get_long().intValue());
			timestamp = getTime(isd.getIntersectionRecord().getSpatData().getTimestamp());

			this.dialogId = (int)isd.getDialogID().longValue();
			
		} else {
			logger.warn(String.format("Received unsupported message of type: %s", this.message.getClass().getName()));
			this.dialogId = (int)SemiDialogID.reserved1.getUnknownEnumerator().longValue();
		}
	}
	
	private long getTime(DDateTime dateTime) {
		Calendar cal = Calendar.getInstance();
		cal.set(dateTime.getYear().intValue(), dateTime.getMonth().intValue()-1, dateTime.getDay().intValue(),
				dateTime.getHour().intValue(), dateTime.getMinute().intValue(), dateTime.getSecond().intValue());
		return cal.getTimeInMillis();
	}
	
	private boolean dialogIdMatches(Filter filter) {
		return ((filter.getDialogId() == -1) || (filter.getDialogId() == this.dialogId));
	}
	
	private boolean vsmTypeMatches(Filter filter) {
		return (filter.getVsmType() == 0 || 
				((filter.getVsmType() & this.vsmType) > 0));
	}
	
	private boolean regionMatches(Filter filter) {
		boolean matches = false;
		if (filter.getBoundingBox() == null) {
			matches = true;
		} else {
			if (this.dialogId == SemiDialogID.vehSitData.longValue()) {
				Point vehSitPoint = ctx.makePoint(this.nwLon, this.nwLat);
				if (filter.getBoundingBox().relate(vehSitPoint) == SpatialRelation.CONTAINS ||
					filter.getBoundingBox().relate(vehSitPoint) == SpatialRelation.INTERSECTS) {
					matches = true;
				}
			} else if (this.dialogId == SemiDialogID.intersectionSitDataDep.longValue()) {
				Rectangle intersectionBox = ctx.makeRectangle(this.nwLon,
						this.seLon, this.seLat, this.nwLat);
				if (filter.getBoundingBox().relate(intersectionBox) == SpatialRelation.CONTAINS ||
					filter.getBoundingBox().relate(intersectionBox) == SpatialRelation.INTERSECTS) {
					matches = true;
				}
			} 
		}
		return matches;
	}
	
	public boolean matches(Filter filter) {
		return (dialogIdMatches(filter) && vsmTypeMatches(filter) && regionMatches(filter));
	}

	public byte[] getRawMessage() {
		return rawMessage;
	}
	
	public AbstractData getMessage() {
		return message;
	}

	public int getDialogId() {
		return dialogId;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
