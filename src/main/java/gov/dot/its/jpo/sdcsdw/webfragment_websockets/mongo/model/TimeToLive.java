package gov.dot.its.jpo.sdcsdw.webfragment_websockets.mongo.model;

import java.util.Calendar;
import java.util.Date;

public enum TimeToLive {
    Minute(0), HalfHour(1), Day(2), Week(3), Month(4), Year(5);
    
    private static final int DEFAULT_FIELD_NUMBER = 0;
    private static final int DEFAULT_EXPIRATION_VALUE = 30;
    
    private int code;
    private int fieldNumber;
    
    private TimeToLive(int code) {
        this.code = code;
        if (code == 0) {
            this.fieldNumber = Calendar.MINUTE;
        } else if (code == 1) {
            this.fieldNumber = Calendar.MINUTE;
        } else if (code == 2) {
            this.fieldNumber = Calendar.DATE;
        } else if (code == 3) {
            this.fieldNumber = Calendar.DATE;
        } else if (code == 4) {
            this.fieldNumber = Calendar.MONTH;
        } else {
            this.fieldNumber = Calendar.YEAR;
        }
    }
    
    public int getCode()
    {
        return code;
    }
    
    public Date getExpiration() {
        Calendar cal = Calendar.getInstance();
        if (this.code == 0 || 
            this.code == 2 || 
            this.code == 4 || 
            this.code == 5) {
            cal.add(this.fieldNumber, 1);
        } else if (this.code == 1) {
            cal.add(this.fieldNumber, 30);
        } else {
            cal.add(this.fieldNumber, 7);
        }
        return cal.getTime();
    }
    
    public static TimeToLive fromCode(int code) {
        switch (code) {
            case 0  : return Minute;
            case 1  : return HalfHour;
            case 2  : return Day;
            case 3  : return Week;
            case 4  : return Month;
            case 5  : return Year;
            default : return null;
        }
    }
    
    public static TimeToLive fromString(String code) {
        for(TimeToLive ttl : TimeToLive.values()) {
            if (ttl.toString().equalsIgnoreCase(code)) {
                return ttl;
            }
        }
        
        return null;
    }
    
    
    
    public static Date getExpiration(int ttlValue, String ttlUnit) {
        Calendar cal = Calendar.getInstance();
        if (ttlValue < 1 || ttlUnit == null) {
            cal.add(DEFAULT_FIELD_NUMBER, DEFAULT_EXPIRATION_VALUE);
        } else if (ttlUnit.equalsIgnoreCase(Minute.toString())) {
            cal.add(Minute.fieldNumber, ttlValue);
        } else if (ttlUnit.equalsIgnoreCase(Day.toString())) {
            cal.add(Day.fieldNumber, ttlValue);
        } else if (ttlUnit.equalsIgnoreCase(Week.toString())) {
            cal.add(Week.fieldNumber, ttlValue);
        } else if (ttlUnit.equalsIgnoreCase(Month.toString())) {
            cal.add(Month.fieldNumber, ttlValue);
        } else if (ttlUnit.equalsIgnoreCase(Year.toString())) {
            cal.add(Year.fieldNumber, ttlValue);
        } else {
            cal.add(DEFAULT_FIELD_NUMBER, DEFAULT_EXPIRATION_VALUE);
        }
        
        return cal.getTime();
    }
    
}