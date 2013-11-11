package com.izforge.izpack.panels.finish;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.izforge.izpack.api.adaptator.IXMLElement;

public class FinishPanelHelper {

	static File getPropertiesFileFromXmlFile( final File xmlFile ) {
		return new File( xmlFile.getAbsolutePath().replaceAll("\\.xml$", ".properties") );
	}
	
	static void saveAutoInstallXml( final File xmlFile ) {
		throw new IllegalAccessError("saveAutoInstallXml() is not yet implemented.  Separate implementations currently exist for FinishPanel and FinishConsolePanel");
	}
	
	/**
	 * Iterates through all &lt;entry> elements of &lt;UserInputPanel>s in the &lt;AutomatedInstallation> document
	 * 
	 * @param xmlData
	 * @param propertiesFile - where to write to output to
	 * @throws XPathExpressionException
	 * @throws IOException
	 */
	static void savePopulatedOptionsFile( final IXMLElement xmlData, final File file ) throws XPathExpressionException, IOException {
		File propertiesFile = getPropertiesFileFromXmlFile(file);
		
    	FileOutputStream out = new FileOutputStream( propertiesFile );
    	Properties props = new Properties();
    	
    	XPathFactory factory = XPathFactory.newInstance(); 
    	XPath xpath = factory.newXPath();
    	NodeList entries = (NodeList)xpath.evaluate("//entry", xmlData.getElement(), XPathConstants.NODESET);
    	int i = entries.getLength();
    	while( i-- != 0 ) {
    		Element el = (Element)entries.item(i);
    		String key = el.getAttribute("key");
    		String value = el.getAttribute("value");
    		props.put(key, value);
    	}
    	
    	props.store( new FileOutputStream(propertiesFile), "For use with '-options-template' - see http://docs.codehaus.org/display/IZPACK/Unattended+Installations");
	}
	
}
