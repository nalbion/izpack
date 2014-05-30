package com.izforge.izpack.panels.finish;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.core.data.DefaultVariables;
import com.izforge.izpack.util.Platform;
import com.izforge.izpack.util.Platforms;

public class FinishPanelTest {
	
	@Test
	public void testGetPropertiesFileFromXmlFile() {
		File xmlFile = new File( "/path.xml/file.xml" );
		Assert.assertEquals( "/path.xml/file.properties", 
							FinishPanelHelper.getPropertiesFileFromXmlFile(xmlFile)
											.getAbsolutePath()
											.replaceFirst("^.:\\\\", "/").replaceAll("\\\\", "/") );
	}
	
	@Test
	public void testSavePopulatedOptionsFile() throws Exception {
		DefaultVariables variables = new DefaultVariables();
		Platform platform = new Platforms().getCurrentPlatform();
		AutomatedInstallData installData = new AutomatedInstallData(variables, platform);
		
		IXMLElement xmlData = installData.getXmlData();
		
//		installData.setVariable("bla", "spam");
//		List<Panel> panels = installData.getPanelsOrder();
//		for( Panel panel : panels ) {
//			panel.addConfiguration("foo", "bar");
//		}
		IXMLElement userPanel = new XMLElementImpl( "com.izforge.izpack.panels.userinput.UserInputPanel", xmlData );
		xmlData.addChild(userPanel);
		IXMLElement userInput = new XMLElementImpl( "userInput", userPanel );
		userPanel.addChild(userInput);
		
		IXMLElement entry = new XMLElementImpl("entry", userInput);
		userPanel.addChild(entry);
		entry.setAttribute("key", "SCATSITSPortIPAddress");
		entry.setAttribute("value", "");
		
		entry = new XMLElementImpl("entry", userInput);
		userPanel.addChild(entry);
		entry.setAttribute("key", "ptips.messaging.service.naming.host");
		entry.setAttribute("value", "163.189.13.191");
		
		File path = new File("target/temp");
		path.mkdirs();
		File propertiesFile = new File(path, "auto-install.properties");
		
		FinishPanelHelper.savePopulatedOptionsFile(xmlData, propertiesFile);
	}
}
