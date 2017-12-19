package gov.usdot.cv.kluge;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.CodecFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.FormattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.UnformattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.HexPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.DocumentXerData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GeoJsonExtractorTest
{

    private static final String testHexPer = "44400000000CB7605B26283B90A7148D2B0A89C49F8A85A7763BFE5BB02D92107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9CF914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600"; 
    
    private static final JSONObject expectedRegion;
    
    private static final double expectedWest  =   44.9984590;
    private static final double expectedNorth = -111.0408170;
    private static final double expectedEast  =   41.1046740;
    private static final double expectedSouth = -104.1113120;
    
    static {
        JSONArray expectedCoordinates = new JSONArray();
        JSONArray nw = new JSONArray();
        JSONArray ne = new JSONArray();
        JSONArray se = new JSONArray();
        JSONArray sw = new JSONArray();
        
        
        nw.add(expectedNorth);
        nw.add(expectedWest);
        ne.add(expectedNorth);
        ne.add(expectedEast);
        se.add(expectedSouth);
        se.add(expectedEast);
        sw.add(expectedSouth);
        sw.add(expectedWest);
        
        expectedCoordinates.add(nw);
        expectedCoordinates.add(ne);
        expectedCoordinates.add(se);
        expectedCoordinates.add(sw);
        expectedCoordinates.add(nw);
        
        
        JSONArray expectedCoordinatesWrapper = new JSONArray();
        expectedCoordinatesWrapper.add(expectedCoordinates);
        
        expectedRegion = new JSONObject();
        expectedRegion.put("type", "Polygon");
        expectedRegion.put("coordinates", expectedCoordinatesWrapper);
    }
    
    @Test
    public void testBuildRegionFromAsdXml() throws CodecFailedException, FormattingFailedException, UnformattingFailedException
    {
        
        
        Document xerDocument = PerXerCodec.perToXer(Asn1Types.AdvisorySituationDataType, testHexPer, HexPerData.unformatter, DocumentXerData.formatter);
        
        JSONObject region = GeoJsonExtractor.buildRegionFromAsdXml(xerDocument);
        
        assertEquals(expectedRegion, region);
    }

}
