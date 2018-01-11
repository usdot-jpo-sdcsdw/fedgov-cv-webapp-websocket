package gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser;

/** A pair of a JSON key and a XER path for an XER field
 * 
 * @author amm30955
 *
 */
public class FieldPathPair
{
    /** Create a pair
     * 
     * @param field JSON key of the field
     * @param path XER XPath of the field
     */
    public FieldPathPair(String field, String path)
    {
        this.field = field;
        this.path = path;
    }
    
    /** Get the JSON key of the field 
     * 
     * @return JSON key of the field
     */
    public String getField()
    {
        return field;
    }
    
    /** Get the XPath of the field
     * 
     * @return XER XPath of the field
     */
    public String getPath()
    {
        return path;
    }
    
    /** JSON key of the field */
    private final String field;
    
    /** XER XPath of the field */
    private final String path;
}