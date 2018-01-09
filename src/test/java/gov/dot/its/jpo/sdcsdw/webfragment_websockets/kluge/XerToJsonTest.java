package gov.dot.its.jpo.sdcsdw.webfragment_websockets.kluge;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.HexPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.DocumentXerData;
import gov.dot.its.jpo.sdcsdw.webfragment_websockets.kluge.XerToJson;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class XerToJsonTest
{

    @Test
    public void testXmlToJsonDocument() throws Exception
    {
        String testXml = "<A><B>C</B></A>";
        String testJson = "{ \"A\": { \"B\": \"C\" } }";
        
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(testXml.getBytes()));
        JSONObject expectedJson = (JSONObject)JSONSerializer.toJSON(testJson);
        JSONObject actualJson = XerToJson.xmlToJson(doc);
        
        assertEquals(expectedJson, actualJson);
    }
    
    @Test
    public void testXmlToJsonASD() throws Exception
    {
        String testHexPer = "44400000000CB7605B26283B90A7148D2B0A89C49F8A85A7763BFE5BB02D92107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9CF914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600";
        
        // {"AdvisorySituationData":{"dialogID":"advSitDataDep","seqID":"data","groupID":"00000000","requestID":"CB7605B2","timeToLive":"week","serviceRegion":{"nwCorner":{"lat":"449984590","long":"-1110408170"},"seCorner":{"lat":"411046740","long":"-1041113120"}},"asdmDetails":{"asdmID":"CB7605B2","asdmType":"tim","distType":"10","startTime":{"year":"2017","month":"12","day":"1","hour":"17","minute":"47"},"stopTime":{"year":"2018","month":"12","day":"1","hour":"17","minute":"47"},"advisoryMessage":"03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0"}}}
        Document xerDocument = PerXerCodec.perToXer(Asn1Types.AdvisorySituationDataType, testHexPer, HexPerData.unformatter, DocumentXerData.formatter);
        
        JSONObject json = XerToJson.xmlToJson(xerDocument);
        
        System.out.println(json.toString());
        
        //assertTrue(false);
    }

}
