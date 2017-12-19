package gov.usdot.cv.mongodb.datasink.model;

public enum AdvisoryBroadcastType
{
    spatAggregate   (0),
    map             (1),
    tim             (2),
    ev              (3);
    
    public int getCode()
    {
        return code;
    }
    
    private AdvisoryBroadcastType(int code)
    {
        this.code = code;
    }
    
    private int code;
}
