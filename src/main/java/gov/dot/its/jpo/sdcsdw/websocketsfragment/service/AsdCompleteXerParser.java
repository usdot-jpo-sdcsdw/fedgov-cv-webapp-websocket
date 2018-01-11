package gov.dot.its.jpo.sdcsdw.websocketsfragment.service;

import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.model.AdvisoryBroadcastType;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.model.DialogId;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.model.SequenceId;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.model.TimeToLive;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.FieldPathPair;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonExtractor;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParserException;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParsers;
import net.sf.json.JSONObject;

/** Static methods for extracting ASD fields from XER into a JSON object
 * 
 * @author amm30955
 *
 */
public class AsdCompleteXerParser
{
    /** JSON keys and XER XPath Paths for each ASD field */
    public static class Fields
    {
        //public static final FieldPathPair RECEIPT_ID
        //    = new FieldPathPair("receiptId", );
        public static final FieldPathPair DIALOG_ID
            = new FieldPathPair("dialogId", MAKE_ASD_PATH("dialogID"));
        public static final FieldPathPair SEQUENCE_ID 
            = new FieldPathPair("sequenceId", MAKE_ASD_PATH("seqID"));
        public static final FieldPathPair GROUP_ID
            = new FieldPathPair("groupId", MAKE_ASD_PATH("groupID"));
        public static final FieldPathPair REQUEST_ID
            = new FieldPathPair("requestId", MAKE_ASD_PATH("requestID"));
        public static final FieldPathPair RECORD_ID
            = new FieldPathPair("recordId", MAKE_ASD_PATH("recordID"));
        public static final FieldPathPair TIME_TO_LIVE
            = new FieldPathPair("timeToLive", MAKE_ASD_PATH("timeToLive"));
        public static final FieldPathPair NW_LAT
            = new FieldPathPair("nwLat", MAKE_COORDINATE_PATH("nwCorner", "lat"));
        public static final FieldPathPair NW_LON
            = new FieldPathPair("nwLon", MAKE_COORDINATE_PATH("nwCorner", "long"));
        public static final FieldPathPair SE_LAT
            = new FieldPathPair("seLat", MAKE_COORDINATE_PATH("seCorner", "lat"));
        public static final FieldPathPair SE_LON
            = new FieldPathPair("seLon", MAKE_COORDINATE_PATH("seCorner", "long"));
        public static final FieldPathPair ASDM_ID
            = new FieldPathPair("asdmId", MAKE_ADVISORY_DETAILS_PATH("asdmID"));
        public static final FieldPathPair ASDM_TYPE
            = new FieldPathPair("asdmType", MAKE_ADVISORY_DETAILS_PATH("asdmType"));
        public static final FieldPathPair DIST_TYPE
            = new FieldPathPair("distType", MAKE_ADVISORY_DETAILS_PATH("distType"));
        public static final FieldPathPair START_TIME
            = new FieldPathPair("startTime", MAKE_ADVISORY_DETAILS_PATH("startTime"));
        public static final FieldPathPair STOP_TIME
            = new FieldPathPair("stopTime", MAKE_ADVISORY_DETAILS_PATH("stopTime"));
        public static final FieldPathPair ADVISORY_MESSAGE
            = new FieldPathPair("advisoryMessage", MAKE_ADVISORY_DETAILS_PATH("advisoryMessage"));
            
        
        /** The base of each XPath */
        private static final String ASD_PATH = "/AdvisorySituationData";
        
        private static final String MAKE_ASD_PATH(String path)
        {
            return ASD_PATH + "/" + path;
        }
        
        private static final String SERVICE_REGION_PATH = "serviceRegion";
        
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
    
    /** Extract ASD fields from XER into JSON
     * 
     * @param target JSON object to unpack into
     * @param xer XER to extract ASD fields from
     * @throws XerJsonParserException If the XER was malformed
     */
    public static void unpackAsdXer(JSONObject target, Document xer) throws XerJsonParserException
    {
        for (XerJsonExtractor extractor : extractors) {
            extractor.extract(target, xer);
        }
    }
    
    /** Extract ASD fields from XER into JSON
     * 
     * @param xer XER to extract ASD fields from
     * @throws XerJsonParserException If the XER was malformed
     * @return The unpacked JSON object
     */
    public static JSONObject unpackAsdXer(Document xer) throws XerJsonParserException
    {
        JSONObject object = new JSONObject();
        
        unpackAsdXer(object, xer);
        
        return object;
    }
    
    /** Extractors for each field in an ASD
     * 
     */
    private static final XerJsonExtractor[] extractors = {
        new XerJsonExtractor(XerJsonParsers.EnumXerJsonParser(DialogId::valueOf, DialogId::getCode), Fields.DIALOG_ID),
        new XerJsonExtractor(XerJsonParsers.EnumXerJsonParser(SequenceId::valueOf, SequenceId::getCode), Fields.SEQUENCE_ID),
        new XerJsonExtractor(XerJsonParsers.HexIntXerJsonParser, Fields.GROUP_ID),
        new XerJsonExtractor(XerJsonParsers.HexIntXerJsonParser, Fields.REQUEST_ID),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.HexIntXerJsonParser), Fields.RECORD_ID),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.EnumXerJsonParser(TimeToLive::fromString)), Fields.TIME_TO_LIVE),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), Fields.NW_LAT),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), Fields.NW_LON),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), Fields.SE_LAT),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.CoordinateXerJsonParser), Fields.SE_LON),
        new XerJsonExtractor(XerJsonParsers.HexIntXerJsonParser, Fields.ASDM_ID),
        new XerJsonExtractor(XerJsonParsers.EnumXerJsonParser(AdvisoryBroadcastType::valueOf), Fields.ASDM_TYPE),
        new XerJsonExtractor(XerJsonParsers.BitIntXerJsonParser, Fields.DIST_TYPE),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.DateXerJsonParser), Fields.START_TIME),
        new XerJsonExtractor(XerJsonParsers.optional(XerJsonParsers.DateXerJsonParser), Fields.STOP_TIME),
        new XerJsonExtractor(XerJsonParsers.StringXerJsonParser, Fields.ADVISORY_MESSAGE),
    };
}