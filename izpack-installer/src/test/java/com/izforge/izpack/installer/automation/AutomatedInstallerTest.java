package com.izforge.izpack.installer.automation;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.ResourceNotFoundException;
import com.izforge.izpack.api.resource.Locales;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.core.data.DefaultVariables;
import com.izforge.izpack.core.resource.DefaultLocales;
import com.izforge.izpack.core.resource.DefaultResources;
import com.izforge.izpack.util.Platform;
import com.izforge.izpack.util.Platforms;

public class AutomatedInstallerTest {

	@Test
	public void testMergeSystemProperties() throws Exception {
		Properties props = new Properties();
		props.setProperty("foo", "glorious foo");
		Variables variables = new DefaultVariables(props);
		Platform platform = new Platforms().getCurrentPlatform();
		
		Resources resources = new DefaultResources();
		Locales locales = new DefaultLocales(resources);

		AutomatedInstallData installData = new AutomatedInstallData(variables, platform);
		AutomatedInstaller installer = new AutomatedInstaller(null, installData, locales, null, null, null);
		try { 
			installer.init("src/test/resources/auto-install.xml", null);
		} catch( ResourceNotFoundException e ) {
			// probably need to mock out some of the parameters...
		}
	}
}
