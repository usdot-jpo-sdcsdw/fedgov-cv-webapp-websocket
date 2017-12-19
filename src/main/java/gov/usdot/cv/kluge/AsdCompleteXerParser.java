package gov.usdot.cv.kluge;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import gov.usdot.cv.mongodb.datasink.model.AdvisoryBroadcastType;
import gov.usdot.cv.mongodb.datasink.model.DialogId;
import gov.usdot.cv.mongodb.datasink.model.SequenceId;
import gov.usdot.cv.mongodb.datasink.model.TimeToLive;
import net.sf.json.JSONObject;

public class AsdCompleteXerParser
{
    
    public static class JsonFields {
        public static final String RECEIPT_ID           = "receiptId";
        public static final String DIALOG_ID            = "dialogId";
        public static final String SEQUENCE_ID          = "sequenceId";
        public static final String GROUP_ID             = "groupId";
        public static final String REQUEST_ID           = "requestId";
        public static final String RECORD_ID            = "recordId";
        public static final String TIME_TO_LIVE         = "timeToLive";
        public static final String NW_LAT               = "nwLat";
        public static final String NW_LON               = "nwLon";
        public static final String SE_LAT               = "seLat";
        public static final String SE_LON               = "seLon";
        public static final String ASDM_ID              = "asdmId";
        public static final String ASDM_TYPE            = "asdmType";
        public static final String DIST_TYPE            = "distType";
        public static final String START_TIME           = "startTime";
        public static final String STOP_TIME            = "stopTime";
        public static final String ADVISORY_MESSAGE     = "advisoryMessage";
        public static final String ENCODED_MSG          = "encodedMsg";
    }
    
    public static class XerFields {
        // TODO: What is this?
        //public static final String RECEIPT_ID           = "receiptId";
        
        
        public static final String DIALOG_ID            = MAKE_ASD_PATH("dialogID");
        public static final String SEQUENCE_ID          = MAKE_ASD_PATH("seqID");
        public static final String GROUP_ID             = MAKE_ASD_PATH("groupID");
        public static final String REQUEST_ID           = MAKE_ASD_PATH("requestID");
        public static final String RECORD_ID            = MAKE_ASD_PATH("recordID");
        public static final String TIME_TO_LIVE         = MAKE_ASD_PATH("timeToLive");
        public static final String NW_LAT               = MAKE_COORDINATE_PATH("nwCorner", "lat");
        public static final String NW_LON               = MAKE_COORDINATE_PATH("nwCorner", "long");
        public static final String SE_LAT               = MAKE_COORDINATE_PATH("seCorner", "lat");
        public static final String SE_LON               = MAKE_COORDINATE_PATH("seCorner", "long");
        public static final String ASDM_ID              = MAKE_ADVISORY_DETAILS_PATH("asdmID");
        public static final String ASDM_TYPE            = MAKE_ADVISORY_DETAILS_PATH("asdmType");
        public static final String DIST_TYPE            = MAKE_ADVISORY_DETAILS_PATH("distType");
        public static final String START_TIME           = MAKE_ADVISORY_DETAILS_PATH("startTime");
        public static final String STOP_TIME            = MAKE_ADVISORY_DETAILS_PATH("stopTime");
        public static final String ADVISORY_MESSAGE     = MAKE_ADVISORY_DETAILS_PATH("advisoryMessage");
        
        
        private static final String ASD_PATH = "/AdvisorySituationData";
        
        private static final String MAKE_ASD_PATH(String path)
        {
            return ASD_PATH + "/" + path;
        }
        
        private static final String SERVICE_REGION_PATH = "serviceRegion";
        
        private static final String NW_CORNER = "nwCorner";
        private static final String NE_CORNER = "neCorner";
        private static final String SW_CORNER = "swCorner";
        private static final String SE_CORNER = "seCorner";
        private static final String LAT = "lat";
        private static final String LONG = "long";
        private static final String MAKE_SERVICE_REGION_PATH(String path)
        {
            return MAKE_ASD_PATH(SERVICE_REGION_PATH + "/" + path);
        }
        
        private static final String MAKE_COORDINATE_PATH(String corner, String coord)
        {
            return MAKE_SERVICE_REGION_PATH(corner + "/" + coord);
        }
        
        private static final String ADVISORY_DETAILS_PATH = "asdmDetails";
        
        private static final String MAKE_ADVISORY_DETAILS_PATH(String path)
        {
            return MAKE_ASD_PATH(ADVISORY_DETAILS_PATH + "/" + path);
        }
    }
    
    
    
    public static void unpackAsdXer(JSONObject target, Document xer) throws XerJsonParserException
    {
        for (XerJsonExtractor extractor : extractors) {
            extractor.extract(target, xer);
        }
    }
    
    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    
    
    
    private static final XerJsonExtractor[] extractors = {
        new XerJsonExtractor(XerJsonParsers.EnumXerJsonParser(DialogId::valueOf, DialogId::getCode), JsonFields.DIALOG_ID, XerFields.DIALOG_ID),
        new XerJsonExtractor(XerJsonParsers.EnumXerJsonParser(SequenceId::valueOf, SequenceId::getCode), JsonFields.SEQUENCE_ID, XerFields.SEQUENCE_ID),
        new XerJsonExtractor(XerJsonParsers.HexIntXerJsonParser, JsonFields.GROUP_ID, XerFields.GROUP_ID),
        new XerJsonExtractor(XerJsonParsers.HexIntXerJsonParser, JsonFields.REQUEST_ID, XerFields.REQUEST_ID),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.HexIntXerJsonParser), JsonFields.RECORD_ID, XerFields.RECORD_ID),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.EnumXerJsonParser(TimeToLive::fromString)), JsonFields.TIME_TO_LIVE, XerFields.TIME_TO_LIVE),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), JsonFields.NW_LAT, XerFields.NW_LAT),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), JsonFields.NW_LON, XerFields.NW_LON),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), JsonFields.SE_LAT, XerFields.SE_LAT),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), JsonFields.SE_LON, XerFields.SE_LON),
        new XerJsonExtractor(XerJsonParsers.HexIntXerJsonParser, JsonFields.ASDM_ID, XerFields.ASDM_ID),
        new XerJsonExtractor(XerJsonParsers.EnumXerJsonParser(AdvisoryBroadcastType::valueOf), JsonFields.ASDM_TYPE, XerFields.ASDM_TYPE),
        new XerJsonExtractor(XerJsonParsers.BitIntXerJsonParser, JsonFields.DIST_TYPE, XerFields.DIST_TYPE),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.DateXerJsonParser), JsonFields.START_TIME, XerFields.START_TIME),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.DateXerJsonParser), JsonFields.STOP_TIME, XerFields.STOP_TIME),
        new XerJsonExtractor(XerJsonParsers.StringXerJsonParser, JsonFields.ADVISORY_MESSAGE, XerFields.ADVISORY_MESSAGE),
    };
}