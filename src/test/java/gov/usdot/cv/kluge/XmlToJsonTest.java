package gov.usdot.cv.kluge;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.HexPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.DocumentXerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.RawXerData;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class XmlToJsonTest
{

    @Test
    public void testXmlToJsonDocument() throws Exception
    {
        String testXml = "<A><B>C</B></A>";
        String testJson = "{ \"A\": { \"B\": \"C\" } }";
        
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(testXml.getBytes()));
        JSONObject expectedJson = (JSONObject)JSONSerializer.toJSON(testJson);
        JSONObject actualJson = AdvisorySituationDataXmlToJson.xmlToJson(doc);
        
        assertEquals(expectedJson, actualJson);
    }
    
    @Test
    public void testXmlToJsonASD() throws Exception
    {
        String testHexPer = "44400000000CB7605B26283B90A7148D2B0A89C49F8A85A7763BFE5BB02D92107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9CF914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600";
        
        Document xerDocument = PerXerCodec.perToXer(Asn1Types.AdvisorySituationDataType, testHexPer, HexPerData.unformatter, DocumentXerData.formatter);
        
        JSONObject json = AdvisorySituationDataXmlToJson.xmlToJson(xerDocument);
        
        System.out.println(json.toString());
        
        //assertTrue(false);
    }

}
