/*
 * This file is part of WebLookAndFeel library.
 *
 * WebLookAndFeel library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebLookAndFeel library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebLookAndFeel library.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alee.extended.style;

import com.alee.extended.breadcrumb.WebBreadcrumb;
import com.alee.extended.breadcrumb.WebBreadcrumbLabel;
import com.alee.extended.breadcrumb.WebBreadcrumbPanel;
import com.alee.extended.checkbox.WebTristateCheckBox;
import com.alee.extended.layout.HorizontalFlowLayout;
import com.alee.extended.layout.VerticalFlowLayout;
import com.alee.extended.panel.CenterPanel;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.panel.GroupingType;
import com.alee.extended.panel.WebButtonGroup;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.extended.syntax.SyntaxPreset;
import com.alee.extended.syntax.WebSyntaxArea;
import com.alee.extended.syntax.WebSyntaxScrollPane;
import com.alee.extended.tree.WebFileTree;
import com.alee.extended.window.PopOverLocation;
import com.alee.extended.window.WebPopOver;
import com.alee.global.StyleConstants;
import com.alee.laf.Styles;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.button.WebToggleButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.menu.WebCheckBoxMenuItem;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.radiobutton.WebRadioButton;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollBar;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.tabbedpane.TabbedPaneStyle;
import com.alee.laf.tabbedpane.WebTabbedPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.text.WebTextField;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.laf.tree.TreeSelectionStyle;
import com.alee.managers.glasspane.GlassPaneManager;
import com.alee.managers.glasspane.WebGlassPane;
import com.alee.managers.hotkey.Hotkey;
import com.alee.managers.hotkey.HotkeyManager;
import com.alee.managers.hotkey.HotkeyRunnable;
import com.alee.managers.log.Log;
import com.alee.managers.style.StyleManager;
import com.alee.managers.style.SupportedComponent;
import com.alee.managers.style.data.SkinInfo;
import com.alee.managers.style.data.SkinInfoConverter;
import com.alee.managers.style.skin.CustomSkin;
import com.alee.utils.*;
import com.alee.utils.swing.*;
import com.alee.utils.text.LoremIpsum;
import com.alee.utils.xml.ResourceFile;
import com.alee.utils.xml.ResourceLocation;
import com.thoughtworks.xstream.converters.ConversionException;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * WebLaF style editor application.
 * It allows you to edit and preview WebLaF skins in runtime.
 *
 * @author Mikle Garin
 */

public class StyleEditor extends WebFrame
{
    /**
     * todo 1. Translate editor
     * todo 2. Add JavaDoc
     */

    private static final ImageIcon info = new ImageIcon ( StyleEditor.class.getResource ( "icons/status/info.png" ) );
    private static final ImageIcon ok = new ImageIcon ( StyleEditor.class.getResource ( "icons/status/ok.png" ) );
    private static final ImageIcon error = new ImageIcon ( StyleEditor.class.getResource ( "icons/status/error.png" ) );

    private static final ImageIcon tabIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/tab.png" ) );

    private static final BufferedImage magnifier =
            ImageUtils.getBufferedImage ( new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/magnifierImage.png" ) ) );

    private WebToolBar toolBar;
    private WebPanel container;
    private WebSplitPane split;
    private WebPanel componentViewer;
    private WebPanel editorsContainer;

    private WebStatusBar statusbar;
    private WebBreadcrumbLabel statusMessage;

    private final List<JComponent> previewComponents = new ArrayList<JComponent> ();
    private final List<WebPanel> boundsPanels = new ArrayList<WebPanel> ();

    private int updateDelay = 50;
    private int zoomFactor = 4;
    private ComponentOrientation orientation = WebLookAndFeel.getOrientation ();
    private boolean enabled = true;

    private final ResourceFile baseSkinFile;
    private List<WebSyntaxArea> editors;

    public StyleEditor ( final ResourceFile editedSkinFile )
    {
        super ( "WebLaF skin editor" );
        setIconImages ( WebLookAndFeel.getImages () );

        // todo Make changeable through constructor
        baseSkinFile = editedSkinFile;

        initializeContainer ();
        initializeToolBar ();
        initializeStatusBar ();
        initializeViewer ();
        initializeEditors ();

        setDefaultCloseOperation ( WindowConstants.EXIT_ON_CLOSE );
        setSize ( 1200, 800 );
        setLocationRelativeTo ( null );
    }

    private void initializeToolBar ()
    {
        toolBar = new WebToolBar ();
        toolBar.setStyleId ( "preview-toolbar" );

        final ImageIcon magnifierIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/magnifier.png" ) );
        final WebToggleButton magnifierButton = new WebToggleButton ( magnifierIcon );
        magnifierButton.setStyleId ( "preview-tool-toggle-button" );
        magnifierButton.setToolTip ( magnifierIcon, "Show/hide magnifier tool" );
        magnifierButton.addHotkey ( Hotkey.ALT_Q );
        initializeMagnifier ( magnifierButton );

        final WebButton zoomFactorButton = new WebButton ( "4x" );
        zoomFactorButton.setStyleId ( "preview-tool-button" );
        zoomFactorButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                final WebPopupMenu menu = new WebPopupMenu ();
                for ( int i = 2; i <= 6; i++ )
                {
                    final int factor = i;
                    final JMenuItem menuItem = new WebMenuItem ( i + "x zoom" );
                    menuItem.addActionListener ( new ActionListener ()
                    {
                        @Override
                        public void actionPerformed ( final ActionEvent e )
                        {
                            zoomFactor = factor;
                            zoomFactorButton.setText ( factor + "x" );
                        }
                    } );
                    menu.add ( menuItem );
                }
                menu.showBelowMiddle ( zoomFactorButton );
            }
        } );

        toolBar.add ( new WebButtonGroup ( magnifierButton, zoomFactorButton ) );

        final ImageIcon boundsIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/bounds.png" ) );
        final WebToggleButton boundsButton = new WebToggleButton ( boundsIcon );
        boundsButton.setStyleId ( "preview-tool-toggle-button" );
        boundsButton.setToolTip ( boundsIcon, "Show/hide component bounds" );
        boundsButton.addHotkey ( Hotkey.ALT_W );
        boundsButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                for ( final WebPanel boundsPanel : boundsPanels )
                {
                    boundsPanel.setStyleId ( boundsButton.isSelected () ? "dashed-border" : "empty-border" );
                }
            }
        } );
        toolBar.add ( boundsButton );

        final ImageIcon disabledIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/disabled.png" ) );
        final WebToggleButton disabledButton = new WebToggleButton ( disabledIcon );
        disabledButton.setStyleId ( "preview-tool-toggle-button" );
        disabledButton.setToolTip ( disabledIcon, "Disable/enable components" );
        disabledButton.addHotkey ( Hotkey.ALT_D );
        disabledButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                enabled = !enabled;

                // Applying enabled state to separate components as they might not be visible on panel
                for ( final JComponent component : previewComponents )
                {
                    SwingUtils.setEnabledRecursively ( component, enabled );
                }
            }
        } );
        toolBar.add ( disabledButton );

        final ImageIcon orientationIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/orientation.png" ) );
        final WebToggleButton orientationButton = new WebToggleButton ( orientationIcon );
        orientationButton.setStyleId ( "preview-tool-toggle-button" );
        orientationButton.setToolTip ( orientationIcon, "Change components orientation" );
        orientationButton.addHotkey ( Hotkey.ALT_R );
        orientationButton.setSelected ( !orientation.isLeftToRight () );
        orientationButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                orientation = orientation.isLeftToRight () ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;

                // Applying orientation to whole panel first
                componentViewer.applyComponentOrientation ( orientation );

                // Applying orientation to separate components as they might not be visible on panel
                for ( final JComponent component : previewComponents )
                {
                    component.applyComponentOrientation ( orientation );
                }
            }
        } );
        toolBar.add ( orientationButton );
    }

    private void initializeContainer ()
    {
        container = new WebPanel ();
        getContentPane ().add ( container, BorderLayout.CENTER );

        split = new WebSplitPane ( WebSplitPane.HORIZONTAL_SPLIT, true );
        split.setDividerLocation ( 300 );
        split.setDividerSize ( 8 );
        split.setDrawDividerBorder ( true );
        split.setOneTouchExpandable ( true );
        container.add ( split, BorderLayout.CENTER );
    }

    private void initializeStatusBar ()
    {
        statusbar = new WebStatusBar ();

        //

        final WebBreadcrumb updateBreadcrumb = new WebBreadcrumb ();
        updateBreadcrumb.setEncloseLastElement ( false );

        final ImageIcon updateIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/update.png" ) );
        final WebLabel delayLabel = new WebLabel ( "Skin update delay:", updateIcon );
        final WebTextField delayField = new WebTextField ( new IntTextDocument (), "" + updateDelay, 3 );
        delayField.setShadeWidth ( 0 );
        delayField.setHorizontalAlignment ( WebTextField.CENTER );
        delayField.getDocument ().addDocumentListener ( new IntDocumentChangeListener ()
        {
            @Override
            public void documentChanged ( final Integer newValue, final DocumentEvent e )
            {
                updateDelay = newValue != null ? newValue : updateDelay;
                if ( updateDelay < 0 )
                {
                    updateDelay = 0;
                }
            }
        } );
        final WebLabel msLabel = new WebLabel ( "ms" );
        final WebBreadcrumbPanel panel = new WebBreadcrumbPanel ();
        panel.setLayout ( new HorizontalFlowLayout ( 4, false ) );
        panel.add ( delayLabel, new CenterPanel ( delayField, false, true ), msLabel );
        updateBreadcrumb.add ( panel );

        statusMessage = new WebBreadcrumbLabel ( "Edit XML at the right side and see UI changes at the left side!", info );
        statusMessage.setStyleId ( "status-message-label" );
        updateBreadcrumb.add ( statusMessage );

        statusbar.add ( updateBreadcrumb );

        //

        statusbar.addToEnd ( new WebMemoryBar ().setPreferredWidth ( 200 ) );

        //

        container.add ( statusbar, BorderLayout.SOUTH );
    }

    private void initializeViewer ()
    {
        componentViewer = new WebPanel ( "preview-pane", new VerticalFlowLayout ( VerticalFlowLayout.TOP, 0, 15, true, false ) );
        componentViewer.setMargin ( 10 );

        final WebScrollPane previewScroll = new WebScrollPane ( Styles.scrollpaneUndecorated, componentViewer );
        previewScroll.setScrollBarStyleId ( "preview-scroll" );

        split.setLeftComponent ( new GroupPanel ( GroupingType.fillLast, 0, false, toolBar, previewScroll ) );

        //

        final WebLabel label = new WebLabel ( "Just a label", WebLookAndFeel.getIcon ( 16 ) );
        addViewComponent ( "Label", label, label, true );

        //

        final WebButton button = new WebButton ( "Simple button", WebLookAndFeel.getIcon ( 16 ) );
        addViewComponent ( "Button", button, button, true );

        //

        final WebButton iconButton = new WebButton ( WebLookAndFeel.getIcon ( 24 ) );
        addViewComponent ( "Icon button", iconButton, iconButton, true );

        //

        final WebToggleButton toggleButton = new WebToggleButton ( "Toggle me", WebLookAndFeel.getIcon ( 16 ) );
        addViewComponent ( "Toggle button", toggleButton, toggleButton, true );

        //

        final WebToggleButton iconToggleButton = new WebToggleButton ( WebLookAndFeel.getIcon ( 24 ) );
        addViewComponent ( "Icon toggle button", iconToggleButton, iconToggleButton, true );

        //

        final WebCheckBox checkBox = new WebCheckBox ( "Check me" );
        addViewComponent ( "Checkbox", checkBox, checkBox, true );

        //

        final WebTristateCheckBox tristateCheckBox = new WebTristateCheckBox ( "Check me more" );
        addViewComponent ( "Tristate checkbox", tristateCheckBox, tristateCheckBox, true );

        //

        final WebRadioButton radioButton1 = new WebRadioButton ( "Radio button 1" );
        final WebRadioButton radioButton2 = new WebRadioButton ( "Radio button 2" );
        final WebRadioButton radioButton3 = new WebRadioButton ( "Radio button 3" );
        SwingUtils.groupButtons ( radioButton1, radioButton2, radioButton3 );
        final GroupPanel radioGroup = new GroupPanel ( false, radioButton1, radioButton2, radioButton3 );
        addViewComponent ( "Radio button", radioGroup, radioGroup, true );

        //

        final WebScrollBar hsb = new WebScrollBar ( WebScrollBar.HORIZONTAL, 45, 10, 0, 100 );
        addViewComponent ( "Horizontal scroll bar", hsb, hsb, false );

        //

        final WebScrollBar vsb = new WebScrollBar ( WebScrollBar.VERTICAL, 45, 10, 0, 100 ).setPreferredHeight ( 100 );
        addViewComponent ( "Vertical scroll bar", vsb, vsb, true );

        //

        final WebTextArea textArea = new WebTextArea ();
        textArea.setRows ( 5 );

        final LoremIpsum loremIpsum = new LoremIpsum ();
        textArea.setText ( loremIpsum.getParagraphs ( 5 ) );

        final WebScrollPane sp = new WebScrollPane ( textArea );
        sp.setPreferredWidth ( 0 );
        addViewComponent ( "Scroll pane", sp, sp, false );

        //

        final String[] d = new String[]{ "Mikle Garin", "Joe Phillips", "Lilly Stewart", "Alex Jackson", "Joshua Martin", "Mark Einsberg",
                "Alice Manson", "Nancy Drew", "John Linderman", "Trisha Mathew", "Annae Mendy", "Wendy Anderson", "Alex Kurovski" };
        final WebComboBox cb = new WebComboBox ( d );
        addViewComponent ( "Combo box", cb, cb, true );

        //

        final WebList wl = new WebList ( d );
        final WebScrollPane wlScroll = new WebScrollPane ( wl );
        wlScroll.setPreferredSize ( new Dimension ( 200, 150 ) );
        addViewComponent ( "List", wlScroll, wl, false );

        //

        final WebFileTree homeFileTree = new WebFileTree ( FileUtils.getUserHomePath () );
        homeFileTree.setAutoExpandSelectedNode ( false );
        homeFileTree.setShowsRootHandles ( true );
        homeFileTree.setSelectionStyle ( TreeSelectionStyle.group );
        final WebScrollPane homeFileTreeScroll = new WebScrollPane ( homeFileTree );
        homeFileTreeScroll.setPreferredSize ( new Dimension ( 200, 150 ) );
        addViewComponent ( "Tree", homeFileTreeScroll, homeFileTree, false );

        //

        final WebPopupMenu popupMenu = new WebPopupMenu ();
        popupMenu.add ( new WebCheckBoxMenuItem ( "Check item", WebLookAndFeel.getIcon ( 16 ) ) );
        popupMenu.addSeparator ();
        popupMenu.add ( new WebMenuItem ( "Item 1", WebLookAndFeel.getIcon ( 16 ) ) );
        popupMenu.add ( new WebMenuItem ( "Item 2" ) );
        popupMenu.add ( new WebMenuItem ( "Item 3" ) );
        popupMenu.addSeparator ();
        popupMenu.add ( new WebMenuItem ( "Item 4", WebLookAndFeel.getIcon ( 16 ), Hotkey.ALT_F4 ) );

        final WebButton popupButton = new WebButton ( "Show popup menu", new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                popupMenu.showBelowMiddle ( ( WebButton ) e.getSource () );
            }
        } );

        addViewComponent ( "Popup menu", popupButton, popupMenu, true );
    }

    private void addViewComponent ( final String title, final JComponent displayedView, final JComponent view, final boolean center )
    {
        final SupportedComponent type = SupportedComponent.getComponentTypeByUIClassID ( view.getUIClassID () );

        final WebLabel titleLabel = new WebLabel ( title, type.getIcon (), WebLabel.LEADING );
        titleLabel.setStyleId ( "preview-title" );

        final WebPanel boundsPanel = new WebPanel ( "empty-border", displayedView );
        boundsPanels.add ( boundsPanel );

        final WebPanel viewPanel = new WebPanel ( "inner-shade", center ? new CenterPanel ( boundsPanel ) : boundsPanel );

        final WebPanel container = new WebPanel ( Styles.panelTransparent, new BorderLayout ( 0, 0 ) );
        container.add ( titleLabel, BorderLayout.NORTH );
        container.add ( viewPanel, BorderLayout.CENTER );
        componentViewer.add ( container );

        titleLabel.addMouseListener ( new MouseAdapter ()
        {
            @Override
            public void mousePressed ( final MouseEvent e )
            {
                viewPanel.setVisible ( !viewPanel.isVisible () );
                componentViewer.revalidate ();
                componentViewer.repaint ();
            }
        } );

        previewComponents.add ( view );
    }

    private void initializeEditors ()
    {
        // Creating XML editors tabbed pane
        final WebTabbedPane editorTabs = new WebTabbedPane ( TabbedPaneStyle.attached );
        editorsContainer = new WebPanel ( editorTabs );

        // Loading editor code theme
        final Theme theme = loadXmlEditorTheme ();

        // Parsing all related files
        final List<String> xmlContent = new ArrayList<String> ();
        final List<String> xmlNames = new ArrayList<String> ();
        final List<ResourceFile> xmlFiles = new ArrayList<ResourceFile> ();
        loadSkinSources ( xmlContent, xmlNames, xmlFiles );

        // Creating editor tabs
        editors = new ArrayList<WebSyntaxArea> ( xmlContent.size () );
        for ( int i = 0; i < xmlContent.size (); i++ )
        {
            final WebPanel tabContent = new WebPanel ();
            tabContent.add ( new TabContentSeparator (), BorderLayout.NORTH );
            tabContent.add ( createSingleXmlEditor ( theme, xmlContent.get ( i ), xmlFiles.get ( i ) ), BorderLayout.CENTER );
            editorTabs.addTab ( xmlNames.get ( i ), tabContent );
            editorTabs.setIconAt ( i, tabIcon );
        }

        // Adding XML editors container into split
        split.setRightComponent ( editorsContainer );

        // Quick file search
        HotkeyManager.registerHotkey ( Hotkey.CTRL_N, new HotkeyRunnable ()
        {
            @Override
            public void run ( final KeyEvent e )
            {
                final WebPopOver popOver = new WebPopOver ( StyleEditor.this );
                popOver.setCloseOnFocusLoss ( true );

                // File name search field
                final WebTextField searchField = new WebTextField ( 25 );
                searchField.setInputPrompt ( "Jump to file..." );
                popOver.add ( searchField );

                // Jump to tabs while typing
                searchField.getDocument ().addDocumentListener ( new DocumentChangeListener ()
                {
                    @Override
                    public void documentChanged ( final DocumentEvent e )
                    {
                        final String text = searchField.getText ().toLowerCase ();
                        if ( !TextUtils.isEmpty ( text ) )
                        {
                            for ( final String name : xmlNames )
                            {
                                if ( name.toLowerCase ().contains ( text ) )
                                {
                                    editorTabs.setSelectedIndex ( xmlNames.indexOf ( name ) );
                                    break;
                                }
                            }
                        }
                    }
                } );

                // Close pop-over on ENTER or ESCAPE
                final KeyEventRunnable closeRunnable = new KeyEventRunnable ()
                {
                    @Override
                    public void run ( final KeyEvent e )
                    {
                        popOver.dispose ();
                        editors.get ( editorTabs.getSelectedIndex () ).requestFocus ();
                        editors.get ( editorTabs.getSelectedIndex () ).requestFocusInWindow ();
                    }
                };
                searchField.onKeyPress ( Hotkey.ENTER, closeRunnable );
                searchField.onKeyPress ( Hotkey.ESCAPE, closeRunnable );

                popOver.show ( PopOverLocation.center );
            }
        } );
    }

    private Component createSingleXmlEditor ( final Theme theme, final String xml, final ResourceFile xmlFile )
    {
        final WebSyntaxArea xmlEditor = new WebSyntaxArea ( xml, SyntaxPreset.xml );
        xmlEditor.applyPresets ( SyntaxPreset.base );
        xmlEditor.applyPresets ( SyntaxPreset.margin );
        xmlEditor.applyPresets ( SyntaxPreset.size );
        xmlEditor.applyPresets ( SyntaxPreset.historyLimit );

        xmlEditor.setCaretPosition ( 0 );

        xmlEditor.setHyperlinksEnabled ( true );
        xmlEditor.setLinkGenerator ( new CodeLinkGenerator ( xmlEditor ) );

        HotkeyManager.registerHotkey ( xmlEditor, xmlEditor, Hotkey.CTRL_SHIFT_Z, new HotkeyRunnable ()
        {
            @Override
            public void run ( final KeyEvent e )
            {
                xmlEditor.undoLastAction ();
            }
        } );

        // Creating editor scroll with preferred settings
        final WebSyntaxScrollPane xmlEditorScroll = new WebSyntaxScrollPane ( xmlEditor );

        // Applying editor theme after scroll creation
        theme.apply ( xmlEditor );

        // Start listening edits
        xmlEditor.onChange ( new DocumentEventRunnable ()
        {
            private final WebTimer updateTimer = new WebTimer ( updateDelay, new ActionListener ()
            {
                @Override
                public void actionPerformed ( final ActionEvent e )
                {
                    TimeUtils.pinTime ();
                    SkinInfoConverter.addCustomResource ( xmlFile.getClassName (), xmlFile.getSource (), xmlEditor.getText () );
                    applySkin ();
                    TimeUtils.showPassedTime ( "Time to apply skin: " );
                }
            } ).setRepeats ( false );

            @Override
            public void run ( final DocumentEvent e )
            {
                updateTimer.restart ( updateDelay );
            }
        } );

        editors.add ( xmlEditor );
        return xmlEditorScroll;
    }

    private void loadSkinSources ( final List<String> xmlContent, final List<String> xmlNames, final List<ResourceFile> xmlFiles )
    {
        // Adding base skin file
        final List<ResourceFile> resources = new ArrayList<ResourceFile> ();
        resources.add ( baseSkinFile );

        // Parsing all related skin files
        while ( resources.size () > 0 )
        {
            try
            {
                loadFirstResource ( resources, xmlContent, xmlNames, xmlFiles );
            }
            catch ( final IOException e )
            {
                Log.error ( this, e );
            }
        }
    }

    private void loadFirstResource ( final List<ResourceFile> resources, final List<String> xmlContent, final List<String> xmlNames,
                                     final List<ResourceFile> xmlFiles ) throws IOException
    {
        final ResourceFile rf = resources.get ( 0 );
        final Source xmlSource = new Source ( ReflectUtils.getClassSafely ( rf.getClassName () ).getResource ( rf.getSource () ) );
        xmlSource.setLogger ( null );
        xmlSource.fullSequentialParse ();

        final Element baseClassTag = xmlSource.getFirstElement ( SkinInfoConverter.CLASS_NODE );
        final String baseClass = baseClassTag != null ? baseClassTag.getContent ().toString () : null;

        for ( final Element includeTag : xmlSource.getAllElements ( SkinInfoConverter.INCLUDE_NODE ) )
        {
            final String includeClass = includeTag.getAttributeValue ( SkinInfoConverter.NEAR_CLASS_ATTRIBUTE );
            final String finalClass = includeClass != null ? includeClass : baseClass;
            final String src = includeTag.getContent ().toString ();
            resources.add ( new ResourceFile ( ResourceLocation.nearClass, src, finalClass ) );
        }

        xmlContent.add ( xmlSource.toString () );
        xmlNames.add ( new File ( rf.getSource () ).getName () );
        xmlFiles.add ( rf );

        resources.remove ( 0 );
    }

    private Theme loadXmlEditorTheme ()
    {
        try
        {
            return Theme.load ( StyleEditor.class.getResourceAsStream ( "resources/XmlEditorTheme.xml" ) );
        }
        catch ( final IOException e )
        {
            Log.error ( this, e );
            return null;
        }
    }

    private void applySkin ()
    {
        try
        {
            long time = System.currentTimeMillis ();
            StyleManager.installSkin ( new CustomSkin ( ( SkinInfo ) XmlUtils.fromXML ( editors.get ( 0 ).getText () ) ) );
            componentViewer.revalidate ();

            // Information in status bar
            time = System.currentTimeMillis () - time;
            statusMessage.setIcon ( ok );
            statusMessage.setText ( "Style updated succesfully within " + time + " ms" );
        }
        catch ( final ConversionException ex )
        {
            // Short stack trace for parse exceptions
            Log.error ( this, "Unable to update skin: " + ex.getMessage () );

            // Information in status bar
            statusMessage.setIcon ( error );
            statusMessage.setText ( "Fix syntax problems within the XML to update styling" );
        }
        catch ( final Throwable ex )
        {
            // Full stack trace for unknown exceptions
            Log.error ( this, "Unable to update skin: " + ex.getMessage (), ex );

            // Information in status bar
            statusMessage.setIcon ( error );
            statusMessage.setText ( "Fix syntax problems within the XML to update styling" );
        }
    }

    /**
     * Initializes magnifier display action for the specified button.
     *
     * @param button magnifier display button
     */
    private void initializeMagnifier ( final WebToggleButton button )
    {
        final WebGlassPane glassPane = GlassPaneManager.getGlassPane ( StyleEditor.this );
        final JComponent zoomProvider = SwingUtils.getRootPane ( StyleEditor.this ).getLayeredPane ();
        button.addActionListener ( new ActionListener ()
        {
            private boolean visible = false;
            private AWTEventListener listener;
            private WebTimer forceUpdater;

            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                performAction ();
            }

            protected void performAction ()
            {
                if ( !visible )
                {
                    visible = true;

                    if ( forceUpdater == null || listener == null )
                    {
                        forceUpdater = new WebTimer ( 200, new ActionListener ()
                        {
                            @Override
                            public void actionPerformed ( final ActionEvent e )
                            {
                                updateMagnifier ();
                            }
                        } );
                        listener = new AWTEventListener ()
                        {
                            @Override
                            public void eventDispatched ( final AWTEvent event )
                            {
                                SwingUtilities.invokeLater ( new Runnable ()
                                {
                                    @Override
                                    public void run ()
                                    {
                                        if ( visible )
                                        {
                                            forceUpdater.restart ();
                                            updateMagnifier ();
                                        }
                                    }
                                } );
                            }
                        };
                    }
                    Toolkit.getDefaultToolkit ().addAWTEventListener ( listener, AWTEvent.MOUSE_MOTION_EVENT_MASK );
                    Toolkit.getDefaultToolkit ().addAWTEventListener ( listener, AWTEvent.MOUSE_WHEEL_EVENT_MASK );
                    Toolkit.getDefaultToolkit ().addAWTEventListener ( listener, AWTEvent.MOUSE_EVENT_MASK );
                    updateMagnifier ();

                    setCursor ( SystemUtils.getTransparentCursor () );
                }
                else
                {
                    visible = false;

                    Toolkit.getDefaultToolkit ().removeAWTEventListener ( listener );
                    forceUpdater.stop ();
                    hideMagnifier ();

                    setCursor ( Cursor.getDefaultCursor () );
                }
            }

            protected void updateMagnifier ()
            {
                final Point mp = MouseInfo.getPointerInfo ().getLocation ();
                final Rectangle gb = SwingUtils.getBoundsOnScreen ( glassPane );
                if ( gb.contains ( mp ) )
                {
                    final Point gp = gb.getLocation ();
                    final int mx = mp.x - gp.x - magnifier.getWidth () / 2;
                    final int my = mp.y - gp.y - magnifier.getHeight () / 2;

                    final int w = 162 / zoomFactor;
                    final BufferedImage image = ImageUtils.createCompatibleImage ( w, w, Transparency.TRANSLUCENT );
                    final Graphics2D g2d = image.createGraphics ();
                    g2d.translate ( -( mp.x - gp.x - w / 2 ), -( mp.y - gp.y - w / 2 ) );
                    zoomProvider.paintAll ( g2d );
                    g2d.dispose ();

                    final BufferedImage finalImage = ImageUtils.createCompatibleImage ( 220, 220, Transparency.TRANSLUCENT );
                    final Graphics2D g = finalImage.createGraphics ();
                    g.setClip ( new Ellipse2D.Double ( 29, 29, 162, 162 ) );
                    g.drawImage ( image, 29, 29, 162, 162, null );
                    g.setClip ( null );
                    g.drawImage ( magnifier, 0, 0, null );
                    g.dispose ();

                    glassPane.setPaintedImage ( finalImage, new Point ( mx, my ) );
                }
                else
                {
                    hideMagnifier ();
                }
            }

            protected void hideMagnifier ()
            {
                glassPane.setPaintedImage ( null, null );
            }
        } );
    }

    /**
     * Custom tab content separator.
     */
    private class TabContentSeparator extends JComponent
    {
        @Override
        protected void paintComponent ( final Graphics g )
        {
            g.setColor ( new Color ( 237, 237, 237 ) );
            g.fillRect ( 0, 0, getWidth (), getHeight () - 1 );
            g.setColor ( StyleConstants.darkBorderColor );
            g.drawLine ( 0, getHeight () - 1, getWidth () - 1, getHeight () - 1 );
        }

        @Override
        public Dimension getPreferredSize ()
        {
            return new Dimension ( 0, 4 );
        }
    }

    /**
     * StyleEditor main method used to launch editor.
     *
     * @param args arguments
     */
    public static void main ( final String[] args )
    {
        // Custom StyleEditor skin for WebLaF
        StyleManager.setDefaultSkin ( StyleEditorSkin.class.getCanonicalName () );
        WebLookAndFeel.install ();

        // Displaying StyleEditor
        final ResourceFile skin = new ResourceFile ( ResourceLocation.nearClass, "resources/StyleEditorSkin.xml", StyleEditorSkin.class );
        final StyleEditor styleEditor = new StyleEditor ( skin );
        styleEditor.setVisible ( true );
    }
}