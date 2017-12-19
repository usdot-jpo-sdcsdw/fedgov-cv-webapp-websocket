package gov.usdot.cv.mongodb.datasink.model;

public enum SequenceId
{
    svcReq              (1),
    svcResp             (2),
    dataReq             (3),
    dataConf            (4),
    data                (5), 
    accept              (6),
    receipt             (7),
    subscriptionReq     (8),
    subscriptinoResp    (9),
    subscriptionCancel  (10);
    
    public int getCode()
    {
        return code;
    }
    
    private SequenceId(int code)
    {
        this.code = code;
    }
    
    private int code;
}
