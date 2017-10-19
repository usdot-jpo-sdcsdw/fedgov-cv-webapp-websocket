package gov.usdot.cv.websocket.jms.filter;

import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;

import com.spatial4j.core.shape.Rectangle;

public class Filter {

	private final int dialogId;
	private final int vsmType;
	private final Rectangle boundingBox;
	private final String resultEncoding;
	
	public Filter(int dialogId, int vsmType, Rectangle boundingBox, String resultEncoding) {
		super();
		this.dialogId = dialogId;
		this.vsmType = (long)dialogId == SemiDialogID.vehSitData.longValue() ? vsmType : 0;
		this.boundingBox = boundingBox;
		this.resultEncoding = resultEncoding;
	}

	public int getDialogId() {
		return dialogId;
	}

	public int getVsmType() {
		return vsmType;
	}

	public Rectangle getBoundingBox() {
		return boundingBox;
	}

	public String getResultEncoding() {
		return resultEncoding;
	}
}
