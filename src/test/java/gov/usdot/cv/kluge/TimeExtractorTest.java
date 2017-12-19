package gov.usdot.cv.kluge;

import static org.junit.Assert.*;

import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.CodecFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.FormattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.UnformattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.HexPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.DocumentXerData;
import gov.usdot.cv.mongodb.datasink.model.TimeToLive;

public class TimeExtractorTest
{
    
    private static final String testHexPer = "44400000000CB7605B26283B90A7148D2B0A89C49F8A85A7763BFE5BB02D92107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9CF914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600";

    private static final Date expectedStartTime;
    private static final Date expectedStopTime;
    private static final TimeToLive expectedTimeToLive = TimeToLive.Week;
    
    static {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MILLISECOND, 0);
        
        cal.set(2017, 12-1, 1, 17, 47, 0);
        
        expectedStartTime = cal.getTime();
        
        cal.set(2018, 12-1, 1, 17, 47, 0);
        
        expectedStopTime = cal.getTime();
    }
    
    @Test
    public void testExtractDateTime() throws CodecFailedException, FormattingFailedException, UnformattingFailedException
    {
        Document xerDocument = PerXerCodec.perToXer(Asn1Types.AdvisorySituationDataType, testHexPer, HexPerData.unformatter, DocumentXerData.formatter);
        
        Date startTime = TimeExtractor.extractDateTime(xerDocument, TimeExtractor.START_TIME);
        Date stopTime = TimeExtractor.extractDateTime(xerDocument, TimeExtractor.STOP_TIME);
        TimeToLive timeToLive = TimeExtractor.extractTimeToLiveCode(xerDocument);
                
        
        assertEquals(expectedStartTime, startTime);
        assertEquals(expectedStopTime, stopTime);
        assertEquals(expectedTimeToLive, timeToLive);
    }

}
