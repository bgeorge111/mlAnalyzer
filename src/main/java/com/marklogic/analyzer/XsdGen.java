package com.marklogic.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
  
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.inst2xsd.Inst2Xsd;
import org.apache.xmlbeans.impl.inst2xsd.Inst2XsdOptions;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;

  
public class XsdGen {
    public String generateSchemaText(InputStream doc) {
    	String xsdText = null;
    	try {
            XsdGen xmlBeans = new XsdGen();
            SchemaDocument schemaDocument = xmlBeans.generateSchema(doc);
  
            StringWriter writer = new StringWriter();
            schemaDocument.save(writer, new XmlOptions().setSavePrettyPrint());
            writer.close();
            xsdText = writer.toString();
  
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xsdText;
    }
  
    public SchemaDocument generateSchema(InputStream is) throws XmlException, IOException {
        // Only 1 instance is required for now
        XmlObject[] xmlInstances = new XmlObject[1];
        xmlInstances[0] = XmlObject.Factory.parse(is);
        return inst2xsd(xmlInstances);
    }
  
  
    private SchemaDocument inst2xsd(XmlObject[] xmlInstances) throws IOException {
        Inst2XsdOptions inst2XsdOptions = new Inst2XsdOptions();
        
        inst2XsdOptions.setDesign(Inst2XsdOptions.DESIGN_VENETIAN_BLIND);
        
  
        SchemaDocument[] schemaDocuments = Inst2Xsd.inst2xsd(xmlInstances, inst2XsdOptions);
        if (schemaDocuments != null && schemaDocuments.length > 0) {
            return schemaDocuments[0];
        }
  
        return null;
    }
}