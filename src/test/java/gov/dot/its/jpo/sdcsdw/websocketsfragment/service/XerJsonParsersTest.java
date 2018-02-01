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
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParserPathMissingException;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParsers;
import net.sf.json.JSONObject;

public class XerJsonParsersTest
{
    private enum TestEnum { A, B, C };
    static final Function<String, TestEnum> enumNameMap = (String s) -> {
        switch(s.toUpperCase()) {
        case "A": return TestEnum.A;
        case "B": return TestEnum.B;
        case "C": return TestEnum.C;
        default: return null;
        }
    };
    static final Function <TestEnum, Integer> enumIntMap = (TestEnum e) -> {
        switch(e) {
        case A: return 10;
        case B: return 20;
        case C: return 30;
        default: return null;
        }
    };
    
    static final Calendar testCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    static final String testDateXml = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
                     + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
                     + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
                     + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
                     + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlSansYear = "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlSansMonth = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlSansDay = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlSansHour = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlSansMinute = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>";
    static final String testDate = String.format("%04d-%02d-%02dT%02d:%02d:00", 
                                      testCalendar.get(Calendar.YEAR), 
                                      testCalendar.get(Calendar.MONTH)+1,
                                      testCalendar.get(Calendar.DAY_OF_MONTH),
                                      testCalendar.get(Calendar.HOUR_OF_DAY),
                                      testCalendar.get(Calendar.MINUTE));
    static final String testDateXmlBadYear = "<year>" + "asdf" + "</year>"
            + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlBadMonth = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<month>" + "asdf"+ "</month>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlBadDay = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<day>" + "asdf" + "</day>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlBadHour = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<hour>" + "asdf" + "</hour>"
            + "<minute>" + testCalendar.get(Calendar.MINUTE) + "</minute>";
    static final String testDateXmlBadMinute = "<year>" + testCalendar.get(Calendar.YEAR) + "</year>"
            + "<month>" + (testCalendar.get(Calendar.MONTH) + 1) + "</month>"
            + "<day>" + testCalendar.get(Calendar.DAY_OF_MONTH) + "</day>"
            + "<hour>" + testCalendar.get(Calendar.HOUR_OF_DAY) + "</hour>"
            + "<minute>" + "asdf" + "</minute>";
    static final ParserTestHarness<String> stringTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.StringXerJsonParser, JSONObject::getString);
    static final ParserTestHarness<String> enumTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.EnumXerJsonParser(enumNameMap), JSONObject::getString);
    static final ParserTestHarness<String> enumTestHarnessRemapped = new ParserTestHarness<>("x", "y", XerJsonParsers.EnumXerJsonParser(enumNameMap, enumIntMap), JSONObject::getString);
    static final ParserTestHarness<Long> longTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.LongXerJsonParser, JSONObject::getLong);
    static final ParserTestHarness<Integer> hexIntTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.HexIntXerJsonParser, JSONObject::getInt);
    static final ParserTestHarness<Integer> bitIntTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.BitIntXerJsonParser, JSONObject::getInt);
    static final ParserTestHarness<Double> doubleTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.DoubleXerJsonParser, JSONObject::getDouble);
    static final ParserTestHarness<Double> coordinateTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.CoordinateXerJsonParser, JSONObject::getDouble);
    static final ParserTestHarness<String> dateTestHarness = new ParserTestHarness<>("x", "y", XerJsonParsers.DateXerJsonParser, JSONObject::getString);
           
    
    @Test
    public void testString() throws Exception
    {
        stringTestHarness.doRequiredPresentValidTest("Hello, World!");
    }
    
    @Test
    public void testOptionalStringPresent() throws Exception
    {
        stringTestHarness.doOptionalPresentValidTest("Hello, World!");
    }
    
    @Test
    public void testOptionalStringMissing() throws Exception
    {
        stringTestHarness.doOptionalMissingTest("Hello, World!");
    }
    
    @Test
    public void testEnum() throws Exception
    {
        enumTestHarness.doRequiredPresentValidTest(Integer.toString(TestEnum.A.ordinal()), "<A/>");
        enumTestHarness.doRequiredPresentValidTest(Integer.toString(TestEnum.A.ordinal()), "<a/>");
        enumTestHarnessRemapped.doRequiredPresentValidTest(Integer.toString(10), "<A/>");
        enumTestHarnessRemapped.doRequiredPresentValidTest(Integer.toString(10), "<a/>");
    }
    
    @Test
    public void testOptionalEnumPresent() throws Exception
    {
        enumTestHarness.doOptionalPresentValidTest(Integer.toString(TestEnum.A.ordinal()), "<A/>");
        enumTestHarness.doOptionalPresentValidTest(Integer.toString(TestEnum.A.ordinal()), "<a/>");
        enumTestHarnessRemapped.doOptionalPresentValidTest(Integer.toString(10), "<A/>");
        enumTestHarnessRemapped.doOptionalPresentValidTest(Integer.toString(10), "<a/>");
    }
    
    @Test
    public void testOptionalEnumMissing() throws Exception
    {
        enumTestHarness.doOptionalMissingTest(Integer.toString(TestEnum.A.ordinal()), "<A/>");
        enumTestHarness.doOptionalMissingTest(Integer.toString(TestEnum.A.ordinal()), "<a/>");
        enumTestHarnessRemapped.doOptionalMissingTest(Integer.toString(10), "<A/>");
        enumTestHarnessRemapped.doOptionalMissingTest(Integer.toString(10), "<a/>");
    }
    
    @Test
    public void testBadEnum() throws Exception
    {
        enumTestHarness.doRequiredPresentInvalidTest(Integer.toString(TestEnum.A.ordinal()), "<D/>");
        enumTestHarness.doRequiredPresentInvalidTest(Integer.toString(TestEnum.A.ordinal()), "10");
        enumTestHarnessRemapped.doRequiredPresentInvalidTest(Integer.toString(10), "");
    }
    
    @Test
    public void testBadOptionalEnum() throws Exception
    {
        enumTestHarness.doOptionalPresentInvalidTest(Integer.toString(TestEnum.A.ordinal()), "<D/>");
        enumTestHarness.doOptionalPresentInvalidTest(Integer.toString(TestEnum.A.ordinal()), "10");
        enumTestHarness.doOptionalPresentInvalidTest(Integer.toString(10), "");
    }
    
    @Test
    public void testLong() throws Exception
    {
        longTestHarness.doRequiredPresentValidTest((long)1000);
    }
    
    @Test
    public void testOptionalLongPresent() throws Exception
    {
        longTestHarness.doOptionalPresentValidTest((long)1000);
    }
    
    @Test
    public void testOptionalLongMissing() throws Exception
    {
        longTestHarness.doOptionalMissingTest((long)1000);
    }
    
    @Test
    public void testBadLong() throws Exception
    {
        longTestHarness.doRequiredPresentInvalidTest((long)1000, "asdf");
    }
    
    @Test
    public void testBadOptionalLong() throws Exception
    {
        longTestHarness.doOptionalPresentInvalidTest((long)1000, "asdf");
    }

    @Test
    public void testHexInt() throws Exception
    {
        hexIntTestHarness.doRequiredPresentValidTest(255, "FF");
        hexIntTestHarness.doRequiredPresentValidTest(255, "ff");
    }
    
    @Test
    public void testOptionalPresentHexInt() throws Exception
    {
        hexIntTestHarness.doOptionalPresentValidTest(255, "FF");
        hexIntTestHarness.doOptionalPresentValidTest(255, "ff");
    }
    
    @Test
    public void testOptionalMissingHexInt() throws Exception
    {
        hexIntTestHarness.doOptionalMissingTest(255, "FF");
    }
    
    @Test
    public void testBadHexInt() throws Exception
    {
        hexIntTestHarness.doRequiredPresentInvalidTest(255, "asdf");
    }
    
    @Test
    public void testBadOptionalHexInt() throws Exception
    {
        hexIntTestHarness.doOptionalPresentInvalidTest(255, "asdf");
    }
    
    @Test
    public void testBitInt() throws Exception
    {
        bitIntTestHarness.doRequiredPresentValidTest(255, "11111111");
    }
    
    @Test
    public void testMissingBitInt() throws Exception
    {
        bitIntTestHarness.doRequiredMissingTest(255, "11111111");
    }
    
    @Test
    public void testOptionalPresentBitInt() throws Exception
    {
        bitIntTestHarness.doOptionalPresentValidTest(255, "11111111");
    }
    
    @Test
    public void testOptionalMissingBitInt() throws Exception
    {
        bitIntTestHarness.doOptionalMissingTest(255, "11111111");
    }
    
    @Test
    public void testBadBitInt() throws Exception
    {
        bitIntTestHarness.doRequiredPresentInvalidTest(255, "asdf");
    }
    
    @Test
    public void testOptionalBadBitInt() throws Exception
    {
        bitIntTestHarness.doOptionalPresentInvalidTest(255, "asdf");
    }
    
    @Test
    public void testDouble() throws Exception
    {
        doubleTestHarness.doRequiredPresentValidTest(0.03);
    }
    
    @Test
    public void testMissingDouble() throws Exception
    {
        doubleTestHarness.doRequiredMissingTest(0.03);
    }
    
    @Test
    public void testOptionalPresentDouble() throws Exception
    {
        doubleTestHarness.doOptionalPresentValidTest(0.03);
    }
    
    @Test
    public void testOptionalMissingDouble() throws Exception
    {
        doubleTestHarness.doOptionalMissingTest(0.03);
    }
    
    @Test
    public void testBadDouble() throws Exception
    {
        doubleTestHarness.doRequiredPresentInvalidTest(0.03, "asdf");
    }
    
    @Test
    public void testBadOptionalDouble() throws Exception
    {
        doubleTestHarness.doOptionalPresentInvalidTest(0.03, "asdf");
    }
    
    @Test
    public void testCoordinate() throws Exception
    {
        coordinateTestHarness.doRequiredPresentValidTest(3.14, "31400000");
    }
    
    @Test
    public void testMissingCoordinate() throws Exception
    {
        coordinateTestHarness.doRequiredMissingTest(3.14, "31400000");
    }
    
    @Test
    public void testOptionalPresentCoordinate() throws Exception
    {
        coordinateTestHarness.doOptionalPresentValidTest(3.14, "31400000");
    }
    
    @Test
    public void testOptionalMissingCoordinate() throws Exception
    {
        coordinateTestHarness.doOptionalMissingTest(3.14, "31400000");
    }
    
    @Test
    public void testBadCoordinate() throws Exception
    {
        coordinateTestHarness.doRequiredPresentInvalidTest(3.14, "asdf");
    }
    
    @Test
    public void testBadOptionalCoordinate() throws Exception
    {
        coordinateTestHarness.doOptionalPresentInvalidTest(3.14, "asdf");
    }
    
    @Test
    public void testDate() throws Exception
    {
        dateTestHarness.doRequiredPresentValidTest(testDate, testDateXml);
    }
    
    @Test
    public void testMissingDate() throws Exception
    {
        dateTestHarness.doRequiredMissingTest(testDate, testDateXml);
    }
    
    @Test
    public void testOptionalPresentDate() throws Exception
    {
        dateTestHarness.doOptionalPresentValidTest(testDate, testDateXml);
    }
    
    @Test
    public void testOptionalMissingDate() throws Exception
    {
        dateTestHarness.doOptionalMissingTest(testDate, testDateXml);
    }
    
    @Test
    public void testBadDate() throws Exception
    {
        dateTestHarness.doRequiredPresentInvalidTest(testDate, "asdf");
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlSansYear);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlSansMonth);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlSansDay);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlSansHour);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlSansMinute);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlBadYear);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlBadMonth);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlBadDay);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlBadHour);
        dateTestHarness.doRequiredPresentInvalidTest(testDate, testDateXmlBadMinute);
    }
    
    @Test
    public void testBadOptionalDate() throws Exception
    {
        dateTestHarness.doOptionalPresentInvalidTest(testDate, "asdf");
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlSansYear);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlSansMonth);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlSansDay);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlSansHour);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlSansMinute);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlBadYear);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlBadMonth);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlBadDay);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlBadHour);
        dateTestHarness.doOptionalPresentInvalidTest(testDate, testDateXmlBadMinute);
    }
    
    
    
    private static class ParserTestHarness<T>
    {
        ParserTestHarness(String field, String wrongField, XerJsonParser parser, BiFunction<JSONObject, String, T> get)
        {
            this.field = field;
            this.wrongField = wrongField;
            this.parser = parser;
            this.get = get;
        }
        
        void doRequiredPresentValidTest(T value, String valueString) throws Exception
        {
            final String path = "/" + field;
            final Document xer = new DocumentXerData("<" + field + ">" + valueString + "</" + field + ">").getFormattedXerData();
            
            final JSONObject json = new JSONObject();
            parser.parseXer(json, field, xer, path);
            
            assertEquals(value, get.apply(json, field));
        }
        
        void doRequiredPresentValidTest(T value) throws Exception
        {
            doRequiredPresentValidTest(value, value.toString());
        }
        
        void doRequiredMissingTest(T value, String valueString) throws Exception
        {
            try {
                final String path = "/" + field;
                final Document xer = new DocumentXerData("<" + wrongField + ">" + valueString + "</" + wrongField + ">").getFormattedXerData();
                
                final JSONObject json = new JSONObject();
                parser.parseXer(json, field, xer, path);
            } catch (XerJsonParserPathMissingException t) {
                return;
            }
            fail("Did not throw XerJsonParserPathMissingException as expected");
        }
        
        void doRequiredMissingTest(T value) throws Exception
        {
            doRequiredMissingTest(value, value.toString());
        }
        
        void doRequiredPresentInvalidTest(T value, String valueString) throws Exception
        {
            final String path = "/" + field;
            final Document xer = new DocumentXerData("<" + field + ">" + valueString + "</" + field + ">").getFormattedXerData();
            
            final JSONObject json = new JSONObject();
            
            try {
                parser.parseXer(json, field, xer, path);
            } catch(XerJsonParserBadTypeException t) {
                return;
            }
            fail("Did not throw XerJsonParserBadTypeException as expected");
        }
        
        void doOptionalPresentValidTest(T value, String valueString) throws Exception
        {
            final String path = "/" + field;
            final Document xer = new DocumentXerData("<" + field + ">" + valueString + "</" + field + ">").getFormattedXerData();
            
            final JSONObject json = new JSONObject();
            XerJsonParsers.optional(parser).parseXer(json, field, xer, path);
            
            assertEquals(value, get.apply(json, field));
        }
        
        void doOptionalPresentValidTest(T value) throws Exception
        {
            doOptionalPresentValidTest(value, value.toString());
        }
        
        void doOptionalPresentInvalidTest(T value, String valueString) throws Exception
        {
            final String path = "/" + field;
            final Document xer = new DocumentXerData("<" + field + ">" + valueString + "</" + field + ">").getFormattedXerData();
            
            final JSONObject json = new JSONObject();
            
            try {
                XerJsonParsers.optional(parser).parseXer(json, field, xer, path);
            } catch(XerJsonParserBadTypeException t) {
                return;
            }
            fail("Did not throw XerJsonParserBadTypeException as expected");
        }
        
        void doOptionalMissingTest(T value, String valueString) throws Exception
        {
            final String path = "/" + field;
            final Document xer = new DocumentXerData("<" + wrongField + ">" + valueString + "</" + wrongField + ">").getFormattedXerData();
            
            final JSONObject json = new JSONObject();
            XerJsonParsers.optional(parser).parseXer(json, field, xer, path);
            
            assertEquals(json, new JSONObject());
        }
        
        void doOptionalMissingTest(T value) throws Exception
        {
            doOptionalMissingTest(value, value.toString());
        }
        
        private final String field;
        private final String wrongField;
        private final XerJsonParser parser;
        private final BiFunction<JSONObject, String, T> get;
    }
}
