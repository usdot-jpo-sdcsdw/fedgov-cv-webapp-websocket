package gov.usdot.cv.kluge;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.json.JSONObject;

public class AdvisorySituationDataXmlToJson
{
    public static JSONObject xmlToJson(Document doc)
    {
        Element docElement = doc.getDocumentElement();
        JSONObject root = new JSONObject();
        xmlToJson(docElement, root);
        return root;
    }
    
    public static void xmlToJson(Element element, JSONObject parent)
    {
        if (element.hasChildNodes()) {
            if (element.getChildNodes().getLength() == 1 && element.getFirstChild().getNodeType() == Node.ELEMENT_NODE && !element.getFirstChild().hasChildNodes()) {
                parent.put(element.getTagName(), ((Element)element.getFirstChild()).getTagName());
            }
            else if (element.getChildNodes().getLength() == 1 && element.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
                parent.put(element.getTagName(), element.getChildNodes().item(0).getTextContent());
            } else {
                JSONObject child = new JSONObject();
                for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
                    Node node = element.getChildNodes().item(i);
                    
                    switch (node.getNodeType()) {
                    case Node.ELEMENT_NODE:
                        xmlToJson((Element)node, child);
                        break;
                    }
                }
                
                parent.put(element.getTagName(), child);
            }
        }
    }
}
