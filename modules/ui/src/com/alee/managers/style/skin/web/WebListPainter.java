package com.alee.managers.style.skin.web;

import com.alee.extended.painter.AbstractPainter;
import com.alee.global.StyleConstants;
import com.alee.laf.list.ListPainter;
import com.alee.laf.list.WebList;
import com.alee.laf.list.WebListStyle;
import com.alee.laf.list.WebListUI;
import com.alee.managers.tooltip.ToolTipProvider;
import com.alee.utils.GeometryUtils;
import com.alee.utils.GraphicsUtils;
import com.alee.utils.LafUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * @author Alexandr Zernov
 */

public class WebListPainter<E extends JList, U extends WebListUI> extends AbstractPainter<E, U> implements ListPainter<E, U>
{
    /**
     * todo 1. Visual shade effect on sides when scrollable
     * todo 2. Even-odd cells highlight
     */

    protected static final int DROP_LINE_THICKNESS = 2;

    /**
     * Style settings.
     */
    protected int selectionRound = WebListStyle.selectionRound;
    protected int selectionShadeWidth = WebListStyle.selectionShadeWidth;
    protected Color selectionBorderColor = WebListStyle.selectionBorderColor;
    protected Color selectionBackgroundColor = WebListStyle.selectionBackgroundColor;
    protected boolean decorateSelection = WebListStyle.decorateSelection;
    protected boolean webColoredSelection = WebListStyle.webColoredSelection;

    /**
     * Listeners.
     */
    protected MouseAdapter mouseAdapter;
    protected ListSelectionListener selectionListener;

    /**
     * Runtime variables.
     */
    protected int rolloverIndex = -1;

    /**
     * Painting variables.
     */
    protected int layoutOrientation;
    protected CellRendererPane rendererPane;
    protected int[] cellHeights = null;
    protected int cellHeight = -1;
    protected int cellWidth = -1;
    protected int listHeight = -1;
    protected int listWidth = -1;
    protected int columnCount;
    protected int preferredHeight;
    protected int rowsPerColumn;
    protected boolean updateLayoutStateNeeded = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public void install ( final E c, final U ui )
    {
        super.install ( c, ui );

        // Rollover listener
        mouseAdapter = new MouseAdapter ()
        {
            @Override
            public void mouseMoved ( final MouseEvent e )
            {
                updateMouseover ( e );
            }

            @Override
            public void mouseDragged ( final MouseEvent e )
            {
                updateMouseover ( e );
            }

            @Override
            public void mouseExited ( final MouseEvent e )
            {
                clearMouseover ();
            }

            private void updateMouseover ( final MouseEvent e )
            {
                final int index = component.locationToIndex ( e.getPoint () );
                final Rectangle bounds = component.getCellBounds ( index, index );
                if ( component.isEnabled () && bounds != null && bounds.contains ( e.getPoint () ) )
                {
                    if ( rolloverIndex != index )
                    {
                        updateRolloverCell ( rolloverIndex, index );
                    }
                }
                else
                {
                    clearMouseover ();
                }
            }

            private void clearMouseover ()
            {
                if ( rolloverIndex != -1 )
                {
                    updateRolloverCell ( rolloverIndex, -1 );
                }
            }

            private void updateRolloverCell ( final int oldIndex, final int newIndex )
            {
                // Updating rollover index
                rolloverIndex = newIndex;

                // Repaint list only if rollover index is used
                if ( decorateSelection && ui.isHighlightRolloverCell () )
                {
                    final Rectangle oldBounds = component.getCellBounds ( oldIndex, oldIndex );
                    final Rectangle newBounds = component.getCellBounds ( newIndex, newIndex );
                    final Rectangle rect = GeometryUtils.getContainingRect ( oldBounds, newBounds );
                    if ( rect != null )
                    {
                        component.repaint ( rect );
                    }
                }

                // Updating custom WebLaF tooltip display state
                final ToolTipProvider provider = getToolTipProvider ();
                if ( provider != null )
                {
                    provider.rolloverCellChanged ( component, oldIndex, 0, newIndex, 0 );
                }
            }
        };
        component.addMouseListener ( mouseAdapter );
        component.addMouseMotionListener ( mouseAdapter );

        // Selection listener
        selectionListener = new ListSelectionListener ()
        {
            @Override
            public void valueChanged ( final ListSelectionEvent e )
            {
                if ( ui.isAutoScrollToSelection () )
                {
                    if ( component.getSelectedIndex () != -1 )
                    {
                        final int index = component.getLeadSelectionIndex ();
                        final Rectangle selection = ui.getCellBounds ( component, index, index );
                        if ( selection != null && !selection.intersects ( component.getVisibleRect () ) )
                        {
                            component.scrollRectToVisible ( selection );
                        }
                    }
                }
            }
        };
        component.addListSelectionListener ( selectionListener );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uninstall ( final E c, final U ui )
    {
        // Removing listeners
        component.removeMouseListener ( mouseAdapter );
        component.removeMouseMotionListener ( mouseAdapter );
        component.removeListSelectionListener ( selectionListener );
        mouseAdapter = null;
        selectionListener = null;
        cellHeights = null;

        super.uninstall ( c, ui );
    }

    /**
     * Paint the rows that intersect the Graphics objects clipRect.  This
     * method calls paintCell as necessary.  Subclasses
     * may want to override these methods.
     *
     * @see #paintCell
     */
    @Override
    public void paint ( final Graphics2D g2d, final Rectangle bounds, final E c, final U ui )
    {
        // prepare to paint
        layoutOrientation = component.getLayoutOrientation ();
        rendererPane = ui.getCellRendererPane ();

        final Shape clip = g2d.getClip ();
        paintImpl ( g2d, c );
        g2d.setClip ( clip );

        paintDropLine ( g2d );

        rendererPane = null;

        if ( updateLayoutStateNeeded )
        {
            ui.setNeedUpdateLayoutState ();
            updateLayoutStateNeeded = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareToPaint ( final boolean updateLayoutStateNeeded )
    {
        this.updateLayoutStateNeeded |= updateLayoutStateNeeded;
    }

    protected void paintImpl ( final Graphics g, final JComponent c )
    {
        switch ( component.getLayoutOrientation () )
        {
            case JList.VERTICAL_WRAP:
                if ( component.getHeight () != listHeight )
                {
                    updateLayoutStateNeeded = true;
                    redrawList ();
                    updateLayoutState ();
                }
                break;
            case JList.HORIZONTAL_WRAP:
                if ( component.getWidth () != listWidth )
                {
                    updateLayoutStateNeeded = true;
                    redrawList ();
                    updateLayoutState ();
                }
                break;
            default:
                break;
        }

        final ListCellRenderer renderer = component.getCellRenderer ();
        final ListModel dataModel = component.getModel ();
        final ListSelectionModel selModel = component.getSelectionModel ();
        final int size;

        if ( ( renderer == null ) || ( size = dataModel.getSize () ) == 0 )
        {
            return;
        }

        // Determine how many columns we need to paint
        final Rectangle paintBounds = g.getClipBounds ();

        final int startColumn;
        final int endColumn;
        if ( c.getComponentOrientation ().isLeftToRight () )
        {
            startColumn = convertLocationToColumn ( paintBounds.x, paintBounds.y );
            endColumn = convertLocationToColumn ( paintBounds.x + paintBounds.width, paintBounds.y );
        }
        else
        {
            startColumn = convertLocationToColumn ( paintBounds.x + paintBounds.width, paintBounds.y );
            endColumn = convertLocationToColumn ( paintBounds.x, paintBounds.y );
        }
        final int maxY = paintBounds.y + paintBounds.height;
        final int leadIndex = adjustIndex ( component.getLeadSelectionIndex (), component );
        final int rowIncrement = ( layoutOrientation == JList.HORIZONTAL_WRAP ) ? columnCount : 1;


        for ( int colCounter = startColumn; colCounter <= endColumn; colCounter++ )
        {
            // And then how many rows in this column
            int row = convertLocationToRowInColumn ( paintBounds.y, colCounter );
            final int rowCount = getRowCount ( colCounter );
            int index = getModelIndex ( colCounter, row );
            final Rectangle rowBounds = ui.getCellBounds ( component, index, index );

            if ( rowBounds == null )
            {
                // Not valid, bail!
                return;
            }
            while ( row < rowCount && rowBounds.y < maxY &&
                    index < size )
            {
                rowBounds.height = getHeight ( colCounter, row );
                g.setClip ( rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height );
                g.clipRect ( paintBounds.x, paintBounds.y, paintBounds.width, paintBounds.height );
                paintCell ( g, index, rowBounds, renderer, dataModel, selModel, leadIndex );
                rowBounds.y += rowBounds.height;
                index += rowIncrement;
                row++;
            }
        }
        // Empty out the renderer pane, allowing renderers to be gc'ed.
        rendererPane.removeAll ();
    }

    protected void redrawList ()
    {
        component.revalidate ();
        component.repaint ();
    }

    /**
     * Recompute the value of cellHeight or cellHeights based
     * and cellWidth, based on the current font and the current
     * values of fixedCellWidth, fixedCellHeight, and prototypeCellValue.
     */
    protected void updateLayoutState ()
    {
        /* If both JList fixedCellWidth and fixedCellHeight have been
         * set, then initialize cellWidth and cellHeight, and set
         * cellHeights to null.
         */

        final int fixedCellHeight = component.getFixedCellHeight ();
        final int fixedCellWidth = component.getFixedCellWidth ();

        cellWidth = ( fixedCellWidth != -1 ) ? fixedCellWidth : -1;

        if ( fixedCellHeight != -1 )
        {
            cellHeight = fixedCellHeight;
            cellHeights = null;
        }
        else
        {
            cellHeight = -1;
            cellHeights = new int[ component.getModel ().getSize () ];
        }

        /* If either of  JList fixedCellWidth and fixedCellHeight haven't
         * been set, then initialize cellWidth and cellHeights by
         * scanning through the entire model.  Note: if the renderer is
         * null, we just set cellWidth and cellHeights[*] to zero,
         * if they're not set already.
         */

        if ( ( fixedCellWidth == -1 ) || ( fixedCellHeight == -1 ) )
        {

            final ListModel dataModel = component.getModel ();
            final int dataModelSize = dataModel.getSize ();
            final ListCellRenderer renderer = component.getCellRenderer ();

            if ( renderer != null )
            {
                for ( int index = 0; index < dataModelSize; index++ )
                {
                    final Object value = dataModel.getElementAt ( index );
                    final Component c = renderer.getListCellRendererComponent ( component, value, index, false, false );
                    rendererPane.add ( c );
                    final Dimension cellSize = c.getPreferredSize ();
                    if ( fixedCellWidth == -1 )
                    {
                        cellWidth = Math.max ( cellSize.width, cellWidth );
                    }
                    if ( fixedCellHeight == -1 )
                    {
                        cellHeights[ index ] = cellSize.height;
                    }
                }
            }
            else
            {
                if ( cellWidth == -1 )
                {
                    cellWidth = 0;
                }
                if ( cellHeights == null )
                {
                    cellHeights = new int[ dataModelSize ];
                }
                for ( int index = 0; index < dataModelSize; index++ )
                {
                    cellHeights[ index ] = 0;
                }
            }
        }

        columnCount = 1;
        if ( layoutOrientation != JList.VERTICAL )
        {
            updateHorizontalLayoutState ( fixedCellWidth, fixedCellHeight );
        }
    }

    /**
     * Invoked when the list is layed out horizontally to determine how many columns to create.
     * <p/>
     * This updates the <code>rowsPerColumn, </code><code>columnCount</code>,
     * <code>preferredHeight</code> and potentially <code>cellHeight</code>
     * instance variables.
     */
    protected void updateHorizontalLayoutState ( final int fixedCellWidth, final int fixedCellHeight )
    {
        final int visRows = component.getVisibleRowCount ();
        final int dataModelSize = component.getModel ().getSize ();
        final Insets insets = component.getInsets ();

        listHeight = component.getHeight ();
        listWidth = component.getWidth ();

        if ( dataModelSize == 0 )
        {
            rowsPerColumn = columnCount = 0;
            preferredHeight = insets.top + insets.bottom;
            return;
        }

        final int height;

        if ( fixedCellHeight != -1 )
        {
            height = fixedCellHeight;
        }
        else
        {
            // Determine the max of the renderer heights.
            int maxHeight = 0;
            if ( cellHeights.length > 0 )
            {
                maxHeight = cellHeights[ cellHeights.length - 1 ];
                for ( int counter = cellHeights.length - 2; counter >= 0; counter-- )
                {
                    maxHeight = Math.max ( maxHeight, cellHeights[ counter ] );
                }
            }
            height = cellHeight = maxHeight;
            cellHeights = null;
        }
        // The number of rows is either determined by the visible row
        // count, or by the height of the list.
        rowsPerColumn = dataModelSize;
        if ( visRows > 0 )
        {
            rowsPerColumn = visRows;
            columnCount = Math.max ( 1, dataModelSize / rowsPerColumn );
            if ( dataModelSize > 0 && dataModelSize > rowsPerColumn &&
                    dataModelSize % rowsPerColumn != 0 )
            {
                columnCount++;
            }
            if ( layoutOrientation == JList.HORIZONTAL_WRAP )
            {
                // Because HORIZONTAL_WRAP flows differently, the
                // rowsPerColumn needs to be adjusted.
                rowsPerColumn = ( dataModelSize / columnCount );
                if ( dataModelSize % columnCount > 0 )
                {
                    rowsPerColumn++;
                }
            }
        }
        else if ( layoutOrientation == JList.VERTICAL_WRAP && height != 0 )
        {
            rowsPerColumn = Math.max ( 1, ( listHeight - insets.top -
                    insets.bottom ) / height );
            columnCount = Math.max ( 1, dataModelSize / rowsPerColumn );
            if ( dataModelSize > 0 && dataModelSize > rowsPerColumn &&
                    dataModelSize % rowsPerColumn != 0 )
            {
                columnCount++;
            }
        }
        else if ( layoutOrientation == JList.HORIZONTAL_WRAP && cellWidth > 0 &&
                listWidth > 0 )
        {
            columnCount = Math.max ( 1, ( listWidth - insets.left -
                    insets.right ) / cellWidth );
            rowsPerColumn = dataModelSize / columnCount;
            if ( dataModelSize % columnCount > 0 )
            {
                rowsPerColumn++;
            }
        }
        preferredHeight = rowsPerColumn * cellHeight + insets.top +
                insets.bottom;
    }

    /**
     * Returns the height of the cell at the passed in location.
     */
    protected int getHeight ( final int column, final int row )
    {
        if ( column < 0 || column > columnCount || row < 0 )
        {
            return -1;
        }
        if ( layoutOrientation != JList.VERTICAL )
        {
            return cellHeight;
        }
        if ( row >= component.getModel ().getSize () )
        {
            return -1;
        }
        return ( cellHeights == null ) ? cellHeight : ( ( row < cellHeights.length ) ? cellHeights[ row ] : -1 );
    }

    /**
     * Returns the model index for the specified display location.
     * If <code>column</code>x<code>row</code> is beyond the length of the
     * model, this will return the model size - 1.
     */
    protected int getModelIndex ( final int column, final int row )
    {
        switch ( layoutOrientation )
        {
            case JList.VERTICAL_WRAP:
                return Math.min ( component.getModel ().getSize () - 1, rowsPerColumn * column + Math.min ( row, rowsPerColumn - 1 ) );
            case JList.HORIZONTAL_WRAP:
                return Math.min ( component.getModel ().getSize () - 1, row * columnCount + column );
            default:
                return row;
        }
    }

    /**
     * Returns the number of rows in the given column.
     */
    protected int getRowCount ( final int column )
    {
        if ( column < 0 || column >= columnCount )
        {
            return -1;
        }
        if ( layoutOrientation == JList.VERTICAL || ( column == 0 && columnCount == 1 ) )
        {
            return component.getModel ().getSize ();
        }
        if ( column >= columnCount )
        {
            return -1;
        }
        if ( layoutOrientation == JList.VERTICAL_WRAP )
        {
            if ( column < ( columnCount - 1 ) )
            {
                return rowsPerColumn;
            }
            return component.getModel ().getSize () - ( columnCount - 1 ) * rowsPerColumn;
        }
        // JList.HORIZONTAL_WRAP
        final int diff = columnCount - ( columnCount * rowsPerColumn - component.getModel ().getSize () );

        if ( column >= diff )
        {
            return Math.max ( 0, rowsPerColumn - 1 );
        }
        return rowsPerColumn;
    }

    /**
     * Returns the closest row that starts at the specified y-location
     * in the passed in column.
     */
    protected int convertLocationToRowInColumn ( final int y, final int column )
    {
        int x = 0;

        if ( layoutOrientation != JList.VERTICAL )
        {
            if ( ltr )
            {
                x = column * cellWidth;
            }
            else
            {
                x = component.getWidth () - ( column + 1 ) * cellWidth - component.getInsets ().right;
            }
        }
        return convertLocationToRow ( x, y, true );
    }

    /**
     * Returns the row at location x/y.
     *
     * @param closest If true and the location doesn't exactly match a
     *                particular location, this will return the closest row.
     */
    protected int convertLocationToRow ( final int x, final int y0, final boolean closest )
    {
        final int size = component.getModel ().getSize ();

        if ( size <= 0 )
        {
            return -1;
        }
        final Insets insets = component.getInsets ();
        if ( cellHeights == null )
        {
            int row = ( cellHeight == 0 ) ? 0 : ( ( y0 - insets.top ) / cellHeight );
            if ( closest )
            {
                if ( row < 0 )
                {
                    row = 0;
                }
                else if ( row >= size )
                {
                    row = size - 1;
                }
            }
            return row;
        }
        else if ( size > cellHeights.length )
        {
            return -1;
        }
        else
        {
            int y = insets.top;
            int row = 0;

            if ( closest && y0 < y )
            {
                return 0;
            }
            int i;
            for ( i = 0; i < size; i++ )
            {
                if ( ( y0 >= y ) && ( y0 < y + cellHeights[ i ] ) )
                {
                    return row;
                }
                y += cellHeights[ i ];
                row += 1;
            }
            return i - 1;
        }
    }

    /**
     * Returns the closest column to the passed in location.
     */
    protected int convertLocationToColumn ( final int x, final int y )
    {
        if ( cellWidth > 0 )
        {
            if ( layoutOrientation == JList.VERTICAL )
            {
                return 0;
            }
            final Insets insets = component.getInsets ();
            final int col;
            if ( ltr )
            {
                col = ( x - insets.left ) / cellWidth;
            }
            else
            {
                col = ( component.getWidth () - x - insets.right - 1 ) / cellWidth;
            }
            if ( col < 0 )
            {
                return 0;
            }
            else if ( col >= columnCount )
            {
                return columnCount - 1;
            }
            return col;
        }
        return 0;
    }

    protected int adjustIndex ( final int index, final JList list )
    {
        return index < list.getModel ().getSize () ? index : -1;
    }

    protected void paintDropLine ( final Graphics g )
    {
        final JList.DropLocation loc = component.getDropLocation ();
        if ( loc == null || !loc.isInsert () )
        {
            return;
        }

        // todo check needs. Maybe move to the style
        //        final Color c = DefaultLookup.getColor ( list, this, "List.dropLineColor", null );
        //        if ( c != null )
        //        {
        //            g.setColor ( c );
        //            final Rectangle rect = getDropLineRect ( loc );
        //            g.fillRect ( rect.x, rect.y, rect.width, rect.height );
        //        }
    }

    //    private Rectangle getDropLineRect ( final JList.DropLocation loc )
    //    {
    //        final int size = list.getModel ().getSize ();
    //
    //        if ( size == 0 )
    //        {
    //            final Insets insets = list.getInsets ();
    //            if ( layoutOrientation == JList.HORIZONTAL_WRAP )
    //            {
    //                if ( ltr )
    //                {
    //                    return new Rectangle ( insets.left, insets.top, DROP_LINE_THICKNESS, 20 );
    //                }
    //                else
    //                {
    //                    return new Rectangle ( list.getWidth () - DROP_LINE_THICKNESS - insets.right, insets.top, DROP_LINE_THICKNESS, 20 );
    //                }
    //            }
    //            else
    //            {
    //                return new Rectangle ( insets.left, insets.top, list.getWidth () - insets.left - insets.right, DROP_LINE_THICKNESS );
    //            }
    //        }
    //
    //        Rectangle rect = null;
    //        int index = loc.getIndex ();
    //        boolean decr = false;
    //
    //        if ( layoutOrientation == JList.HORIZONTAL_WRAP )
    //        {
    //            if ( index == size )
    //            {
    //                decr = true;
    //            }
    //            else if ( index != 0 && convertModelToRow ( index ) != convertModelToRow ( index - 1 ) )
    //            {
    //                final Rectangle prev = getCellBounds ( list, index - 1 );
    //                final Rectangle me = getCellBounds ( list, index );
    //                final Point p = loc.getDropPoint ();
    //
    //                if ( ltr )
    //                {
    //                    decr = Point2D.distance ( prev.x + prev.width, prev.y + ( int ) ( prev.height / 2.0 ), p.x, p.y ) <
    //                            Point2D.distance ( me.x, me.y + ( int ) ( me.height / 2.0 ), p.x, p.y );
    //                }
    //                else
    //                {
    //                    decr = Point2D.distance ( prev.x, prev.y + ( int ) ( prev.height / 2.0 ), p.x, p.y ) <
    //                            Point2D.distance ( me.x + me.width, me.y + ( int ) ( prev.height / 2.0 ), p.x, p.y );
    //                }
    //            }
    //
    //            if ( decr )
    //            {
    //                index--;
    //                rect = getCellBounds ( list, index );
    //                if ( ltr )
    //                {
    //                    rect.x += rect.width;
    //                }
    //                else
    //                {
    //                    rect.x -= DROP_LINE_THICKNESS;
    //                }
    //            }
    //            else
    //            {
    //                rect = getCellBounds ( list, index );
    //                if ( !ltr )
    //                {
    //                    rect.x += rect.width - DROP_LINE_THICKNESS;
    //                }
    //            }
    //
    //            if ( rect.x >= list.getWidth () )
    //            {
    //                rect.x = list.getWidth () - DROP_LINE_THICKNESS;
    //            }
    //            else if ( rect.x < 0 )
    //            {
    //                rect.x = 0;
    //            }
    //
    //            rect.width = DROP_LINE_THICKNESS;
    //        }
    //        else if ( layoutOrientation == JList.VERTICAL_WRAP )
    //        {
    //            if ( index == size )
    //            {
    //                index--;
    //                rect = getCellBounds ( list, index );
    //                rect.y += rect.height;
    //            }
    //            else if ( index != 0 && convertModelToColumn ( index ) != convertModelToColumn ( index - 1 ) )
    //            {
    //
    //                final Rectangle prev = getCellBounds ( list, index - 1 );
    //                final Rectangle me = getCellBounds ( list, index );
    //                final Point p = loc.getDropPoint ();
    //                if ( Point2D.distance ( prev.x + ( int ) ( prev.width / 2.0 ), prev.y + prev.height, p.x, p.y ) <
    //                        Point2D.distance ( me.x + ( int ) ( me.width / 2.0 ), me.y, p.x, p.y ) )
    //                {
    //                    index--;
    //                    rect = getCellBounds ( list, index );
    //                    rect.y += rect.height;
    //                }
    //                else
    //                {
    //                    rect = getCellBounds ( list, index );
    //                }
    //            }
    //            else
    //            {
    //                rect = getCellBounds ( list, index );
    //            }
    //
    //            if ( rect.y >= list.getHeight () )
    //            {
    //                rect.y = list.getHeight () - DROP_LINE_THICKNESS;
    //            }
    //
    //            rect.height = DROP_LINE_THICKNESS;
    //        }
    //        else
    //        {
    //            if ( index == size )
    //            {
    //                index--;
    //                rect = getCellBounds ( list, index );
    //                rect.y += rect.height;
    //            }
    //            else
    //            {
    //                rect = getCellBounds ( list, index );
    //            }
    //
    //            if ( rect.y >= list.getHeight () )
    //            {
    //                rect.y = list.getHeight () - DROP_LINE_THICKNESS;
    //            }
    //
    //            rect.height = DROP_LINE_THICKNESS;
    //        }
    //
    //        return rect;
    //    }

    //    /**
    //     * Returns the row that the model index <code>index</code> will be displayed in..
    //     */
    //    protected int convertModelToRow ( final int index )
    //    {
    //        final int size = list.getModel ().getSize ();
    //
    //        if ( ( index < 0 ) || ( index >= size ) )
    //        {
    //            return -1;
    //        }
    //
    //        if ( layoutOrientation != JList.VERTICAL && columnCount > 1 &&
    //                rowsPerColumn > 0 )
    //        {
    //            if ( layoutOrientation == JList.VERTICAL_WRAP )
    //            {
    //                return index % rowsPerColumn;
    //            }
    //            return index / columnCount;
    //        }
    //        return index;
    //    }

    //    /**
    //     * Returns the column that the model index <code>index</code> will be displayed in.
    //     */
    //    protected int convertModelToColumn ( final int index )
    //    {
    //        final int size = list.getModel ().getSize ();
    //
    //        if ( ( index < 0 ) || ( index >= size ) )
    //        {
    //            return -1;
    //        }
    //
    //        if ( layoutOrientation != JList.VERTICAL && rowsPerColumn > 0 &&
    //                columnCount > 1 )
    //        {
    //            if ( layoutOrientation == JList.VERTICAL_WRAP )
    //            {
    //                return index / rowsPerColumn;
    //            }
    //            return index % columnCount;
    //        }
    //        return 0;
    //    }
    //
    //    /**
    //     * Gets the bounds of the specified model index, returning the resulting
    //     * bounds, or null if <code>index</code> is not valid.
    //     */
    //    protected Rectangle getCellBounds ( final JList list, final int index )
    //    {
    //        return ui.getCellBounds ( list, index, index );
    //    }

    /**
     * Paint one List cell: compute the relevant state, get the "rubber stamp" cell renderer component, and then use the CellRendererPane
     * to paint it. Subclasses may want to override this method rather than paint().
     *
     * @param g            graphics context
     * @param index        cell index
     * @param rowBounds    cell bounds
     * @param cellRenderer cell renderer
     * @param dataModel    list model
     * @param selModel     list selection model
     * @param leadIndex    lead cell index
     * @see #paint
     */
    protected void paintCell ( final Graphics g, final int index, final Rectangle rowBounds, final ListCellRenderer cellRenderer,
                               final ListModel dataModel, final ListSelectionModel selModel, final int leadIndex )
    {
        //        if ( list.getLayoutOrientation () == WebList.VERTICAL && ( evenLineColor != null || oddLineColor != null ) )
        //        {
        //            boolean even = index % 2 == 0;
        //            if ( even && evenLineColor != null )
        //            {
        //                g.setColor ( evenLineColor );
        //                g.fillRect ( rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height );
        //            }
        //            if ( !even && oddLineColor != null )
        //            {
        //                g.setColor ( oddLineColor );
        //                g.fillRect ( rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height );
        //            }
        //        }

        final Object value = dataModel.getElementAt ( index );
        final boolean isSelected = selModel.isSelectedIndex ( index );

        if ( decorateSelection && ( isSelected || index == rolloverIndex ) )
        {
            final Graphics2D g2d = ( Graphics2D ) g;
            final Composite oc = GraphicsUtils.setupAlphaComposite ( g2d, 0.35f, !isSelected );

            final Rectangle rect = new Rectangle ( rowBounds );
            rect.x += selectionShadeWidth;
            rect.y += selectionShadeWidth;
            rect.width -= selectionShadeWidth * 2 + ( selectionBorderColor != null ? 1 : 0 );
            rect.height -= selectionShadeWidth * 2 + ( selectionBorderColor != null ? 1 : 0 );

            LafUtils.drawCustomWebBorder ( g2d, component,
                    new RoundRectangle2D.Double ( rect.x, rect.y, rect.width, rect.height, selectionRound * 2, selectionRound * 2 ),
                    StyleConstants.shadeColor, selectionShadeWidth, true, webColoredSelection, selectionBorderColor, selectionBorderColor,
                    selectionBackgroundColor );

            GraphicsUtils.restoreComposite ( g2d, oc, !isSelected );
        }

        final boolean cellHasFocus = component.hasFocus () && ( index == leadIndex );
        final Component rendererComponent = cellRenderer.getListCellRendererComponent ( component, value, index, isSelected, cellHasFocus );
        rendererPane.paintComponent ( g, rendererComponent, component, rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height, true );
    }

    /**
     * Returns custom WebLaF tooltip provider.
     *
     * @return custom WebLaF tooltip provider
     */
    protected ToolTipProvider<? extends WebList> getToolTipProvider ()
    {
        return component != null && component instanceof WebList ? ( ( WebList ) component ).getToolTipProvider () : null;
    }

    /**
     * Returns whether should decorate selected and rollover cells or not.
     *
     * @return true if should decorate selected and rollover cells, false otherwise
     */
    public boolean isDecorateSelection ()
    {
        return decorateSelection;
    }

    /**
     * Sets whether should decorate selected and rollover cells or not.
     *
     * @param decorateSelection whether should decorate selected and rollover cells or not
     */
    public void setDecorateSelection ( final boolean decorateSelection )
    {
        this.decorateSelection = decorateSelection;
    }

    /**
     * Returns cells selection rounding.
     *
     * @return cells selection rounding
     */
    public int getSelectionRound ()
    {
        return selectionRound;
    }

    /**
     * Sets cells selection rounding.
     *
     * @param selectionRound new cells selection rounding
     */
    public void setSelectionRound ( final int selectionRound )
    {
        this.selectionRound = selectionRound;
    }

    /**
     * Returns cells selection shade width.
     *
     * @return cells selection shade width
     */
    public int getSelectionShadeWidth ()
    {
        return selectionShadeWidth;
    }

    /**
     * Sets cells selection shade width.
     *
     * @param selectionShadeWidth new cells selection shade width
     */
    public void setSelectionShadeWidth ( final int selectionShadeWidth )
    {
        this.selectionShadeWidth = selectionShadeWidth;
    }

    /**
     * Returns whether selection should be web-colored or not.
     * In case it is not web-colored selectionBackgroundColor value will be used as background color.
     *
     * @return true if selection should be web-colored, false otherwise
     */
    public boolean isWebColoredSelection ()
    {
        return webColoredSelection;
    }

    /**
     * Sets whether selection should be web-colored or not.
     * In case it is not web-colored selectionBackgroundColor value will be used as background color.
     *
     * @param webColored whether selection should be web-colored or not
     */
    public void setWebColoredSelection ( final boolean webColored )
    {
        this.webColoredSelection = webColored;
    }

    /**
     * Returns selection border color.
     *
     * @return selection border color
     */
    public Color getSelectionBorderColor ()
    {
        return selectionBorderColor;
    }

    /**
     * Sets selection border color.
     *
     * @param color selection border color
     */
    public void setSelectionBorderColor ( final Color color )
    {
        this.selectionBorderColor = color;
    }

    /**
     * Returns selection background color.
     * It is used only when webColoredSelection is set to false.
     *
     * @return selection background color
     */
    public Color getSelectionBackgroundColor ()
    {
        return selectionBackgroundColor;
    }

    /**
     * Sets selection background color.
     * It is used only when webColoredSelection is set to false.
     *
     * @param color selection background color
     */
    public void setSelectionBackgroundColor ( final Color color )
    {
        this.selectionBackgroundColor = color;
    }
}