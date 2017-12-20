package gov.usdot.cv.mongodb.datasink.model;

public enum DialogId
{
    vehSitData                  (154),
    dataSubscription            (155),
    advSitDataDep               (156),
    advSitDatDist               (157),
    reserved1                   (158), 
    reserved2                   (159), 
    objReg                      (160),
    objDisc                     (161),
    intersectionSitDataDep      (162),
    intersectionSitDataQuery    (163);
    
    /** Get the integer representation of this enum */
    public int getCode()
    {
        return code;
    }
    
    /** Create a new instance from the underlying integer */
    private DialogId(int code)
    {
        this.code = code;
    }
    
    /** Underlying integer */
    private int code;
}
