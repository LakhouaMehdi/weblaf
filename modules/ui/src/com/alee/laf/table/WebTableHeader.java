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

package com.alee.laf.table;

import com.alee.laf.WebLookAndFeel;
import com.alee.managers.log.Log;
import com.alee.managers.style.StyleId;
import com.alee.utils.ReflectUtils;
import com.alee.utils.laf.MarginSupport;
import com.alee.utils.laf.PaddingSupport;
import com.alee.utils.laf.ShapeProvider;
import com.alee.utils.laf.Styleable;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;

/**
 * This JTableHeader extension class provides a direct access to WebTableHeaderUI methods.
 *
 * @author Mikle Garin
 */

public class WebTableHeader extends JTableHeader implements Styleable, ShapeProvider, MarginSupport, PaddingSupport
{
    /**
     * Constructs a {@code JTableHeader} with a default {@code TableColumnModel}.
     *
     * @see #createDefaultColumnModel
     */
    public WebTableHeader ()
    {
        super ();
    }

    /**
     * Constructs a {@code JTableHeader} with a default {@code TableColumnModel}.
     *
     * @param id style ID
     * @see #createDefaultColumnModel
     */
    public WebTableHeader ( final StyleId id )
    {
        super ();
        setStyleId ( id );
    }

    /**
     * Constructs a {@code JTableHeader} which is initialized with {@code cm} as the column model.
     * If {@code cm} is {@code null} this method will initialize the table header with a default {@code TableColumnModel}.
     *
     * @param cm the column model for the table
     * @see #createDefaultColumnModel
     */
    public WebTableHeader ( final TableColumnModel cm )
    {
        super ( cm );
    }

    /**
     * Constructs a {@code JTableHeader} which is initialized with {@code cm} as the column model.
     * If {@code cm} is {@code null} this method will initialize the table header with a default {@code TableColumnModel}.
     *
     * @param id style ID
     * @param cm the column model for the table
     * @see #createDefaultColumnModel
     */
    public WebTableHeader ( final StyleId id, final TableColumnModel cm )
    {
        super ( cm );
        setStyleId ( id );
    }


    @Override
    public StyleId getStyleId ()
    {
        return getWebUI ().getStyleId ();
    }

    @Override
    public void setStyleId ( final StyleId id )
    {
        getWebUI ().setStyleId ( id );
    }

    @Override
    public Shape provideShape ()
    {
        return getWebUI ().provideShape ();
    }

    @Override
    public Insets getMargin ()
    {
        return getWebUI ().getMargin ();
    }

    /**
     * Sets new margin.
     *
     * @param margin new margin
     */
    public void setMargin ( final int margin )
    {
        setMargin ( margin, margin, margin, margin );
    }

    /**
     * Sets new margin.
     *
     * @param top    new top margin
     * @param left   new left margin
     * @param bottom new bottom margin
     * @param right  new right margin
     */
    public void setMargin ( final int top, final int left, final int bottom, final int right )
    {
        setMargin ( new Insets ( top, left, bottom, right ) );
    }

    @Override
    public void setMargin ( final Insets margin )
    {
        getWebUI ().setMargin ( margin );
    }

    @Override
    public Insets getPadding ()
    {
        return getWebUI ().getPadding ();
    }

    /**
     * Sets new padding.
     *
     * @param padding new padding
     */
    public void setPadding ( final int padding )
    {
        setPadding ( padding, padding, padding, padding );
    }

    /**
     * Sets new padding.
     *
     * @param top    new top padding
     * @param left   new left padding
     * @param bottom new bottom padding
     * @param right  new right padding
     */
    public void setPadding ( final int top, final int left, final int bottom, final int right )
    {
        setPadding ( new Insets ( top, left, bottom, right ) );
    }

    @Override
    public void setPadding ( final Insets padding )
    {
        getWebUI ().setPadding ( padding );
    }

    /**
     * Returns Web-UI applied to this class.
     *
     * @return Web-UI applied to this class
     */
    private WebTableHeaderUI getWebUI ()
    {
        return ( WebTableHeaderUI ) getUI ();
    }

    /**
     * Installs a Web-UI into this component.
     */
    @Override
    public void updateUI ()
    {
        if ( getUI () == null || !( getUI () instanceof WebTableHeaderUI ) )
        {
            try
            {
                setUI ( ( WebTableHeaderUI ) ReflectUtils.createInstance ( WebLookAndFeel.tableHeaderUI ) );
            }
            catch ( final Throwable e )
            {
                Log.error ( this, e );
                setUI ( new WebTableHeaderUI () );
            }
        }
        else
        {
            setUI ( getUI () );
        }
    }
}