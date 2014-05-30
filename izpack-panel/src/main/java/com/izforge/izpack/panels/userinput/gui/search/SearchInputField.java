/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *  
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *      
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels.userinput.gui.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.api.resource.Messages;
import com.izforge.izpack.gui.GUIPrompt;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.panels.userinput.field.search.ResultType;
import com.izforge.izpack.panels.userinput.field.search.SearchField;
import com.izforge.izpack.panels.userinput.field.search.SearchType;

/**
 * This class encapsulates a lot of search field functionality.
 * <p/>
 * A search field supports searching directories and files on the target system. This is a
 * helper class to manage all installDataGUI belonging to a search field.
 */
public class SearchInputField implements ActionListener
{

    private final String filename;

    private final String checkFilename;

    private final JButton autodetectButton;

    private final JButton browseButton;

    private final JComboBox pathComboBox;

    private final SearchType searchType;

    private final ResultType resultType;

    private final InstallerFrame parent;

    private final InstallData installData;

    /*---------------------------------------------------------------------------*/

    /**
     * Constructor - initializes the object, adds it as action listener to the "autodetect"
     * button.
     *
     * @param field        the search field
     * @param combobox     the <code>JComboBox</code> holding the list of choices; it should be
     *                     editable and contain only Strings
     * @param autobutton   the autodetection button for triggering autodetection
     * @param browsebutton the browse button to look for the file
     */
    public SearchInputField(SearchField field, final InstallerFrame parent, JComboBox combobox, JButton autobutton,
                            JButton browsebutton, InstallData installData)
    {
        this.filename = field.getFilename();
        this.checkFilename = field.getCheckFilename();
        this.parent = parent;
        this.autodetectButton = autobutton;
        this.browseButton = browsebutton;
        this.pathComboBox = combobox;
        this.searchType = field.getType();
        this.resultType = field.getResultType();
        this.installData = installData;

        this.autodetectButton.addActionListener(this);
        this.browseButton.addActionListener(this);

        /*
         * add DocumentListener to manage nextButton if user enters input
         */
        final JTextField editor = (JTextField) pathComboBox.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(new DocumentListener()
        {

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                checkNextButtonState();
            }

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                checkNextButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                checkNextButtonState();
            }

            private void checkNextButtonState()
            {
                Document doc = editor.getDocument();
                try
                {
                    if (pathMatches(doc.getText(0, doc.getLength())))
                    {
                        parent.unlockNextButton(false);
                    }
                    else
                    {
                        parent.lockNextButton();
                    }
                }
                catch (BadLocationException e)
                {/* ignore, it not happens */}
            }
        });

        autodetect();
    }
    
    /**
     * Resolve Windows environment variables
     * @param path - eg: "%JAVA_HOME%\\bin"
     * @param env - System.getenv() (or forced config for testing)
     */
    static String resolveEnvValue( String path, Map<String, String> env ) {
    	StringBuilder str = new StringBuilder();
    	int start = 0, envStart;
    	
    	while( (envStart = path.indexOf('%', start)) >= 0 ) {
    		int end = path.indexOf('%', envStart + 1);
    		if( end < 0 ) {
    			break;
    		}
    		String envKey = path.substring(envStart + 1, end);
    		String envValue = env.get(envKey); //System.getenv( envKey );

    		if( envStart > start ) {
    			str.append( path.substring(start, envStart) );
    		}
    		str.append( envValue );
    		start = end + 1;
    	}
    	if( start > 0 ) {
    		str.append( path.substring(start) );
    		return str.toString();
    	}
    	
    	return path;
    }

    /**
     * check whether the given path matches
     */
    private boolean pathMatches(String path)
    {
        if (path != null)
        {
            path = resolveEnvValue(path, System.getenv());

            File file;
            if (filename == null || searchType == SearchType.DIRECTORY)
            {
                file = new File(path);
            }
            else
            {
                file = new File(path, filename);
            }

            if (file.exists())
            {
                if (checkFilename == null)
                {        
                    return true;        // no file to check for
                }

                if( file.isDirectory() ) 
                {
                    file = new File(file, checkFilename);
                    if( !file.exists() ) 
                    {
                        System.out.println( file.getAbsolutePath() + " does not exist");
                        return false;
                    }
                } 
                else 
                {
                    // Check that the file's path and name ends with "checkFilename"
                    if( !file.getAbsolutePath().endsWith( checkFilename.replaceAll("\\\\/", File.separator) ) ) 
                    {
                        return false;
                    }
                }

                // "file" now points to "checkfilename", but is it the correct type?
                return file.isDirectory() == (searchType == SearchType.DIRECTORY);
            }
        }
        return false;
    }

    /**
     * perform autodetection
     */
    public boolean autodetect()
    {
        List<String> items = new ArrayList<String>();
        
        // Try all of <choice> options - see if any are valid
        for (int x = 0; x < pathComboBox.getItemCount(); x++)
        {
        	if( pathMatches( (String)pathComboBox.getItemAt(x) ) ) {
        		pathComboBox.setSelectedIndex(x);
        		break;
        	}
        }

        /*
         * Check if the user has entered installDataGUI into the ComboBox and add it to the Itemlist
         */
        String selected = (String) pathComboBox.getSelectedItem();
        if (selected == null)
        {
            parent.lockNextButton();
            return false;
        }
        boolean found = false;
        for (int x = 0; x < pathComboBox.getItemCount(); x++)
        {
            if (pathComboBox.getItemAt(x).equals(selected))
            {
                found = true;
            }
        }
        if (!found)
        {
            // System.out.println("Not found in Itemlist");
            pathComboBox.addItem(pathComboBox.getSelectedItem());
        }

        // Checks whether a placeholder item is in the combobox
        // and resolve the paths automatically:
        // /usr/lib/* searches all folders in usr/lib to find
        // /usr/lib/*/lib/tools.jar
        for (int i = 0; i < pathComboBox.getItemCount(); ++i)
        {
            String path = (String) pathComboBox.getItemAt(i);
            path = installData.getVariables().replace(path);
            if (path.endsWith("*"))
            {
                path = path.substring(0, path.length() - 1);
                File dir = new File(path);

                if (dir.isDirectory())
                {
                    File[] subdirs = dir.listFiles();
                    if (subdirs != null)
                    {
                        for (File subdir : subdirs)
                        {
                            String search = subdir.getAbsolutePath();
                            if (pathMatches(search))
                            {
                                items.add(search);
                            }
                        }
                    }
                }
            }
            else
            {
                if (pathMatches(path))
                {
                    items.add(path);
                }
            }
        }
        // Make the entries in the vector unique
        items = new ArrayList<String>(new LinkedHashSet<String>(items));

        // Now clear the combobox and add the items out of the newly
        // generated vector
        pathComboBox.removeAllItems();
        for (String item : items)
        {
            item = installData.getVariables().replace(item);
            pathComboBox.addItem(item);
        }

        // loop through all items
        for (int i = 0; i < pathComboBox.getItemCount(); ++i)
        {
            String path = (String) pathComboBox.getItemAt(i);

            if (pathMatches(path))
            {
                pathComboBox.setSelectedIndex(i);
                parent.unlockNextButton();
                return true;
            }

        }

        // if the user entered something else, it's not listed as an item
        if (pathMatches((String) pathComboBox.getSelectedItem()))
        {
            parent.unlockNextButton();
            return true;
        }
        parent.lockNextButton();
        return false;
    }

    /*--------------------------------------------------------------------------*/

    /**
     * This is called if one of the buttons has been pressed.
     * <p/>
     * It checks, which button caused the action and acts accordingly.
     */
    /*--------------------------------------------------------------------------*/
    @Override
    public void actionPerformed(ActionEvent event)
    {
        if (event.getSource() == autodetectButton)
        {
            if (!autodetect())
            {
                warning("UserInputPanel.search.autodetect.failed.caption",
                        "UserInputPanel.search.autodetect.failed.message");
            }
        }
        else if (event.getSource() == browseButton)
        {
            JFileChooser chooser = new JFileChooser();

            if (resultType != ResultType.FILE)
            {
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }

            String startingPath = (String)pathComboBox.getSelectedItem();
            if( startingPath == null && pathComboBox.getItemCount() != 0 ) {
            	startingPath = (String)pathComboBox.getItemAt(0);
            }
            if( startingPath != null ) {
            	File dir = new File(startingPath);
            	if( dir.exists() ) {
            		chooser.setCurrentDirectory(dir);
            	}
            }
            
            int result = chooser.showOpenDialog(parent);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                File selectedFile = chooser.getSelectedFile();

                pathComboBox.setSelectedItem(selectedFile.getAbsolutePath());

                // use any given directory directly
                if (resultType != ResultType.FILE && !pathMatches(selectedFile.getAbsolutePath()))
                {
                    warning("UserInputPanel.search.wrongselection.caption",
                            "UserInputPanel.search.wrongselection.message");
                }
            }

        }

        // we don't care for anything more here - getResult() does the rest
    }

    /*--------------------------------------------------------------------------*/

    /**
     * Return the result of the search according to result type.
     * <p/>
     * Sometimes, the whole path of the file is wanted, sometimes only the directory where the
     * file is in, sometimes the parent directory.
     *
     * @return null on error
     */
    /*--------------------------------------------------------------------------*/
    public String getResult()
    {
        String item = (String) pathComboBox.getSelectedItem();
        if (item != null)
        {
            item = item.trim();
        }
        String path = item;

        File file = new File(item);

        if (!file.isDirectory())
        {
            path = file.getParent();
        }

        // path now contains the final content of the combo box
        if (resultType == ResultType.DIRECTORY)
        {
            return path;
        }
        else if (resultType == ResultType.FILE)
        {
            if (filename != null)
            {
                return path + File.separatorChar + filename;
            }
            else
            {
                return item;
            }
        }
        else if (resultType == ResultType.PARENTDIR)
        {
            File dir = new File(path);
            return dir.getParent();
        }

        return null;
    }

    private void warning(String title, String message)
    {
        Messages messages = parent.getMessages();
        title = messages.get(title);
        message = messages.get(message);
        new GUIPrompt().message(Prompt.Type.WARNING, title, message);
    }

}
