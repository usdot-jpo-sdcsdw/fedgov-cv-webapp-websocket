package gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.model;

/** Enum representing the types for an advisory broadcast 
 * 
 * @author amm30955
 *
 */
public enum AdvisoryBroadcastType
{
    spatAggregate   (0),
    map             (1),
    tim             (2),
    ev              (3);
    
    /** Get the integer representation of this enum */
    public int getCode()
    {
        return code;
    }
    
    /** Create a new instance from the underlying integer */
    private AdvisoryBroadcastType(int code)
    {
        this.code = code;
    }
    
    /** Underlying integer */
    private int code;
}
