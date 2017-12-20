package gov.usdot.cv.kluge;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.json.JSONObject;

/** Static methods for converting XER into JSON
 * 
 * XML is converted by considering three kinds of nodes. The first are nodes which have only text
 * as their content, these are assumed to be key-value-pairs with a string value. The second are
 * Nodes which have a single child node, which itself has no text or child nodes, these are assumed
 * to be enums with the tag of the child node being the enum string. Any other kind of nodes are
 * assumed to be sub-objects, with the child tags being keys and their values determined
 * recursively by these same 3 rules.
 * 
 * @author amm30955
 *
 */
public class XerToJson
{
    /**  Naive approach to convert an XML document into JSON
     * 
     * @param doc XML document to convert
     * @return JSON encoding of the document
     */
    public static JSONObject xmlToJson(Document doc)
    {
        Element docElement = doc.getDocumentElement();
        JSONObject root = new JSONObject();
        xmlToJson(docElement, root);
        return root;
    }
    
    /**  Naive approach to convert an XML element into JSON
     * 
     * @param element XML element to convert
     * @param parent JSON object to insert the key-value-pair representing this element into
     * @return JSON encoding of the document
     */
    public static void xmlToJson(Element element, JSONObject parent)
    {
        if (element.hasChildNodes()) {
            String elementTag = element.getTagName();
            
            int childCount = element.getChildNodes().getLength();
            
            boolean elementHasChild = childCount == 1;
            
            Node firstChild = element.getFirstChild();
            
            boolean isEnumNode
                = elementHasChild
               && firstChild.getNodeType() == Node.ELEMENT_NODE
               && !firstChild.hasChildNodes(); 
            
            boolean isStringNode
                = elementHasChild
               && firstChild.getNodeType() == Node.TEXT_NODE;
            if (isEnumNode) {
                Element firstElement = (Element)firstChild;
                
                parent.put(elementTag, firstElement.getTagName());
            }
            else if (isStringNode) {
                parent.put(elementTag, firstChild.getTextContent());
            } else {
                JSONObject subObject = new JSONObject();
                for(int i = 0; i < childCount; ++i) {
                    Node node = element.getChildNodes().item(i);
                    
                    switch (node.getNodeType()) {
                    case Node.ELEMENT_NODE:
                        xmlToJson((Element)node, subObject);
                        break;
                    }
                }
                
                parent.put(elementTag, subObject);
            }
        }
    }
}
