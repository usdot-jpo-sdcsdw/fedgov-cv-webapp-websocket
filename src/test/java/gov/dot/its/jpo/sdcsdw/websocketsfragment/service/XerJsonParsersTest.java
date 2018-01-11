package gov.dot.its.jpo.sdcsdw.websocketsfragment.service;

import static org.junit.Assert.*;
import org.junit.rules.ExpectedException;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Test;

import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.DocumentXerData;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParser;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParserBadTypeException;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParsers;
import net.sf.json.JSONObject;

public class XerJsonParsersTest
{
    
    @Test
    public void testString() throws Exception
    {
        doTest("x", "Hello, World!", XerJsonParsers.StringXerJsonParser, JSONObject::getString);
    }
    
    @Test
    public void testEnum() throws Exception
    {
        Function<String, TestEnum> nameMap = (String s) -> {
            switch(s.toUpperCase()) {
            case "A": return TestEnum.A;
            case "B": return TestEnum.B;
            case "C": return TestEnum.C;
            default: return null;
            }
        };
        Function <TestEnum, Integer> intMap = (TestEnum e) -> {
            switch(e) {
            case A: return 10;
            case B: return 20;
            case C: return 30;
            default: return null;
            }
        };
        
        doTest("x", Integer.toString(TestEnum.A.ordinal()), "<A/>", XerJsonParsers.EnumXerJsonParser(nameMap), JSONObject::getString);
        doTest("x", Integer.toString(TestEnum.A.ordinal()), "<a/>", XerJsonParsers.EnumXerJsonParser(nameMap), JSONObject::getString);
        doTest("x", Integer.toString(10), "<A/>", XerJsonParsers.EnumXerJsonParser(nameMap, intMap), JSONObject::getString);
        doTest("x", Integer.toString(10), "<a/>", XerJsonParsers.EnumXerJsonParser(nameMap, intMap), JSONObject::getString);
    }
    
    @Test
    public void testBadEnum() throws Exception
    {
        Function<String, TestEnum> nameMap = (String s) -> {
            switch(s.toUpperCase()) {
            case "A": return TestEnum.A;
            case "B": return TestEnum.B;
            case "C": return TestEnum.C;
            default: return null;
            }
        };
        Function <TestEnum, Integer> intMap = (TestEnum e) -> {
            switch(e) {
            case A: return 10;
            case B: return 20;
            case C: return 30;
            default: return null;
            }
        };
        
        doBadTest("x", "<D/>", XerJsonParsers.EnumXerJsonParser(nameMap), XerJsonParserBadTypeException.class);
        doBadTest("x", "10", XerJsonParsers.EnumXerJsonParser(nameMap), XerJsonParserBadTypeException.class);
        doBadTest("x", "", XerJsonParsers.EnumXerJsonParser(nameMap), XerJsonParserBadTypeException.class);
    }
    
    @Test
    public void testLong() throws Exception
    {
        doTest("x", (long)1000, XerJsonParsers.LongXerJsonParser, JSONObject::getLong);
    }
    
    @Test
    public void testBadLong() throws Exception
    {
        doBadTest("x", "asdf", XerJsonParsers.LongXerJsonParser, XerJsonParserBadTypeException.class);
    }

    @Test
    public void testHexInt() throws Exception
    {
        doTest("x", 255, "FF", XerJsonParsers.HexIntXerJsonParser, JSONObject::getInt);
    }
    
    @Test
    public void testBadHexInt() throws Exception
    {
        doBadTest("x", "asdf", XerJsonParsers.HexIntXerJsonParser, XerJsonParserBadTypeException.class);
    }
    
    @Test
    public void testBitInt() throws Exception
    {
        doTest("x", 255, "11111111", XerJsonParsers.BitIntXerJsonParser, JSONObject::getInt);
    }
    
    @Test
    public void testBadBitInt() throws Exception
    {
        doBadTest("x", "asdf", XerJsonParsers.BitIntXerJsonParser, XerJsonParserBadTypeException.class);
    }
    
    @Test
    public void testDouble() throws Exception
    {
        doTest("x", 0.03, XerJsonParsers.DoubleXerJsonParser, JSONObject::getDouble);
    }
    
    @Test
    public void testBadDouble() throws Exception
    {
        doBadTest("x", "asdf", XerJsonParsers.DoubleXerJsonParser, XerJsonParserBadTypeException.class);
    }
    
    @Test
    public void testCoordinate() throws Exception
    {
        doTest("x", 3.14, "31400000", XerJsonParsers.CoordinateXerJsonParser, JSONObject::getDouble);
    }
    
    @Test
    public void testBadCoordinate() throws Exception
    {
        doBadTest("x", "asdf", XerJsonParsers.CoordinateXerJsonParser, XerJsonParserBadTypeException.class);
    }
    
    @Test
    public void testDate() throws Exception
    {
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        final String xml = "<year>" + cal.get(Calendar.YEAR) + "</year>"
                         + "<month>" + (cal.get(Calendar.MONTH) + 1) + "</month>"
                         + "<day>" + cal.get(Calendar.DAY_OF_MONTH) + "</day>"
                         + "<hour>" + cal.get(Calendar.HOUR_OF_DAY) + "</hour>"
                         + "<minute>" + cal.get(Calendar.MINUTE) + "</minute>";
        final String date = String.format("%04d-%02d-%02dT%02d:%02d:00", 
                                          cal.get(Calendar.YEAR), 
                                          cal.get(Calendar.MONTH)+1,
                                          cal.get(Calendar.DAY_OF_MONTH),
                                          cal.get(Calendar.HOUR_OF_DAY),
                                          cal.get(Calendar.MINUTE));
        
        doTest("x", date, xml, XerJsonParsers.DateXerJsonParser, JSONObject::getString);
    }
    
    @Test
    public void testBadDate() throws Exception
    {
        doBadTest("x", "asdf", XerJsonParsers.DateXerJsonParser, XerJsonParserBadTypeException.class);
        doBadTest("x", "<year>asdf</year>", XerJsonParsers.DateXerJsonParser, XerJsonParserBadTypeException.class);
    }
    
    private enum TestEnum { A, B, C };
    
    private <T> void doTest(String field, T value, XerJsonParser parser, BiFunction<JSONObject, String, T> get) throws Exception
    {
        doTest(field, value, value.toString(), parser, get);
    }
    
    private <T> void doTest(String field, T value, String valueString, XerJsonParser parser, BiFunction<JSONObject, String, T> get) throws Exception
    {
        final String path = "/" + field;
        final Document xer = new DocumentXerData("<" + field + ">" + valueString + "</" + field + ">").getFormattedXerData();
        
        final JSONObject json = new JSONObject();
        parser.parseXer(json, field, xer, path);
        
        assertEquals(value, get.apply(json, field));
    }
    
    private void doBadTest(String field, String valueString, XerJsonParser parser, Class<? extends Throwable> expect) throws Exception
    {
        try {
            doTest(field, new Object(), valueString, parser, JSONObject::getString);
        } catch (Throwable t) {
            if (expect.isInstance(t)) {
                return;
            }
            throw t;
        }
        fail("Did not throw " + expect + " as expected");
    }
}
