package com.marklogic.analyzer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

//from  w ww .j a v a  2 s.  c  o  m
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLParser {
  public List<String> pathList(InputStream stream) throws Exception {
   //File file = new File("src/main/resources/file.xml");
   XPath xPath =  XPathFactory.newInstance().newXPath();
   String expression = "//*[not(*)]";
   List<String> pathArray = new ArrayList<String>();
   DocumentBuilderFactory builderFactory = 
   DocumentBuilderFactory.newInstance();
   DocumentBuilder builder = builderFactory.newDocumentBuilder();
   Document document = builder.parse(stream);
   document.getDocumentElement().normalize();

   NodeList nodeList = (NodeList) 
   xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
   for(int i = 0 ; i < nodeList.getLength(); i++) {
      pathArray.add(getNodePath(nodeList.item(i)));
   }
   return pathArray;
}

/**
 * Builds the Path to the Node in the XML Structure.
 *
 * @param node Child {@link Node}
 * @return {@link String} representation of Path to XML Node.
 */
public static  String getNodePath(Node node) {
    if(node == null) {
        throw new IllegalArgumentException("Node cannot be null");
    }
    StringBuilder pathBuilder = new StringBuilder("/");
    pathBuilder.append(node.getNodeName());

    Node currentNode = node;

    if(currentNode.getNodeType() != Node.DOCUMENT_NODE) {
        while (currentNode.getParentNode() != null) {
            currentNode = currentNode.getParentNode();

            if(currentNode.getNodeType() == Node.DOCUMENT_NODE) {
                break;
            } 
//                else if(getIndexOfArrayNode(currentNode) != null) {
//                pathBuilder.insert(0, "/" + currentNode.getNodeName() + "(*)");
//            } 
                else {
                pathBuilder.insert(0, "/" + currentNode.getNodeName());
            }
        }
    }

    return pathBuilder.toString();
}

/**
 * TODO - doesn't handle Formatted XML - treats formatting as Text Nodes and needs to skip these.
 *
 * Light node test to see if Node is part of an Array of Elements.
 *
 * @param node {@link Node}
 * @return True if part of an array. Otherwise false.
 */
private static boolean isArrayNode(Node node) {
    if (node.getNextSibling() == null && node.getPreviousSibling() == null) {
        // Node has no siblings
        return false;
    } else {
        // Check if node siblings are of the same name. If so, then we are inside an array.
        return (node.getNextSibling() != null && node.getNextSibling().getNodeName().equalsIgnoreCase(node.getNodeName()))
                || (node.getPreviousSibling() != null && node.getPreviousSibling().getNodeName().equalsIgnoreCase(node.getNodeName()));
    }
}

/**
 *  TODO - doesn't handle Formatted XML - treats formatting as Text Nodes and needs to skip these.
 *  Figures out the Index of the Array Node.
 *
 *  @param node {@link Node}
 *  @return Index of element in array. Returns null if not inside an array.
 */
private static Integer getIndexOfArrayNode(Node node) {
    if(isArrayNode(node)) {
        int leftCount = 0;

        Node currentNode = node.getPreviousSibling();

        while(currentNode != null) {
            leftCount++;
            currentNode = currentNode.getPreviousSibling();
        }
        return leftCount;
    } else {
        return null;
    }
}
}