package gov.usdot.cv.kluge;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.CodecFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.FormattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.UnformattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.HexPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.DocumentXerData;
import net.sf.json.JSONObject;

public class AsdCompleteXerParserTest
{

    private static final String testHexPer = "44400000000CB7605B26283B90A7148D2B0A89C49F8A85A7763BFE5BB02D92107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9CF914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600";
    
    private static final JSONObject expectedJson;
    
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);
    
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        expectedJson = new JSONObject();
        expectedJson.put(AsdCompleteXerParser.JsonFields.DIALOG_ID, 156);
        expectedJson.put(AsdCompleteXerParser.JsonFields.SEQUENCE_ID, 5);
        expectedJson.put(AsdCompleteXerParser.JsonFields.GROUP_ID, 0);
        expectedJson.put(AsdCompleteXerParser.JsonFields.REQUEST_ID, 0xCB7605B2);
        expectedJson.put(AsdCompleteXerParser.JsonFields.TIME_TO_LIVE, 3);
        expectedJson.put(AsdCompleteXerParser.JsonFields.NW_LAT, 44.9984590);
        expectedJson.put(AsdCompleteXerParser.JsonFields.NW_LON, -111.0408170);
        expectedJson.put(AsdCompleteXerParser.JsonFields.SE_LAT, 41.1046740);
        expectedJson.put(AsdCompleteXerParser.JsonFields.SE_LON, -104.1113120);
        
        expectedJson.put(AsdCompleteXerParser.JsonFields.ASDM_ID, 0xCB7605B2);
        expectedJson.put(AsdCompleteXerParser.JsonFields.ASDM_TYPE, 2);
        expectedJson.put(AsdCompleteXerParser.JsonFields.DIST_TYPE, 0b10);
        
        try {
            expectedJson.put(AsdCompleteXerParser.JsonFields.START_TIME, DATE_FORMAT.format(DATE_FORMAT.parse("2017-12-01T17:47:00")));
            expectedJson.put(AsdCompleteXerParser.JsonFields.STOP_TIME, DATE_FORMAT.format(DATE_FORMAT.parse("2018-12-01T17:47:00")));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        
        expectedJson.put(AsdCompleteXerParser.JsonFields.ADVISORY_MESSAGE, "03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0");
    }
    
    @Test
    public void testUnpackAsdXer() throws XerJsonParserException, CodecFailedException, FormattingFailedException, UnformattingFailedException
    {
        Document xerDocument = PerXerCodec.perToXer(Asn1Types.AdvisorySituationDataType, testHexPer, HexPerData.unformatter, DocumentXerData.formatter);
        
        JSONObject json = new JSONObject();
        
        AsdCompleteXerParser.unpackAsdXer(json, xerDocument);
        
        assertEquals(expectedJson, json);
    }

}
