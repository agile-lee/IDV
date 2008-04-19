/*
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;
import ucar.unidata.collab.SharableImpl;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;

import ucar.unidata.data.DataTimeRange;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.data.sounding.TrackDataSource;


import ucar.unidata.idv.ControlContext;
import ucar.unidata.ui.drawing.*;


import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.ShapeUtility;

import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.DisplayableDataRef;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.SelectRangeDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.display.TrackDisplayable;



import visad.*;

import visad.georef.EarthLocationLite;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



/**
 * A MetApps Display Control with Displayable and controls for
 * displaying a track (balloon sounding or aircraft track)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class TrackControl extends GridDisplayControl {

    //    extends DisplayControlImpl {


    /** mutex */
    private final Object DATA_MUTEX = new Object();

    /** dummy data */
    private static final Data DUMMY_DATA = new Real(0);

    /** the displayable for the track */
    TrackDisplayable trackDisplay;

    /** the displayable for the track data range */
    SelectRangeDisplayable selectRangeDisplay;

    /** track width */
    int trackWidth = 2;

    /** slider label */
    private JLabel sliderLabel;

    /** Shows the width */
    private JLabel widthLabel;

    /** widget */
    private JButton changeButton;

    /** Entire track type */
    private static final String CMD_ALL = TrackDataSource.ID_WHOLETRACE;

    /** Point track type */
    private static final String CMD_POINT = TrackDataSource.ID_POINTTRACE;

    /** range track type */
    private static final String CMD_RANGE = TrackDataSource.ID_TIMETRACE;

    /** animation end type */
    private static final String CMD_ANIMATION_TIME = "animation_time";

    /** track end type */
    private static final String CMD_TRACK_TIME = "track_time";

    /** track type */
    private String trackType = CMD_RANGE;

    /** selector point */
    private StationModelDisplayable indicator = null;



    /** text field for marker text */
    private JTextField markerTextField;

    /** combobox for maker symbol */
    private JComboBox symbolBox;

    /** marker text */
    private String markerText = "";

    /** marker symbol name */
    private String markerSymbol = ShapeUtility.NONE;

    /** layout model */
    private StationModel layoutModel = null;

    /** The last time range we used */
    private Range lastRange;

    /** Last position */
    private EarthLocationLite lastIndicatorPosition;

    /** indicator time */
    private DateTime lastIndicatorTime;

    /** flag for the marker being visible */
    private boolean markerVisible = false;

    /** flag for using track times */
    private boolean useTrackTimes = false;

    /** flag for using time subset enabled */
    private boolean timeDeclutterEnabled = false;

    /** flag for using time subset enabled */
    private boolean askedUserToDeclutterTime = false;

    /** number of minutes for time subsetting */
    private double timeDeclutterMinutes = 1;

    /** The two time declutter components */
    JComponent[] timeDeclutterComps;

    /** Holds the timeDeclutterMinutes */
    private JTextField timeDeclutterFld;

    /** checkbox */
    private JCheckBox timeDeclutterCbx;

    /** ignore changes */
    private boolean ignoreTimeDeclutterEnabled = false;

    /** The scale the user can enter */
    private float markerScale = 1.0f;

    /** Time strings */
    private final static String[] TIMES_TO_USE = { "Nominal Time",
            "Track Times" };

    //J-
    // The next 3 fields are deprecated but here for old bundles
    /** flag for using time subset enabled */
    private boolean timeSubsetEnabled = false;
    /** flag for using time subset minutes */
    private double timeSubsetMinutes = 30;
    /** Is time relative to track or animation */
    private boolean useTrackTime = true;
    //J+



    /**
     * Create a new Track Control; set the attribute flags
     */
    public TrackControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_TIMERANGE);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private boolean trackDataOk() throws VisADException, RemoteException {
        DataInstance dataInstance = getDataInstance();
        if ((dataInstance == null) || !dataInstance.dataOk()) {
            return false;
        }
        return true;
    }

    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !trackDataOk()) {
            return false;
        }
        trackDisplay = new TrackDisplayable("track" + dataChoice);
        setLineWidth(trackWidth);
        addDisplayable(trackDisplay, getAttributeFlags());
        selectRangeDisplay = new SelectRangeDisplayable();
        addDisplayable(selectRangeDisplay,
                       FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
        getAnimation();
        indicator = new StationModelDisplayable("indicator");
        indicator.setScale(markerScale);
        indicator.setShouldUseAltitude(true);
        updateIndicator();
        addDisplayable(indicator);
        timesHolder = new LineDrawing("track_time" + dataChoice);
        timesHolder.setManipulable(false);
        timesHolder.setVisible(false);
        addDisplayable(timesHolder);
        return setData(dataChoice);
    }

    /**
     * Update the indicator with new shapes
     */
    private void updateIndicator() {
        if (indicator != null) {
            try {
                lastIndicatorPosition = null;
                indicator.setStationModel(getMarkerLayout());
                indicator.setVisible(getMarkerVisible());
                setScaleOnMarker();
                applyTimeRange();
            } catch (Exception exc) {
                logException("Updating indicator", exc);
            }
        }
    }

    /**
     * Return the label that is to be used for the color widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the color widget
     */
    public String getColorWidgetLabel() {
        return "Marker Color";
    }

    /**
     * Set the dataInstance and its paramName
     * in the superclass member data. Set the dataInstance in this class
     * member getGridDataInstance(). Get appropritate contour information
     * for the data.
     *
     * @param dataInstance  the data instance to use
     */
    protected void setDataInstance(DataInstance dataInstance) {
        super.setDataInstance(dataInstance);
        try {
            if (selectRangeDisplay != null) {
                selectRangeDisplay.setSelectRealType(
                    getGridDataInstance().getRealType(0));
            }
        } catch (Exception exc) {
            logException("setting range real type", exc);
        }
    }

    /**
     * Get the display center
     *
     * @return  the center point of the display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public LatLonPoint getDisplayCenter()
            throws RemoteException, VisADException {
        FlatField flatField = getFlatField();
        if (flatField == null) {
            return null;
        }
        Set domainSet = flatField.getDomainSet();
        Real[] llaR = DataUtility.getSample(domainSet,
                                            domainSet.getLength()
                                            - 1).getRealComponents();
        return new LatLonTuple(llaR[0].getValue(), llaR[1].getValue());

    }



    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        if ( !isInitDone()) {
            return;
        }
        try {
            applyTimeRange();
        } catch (Exception exc) {
            logException("applyTimeRange", exc);
        }
    }


    /**
     * Set the data in this control
     *
     * @param choice    data choice
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if (trackDisplay == null) {
            return true;
        }
        if ( !super.setData(choice)) {
            //For now don't assume null data is bad.
            //This way we can come back from a bundle that is polling
            //on as yet non-existent data
            //            return false;
        }
        Unit newUnit = getDisplayUnit();
        //TODO: use the right index
        if ((newUnit != null) && !newUnit.equals(getDisplayUnit())
                && Unit.canConvert(newUnit, getRawDataUnit())) {
            trackDisplay.setDisplayUnit(newUnit);
            selectRangeDisplay.setDisplayUnit(newUnit);
        }
        FlatField ff   = null;
        FieldImpl grid = null;
        if ( !trackDataOk()) {
            if (trackDisplay != null) {
                trackDisplay.setData(DUMMY_DATA);
                indicator.setVisible(false);
                timesHolder.setData(DUMMY_DATA);
            }
            return true;
        }

        if (indicator != null) {
            indicator.setVisible(getMarkerVisible());
        }
        GridDataInstance gdi = getGridDataInstance();
        synchronized (gdi) {
            ff = getFlatField();
            if (ff != null) {
                grid = getGridDataInstance().getGrid(false);
            }
        }
        if ((ff != null) && (grid != null)) {
            updateTimeSelectRange();
            trackDisplay.setTrack(grid);
            setTrackTimes();
            applyTimeRange();
        }
        return true;
    }

    /**
     * Set the times on the track
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    private void setTrackTimes() throws VisADException, RemoteException {
        if ( !(trackType.equals(CMD_RANGE) || (trackDisplay == null))) {
            return;
        }
        Data d = trackDisplay.getData();
        if (d.equals(DUMMY_DATA)) {
            return;
        }
        if ( !getUseTrackTimes()) {
            timesHolder.setData(DUMMY_DATA);
            return;
        }
        FlatField f = (FlatField) ((FieldImpl) d).getSample(0);
        //System.out.println(f.getType());
        double[][] samples  = f.getValues(false);
        int        numTimes = samples[1].length;
        if ( !getTimeDeclutterEnabled()) {
            if ( !getAskedUserToDeclutterTime() && (numTimes > 1000)) {
                int success =
                    GuiUtils.showYesNoCancelDialog(getWindow(),
                        "<html>There are " + numTimes
                        + " time steps in the data.<br>Do you want to show them all?</html>", "Time Declutter", GuiUtils.CMD_NO);
                if (success == JOptionPane.CANCEL_OPTION) {
                    return;
                } else {
                    setAskedUserToDeclutterTime(true);
                    setTimeDeclutterEnabled(success
                                            == JOptionPane.NO_OPTION);
                }
            }
        }

        double[] times = samples[1];

        if (getTimeDeclutterEnabled()) {
            LogUtil.message("Track display: subsetting times");
            Trace.call1("declutterTime");
            times = declutterTime(times);
            Trace.call2("declutterTime");
            LogUtil.message("");
        }
        Unit[] units = f.getDefaultRangeUnits();
        Gridded1DDoubleSet timeSet =
            new Gridded1DDoubleSet(RealTupleType.Time1DTuple, new double[][] {
            times
        }, times.length, (CoordinateSystem) null, new Unit[] { units[1] },
           (ErrorEstimate[]) null, false /*don't copy*/);
        if (timeSet != null) {
            timesHolder.setData(timeSet);
        }
    }

    /**
     * Declutter in time.
     *
     * @param times the original times
     *
     * @return the decluttered times
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double[] declutterTime(double[] times)
            throws VisADException, RemoteException {
        int numTimes = times.length;
        int seconds  = (int) (timeDeclutterMinutes * 60);
        if (seconds == 0) {
            seconds = 1;
        }
        double[] tmpTimes  = new double[times.length];
        int numFound = 0;
        Hashtable seenTime   = new Hashtable();
        for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
            Integer timeKey = new Integer((int)(times[timeIdx] / seconds));
            if ((timeIdx < numTimes - 1) && (seenTime.get(timeKey) != null)) {
                continue;
            }
            seenTime.put(timeKey, timeKey);
            tmpTimes[numFound++] = times[timeIdx];
        }
        double[] newTimes = new double[numFound];
        System.arraycopy(tmpTimes, 0, newTimes, 0, numFound);
        return newTimes;
    }



    /*
    public GridDataInstance getGridDataInstance() {
        return (GridDataInstance) getDataInstance();
    }

    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        return  new GridDataInstance(dataChoice,
                getDataSelection(), getRequestProperties());
                }
    */



    /**
     * Get the data for this (without time dimension);
     *
     * @return the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FlatField getFlatField() throws VisADException, RemoteException {
        if ( !trackDataOk()) {
            return null;
        }

        if (true) {
            //            System.err.println ("Using new getFlatField");
            return DataUtil.getFlatField(getDataInstance().getData());
        }

        /*
        GridDataInstance gdi = getGridDataInstance();
        FieldImpl fi = gdi.getGrid(false);
        if (fi != null) {
            return (fi instanceof FlatField)
                   ? (FlatField) fi
                   : (FlatField) fi.getSample(0, false);
                   }*/
        return null;
    }

    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JComponent contents = (JComponent) super.doMakeContents();
        return GuiUtils.top(contents);
    }


    /**
     * Handle when the time declutering state has changed
     */
    protected void timeDeclutterChanged() {
        try {
            setTrackTimes();
        } catch (Exception e) {
            logException("setTrackTimes", e);
        }
    }


    /**
     * Make the gui panel for the time decluttering
     *
     * @return The time declutter panel
     */
    private JComponent[] getTimeDeclutterComps() {
        if (timeDeclutterComps != null) {
            return timeDeclutterComps;
        }
        ActionListener timeDeclutterListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    if (ignoreTimeDeclutterEnabled) {
                        return;
                    }
                    timeDeclutterMinutes =
                        Misc.parseNumber(timeDeclutterFld.getText().trim());
                    //Only do this when there was a change in the enabled
                    //or (when the text field had a return pressed event)
                    //when the value changed and we are enabled
                    if (timeDeclutterEnabled
                            != timeDeclutterCbx.isSelected()) {
                        timeDeclutterEnabled = timeDeclutterCbx.isSelected();
                        timeDeclutterChanged();
                    } else if (timeDeclutterEnabled) {
                        timeDeclutterChanged();
                    }
                } catch (NumberFormatException nfe) {
                    userErrorMessage("Bad number format");
                }
            }
        };
        timeDeclutterFld = new JTextField(
            getDisplayConventions().format(getTimeDeclutterMinutes()), 4);
        timeDeclutterFld.addActionListener(timeDeclutterListener);
        timeDeclutterCbx = new JCheckBox("", getTimeDeclutterEnabled());
        timeDeclutterCbx.addActionListener(timeDeclutterListener);
        return timeDeclutterComps = new JComponent[] { timeDeclutterCbx,
                timeDeclutterFld };
    }


    /**
     * Add control widgets specific to this control to the list
     *
     * @param controlWidgets   list of control widgets
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {

        super.getControlWidgets(controlWidgets);

        if (trackType.equals(CMD_RANGE)) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Times to Use:"),
                    doMakeTimeOptionWidget(), null));
        }

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Marker:"),
                                             makeLayoutModelWidget()));


        controlWidgets.add(
            new WrapperWidget(
                this,
                GuiUtils.topCenter(
                    GuiUtils.rLabel(getLineWidthWidgetLabel() + ":"),
                    GuiUtils.filler()), getLineWidthWidget().getContents(
                        false)));


    }

    /**
     * Make the time option widget
     *
     * @return  the time option widget
     */
    private Component doMakeTimeOptionWidget() {
        JComboBox box = new JComboBox(TIMES_TO_USE);
        box.setSelectedIndex(getUseTrackTimes()
                             ? 1
                             : 0);
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setUseTrackTimes(
                    ((JComboBox) e.getSource()).getSelectedIndex() == 1);
            }
        });

        JComponent[] timeDeclutterComps = getTimeDeclutterComps();
        JPanel timeDeclutter =
            GuiUtils.left(GuiUtils.hflow(Misc.newList(new Component[] {
            box, new JLabel(" Show Every: "), timeDeclutterComps[1],
            new JLabel(" minutes "), timeDeclutterComps[0],
            new JLabel("enabled")
        }), 2, 1));
        return timeDeclutter;

    }

    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     * @param preSelectedDataChoices set of preselected data choices
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties,
                                       List preSelectedDataChoices) {
        // in version 1.2, there were widgets to do data selection
        // which have been subsumed by the DataTime widget.
        if (timeSubsetEnabled) {
            DataTimeRange dtr = new DataTimeRange();
            dtr.setEndMode((useTrackTime)
                           ? dtr.MODE_DATA
                           : dtr.MODE_ANIMATION);
            dtr.setStartMode(dtr.MODE_RELATIVE);
            dtr.setStartOffsetMinutes(-1 * timeSubsetMinutes);
            setDataTimeRange(dtr);
        }
        super.initAfterUnPersistence(vc, properties, preSelectedDataChoices);
    }


    /**
     * Add display settings paricular to this control
     *
     * @param dsd  the DisplaySettingsDialog
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        if (getDataTimeRange() != null) {
            dsd.addPropertyValue(getDataTimeRange(), "dataTimeRange",
                                 "Time Mode", "Display");
        }
        dsd.addPropertyValue(new Boolean(getUseTrackTimes()),
                             "useTrackTimes", "Use Track Times",
                             SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Boolean(getTimeDeclutterEnabled()),
                             "timeDeclutterEnabled", "Subset Times",
                             SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Float(getTimeDeclutterMinutes()),
                             "timeDeclutterMinutes", "Subset Interval (min)",
                             SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(getMarkerLayout(), "markerLayout",
                             "Marker Layout", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Float(markerScale), "markerScale",
                             "Marker Scale", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Integer(getLineWidth()), "lineWidth",
                             "Line Width", SETTINGS_GROUP_DISPLAY);
    }


    /**
     * Set the DataTimeRange
     *
     * @param range  the DataTimeRange
     */
    public void setDataTimeRange(DataTimeRange range) {
        super.setDataTimeRange(range);
        if (getHaveInitialized()) {
            applyTimeRange();
        }
    }


    /**
     * Handle some sort of time change.  Either the subsetting interval
     * changes or there is a new timestep.
     */
    public void applyTimeRange() {
        try {
            if ( !trackDataOk()) {
                return;
            }
            DataTimeRange    dataTimeRange    = getDataTimeRange(true);
            GridDataInstance gridDataInstance = getGridDataInstance();
            FlatField        flatField;
            Unit             dataTimeUnit;
            synchronized (gridDataInstance) {
                flatField = getFlatField();
                if (flatField == null) {
                    return;
                }
                dataTimeUnit = gridDataInstance.getRawUnit(1);
            }
            Range    r                = getRangeForTimeSelect();
            RealType dataTimeRealType = Util.getRealType(dataTimeUnit);
            Real startReal = new Real(dataTimeRealType, r.getMin(),
                                      dataTimeUnit);
            Real endReal = new Real(dataTimeRealType, r.getMax(),
                                    dataTimeUnit);



            Animation anime    = getAnimation();
            Real      aniValue = ((anime != null)
                                  ? anime.getAniValue()
                                  : null);

            Real[] startEnd = getDataTimeRange().getTimeRange(startReal,
                                  endReal, aniValue);


            double startDate = startEnd[0].getValue(dataTimeUnit);
            double endDate   = startEnd[1].getValue(dataTimeUnit);
            if ( !Misc.equals(lastRange, new Range(startDate, endDate))) {
                lastRange = new Range(startDate, endDate);
                if (trackDisplay != null) {
                    trackDisplay.setSelectedRange(startDate, endDate);
                }
            }
            int      index = 0;
            double[] times = flatField.getValues(false)[1];
            for (; index < times.length; index++) {
                if (times[index] >= endDate) {
                    index--;
                    break;
                }
                if (times[index] == endDate) {
                    break;
                }
            }
            if (index == times.length) {
                index--;
            }
            if (index < 0) {
                index = 0;
            }
            Real[] llaR = DataUtility.getSample(flatField.getDomainSet(),
                              index).getRealComponents();
            if (llaR != null) {
                EarthLocationLite elt = new EarthLocationLite(llaR[0],
                                            llaR[1], llaR[2]);

                if ( !Misc.equals(elt, lastIndicatorPosition)) {
                    lastIndicatorTime = new DateTime(times[index],
                            dataTimeUnit);
                    lastIndicatorPosition = elt;
                    indicator.setStationData(
                        PointObFactory.makePointObs(elt));
                    doShare(ProbeControl.SHARE_POSITION, elt);
                    updateDisplayList();
                }
            }
        } catch (Exception e) {
            logException("applyTimeRange", e);
        }
    }


    /**
     * Override the base class method that creates request properties
     * and add in the appropriate 2d/3d request parameter.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        props.put(TrackDataSource.PROP_TRACKTYPE, trackType);
        return props;
    }

    /**
     * Update the select range for the widget with the data's range
     */
    private void updateTimeSelectRange() {
        try {
            Range r = getRangeForTimeSelect();
            if (r == null) {
                return;
            }
            if (trackDisplay != null) {
                trackDisplay.setRangeForSelect(r.getMin(), r.getMax());
            }
        } catch (Exception e) {
            logException("updateTimeSelectRange", e);
        }
    }

    /**
     * Get the range for selection.
     *
     * @return the Range
     *
     * @throws RemoteException remote data error
     * @throws VisADException  VisAD error
     */
    private Range getRangeForTimeSelect()
            throws VisADException, RemoteException {
        Range            range = getRange();
        GridDataInstance gdi   = getGridDataInstance();
        if ((gdi != null) && (gdi.getNumRealTypes() > 1)) {
            range = gdi.getRange(1);
        }
        return range;
    }

    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        return false;
    }

    /**
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges. The default is 1 though if there is not more than
     * one field in the range then we end up with the 0th value.
     * @return  0
     */
    protected int getColorRangeIndex() {
        return 0;
    }

    /**
     * Method called by other classes that share the selector.
     *
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if ( !getHaveInitialized()) {
            return;
        }
        if (dataId.equals(SHARE_SELECTRANGE)
                && ((DataChoice) data[0]).basicallyEquals(getDataChoice())) {

            try {
                setSelectRange((Range) data[1]);
            } catch (Exception exc) {
                logException("receiveShareData.level", exc);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }

    /**
     * Set the track width property.  Used by persistence
     *
     * @param width  width for track (pixels)
     * @deprecated  use #setLineWidth(int)
     */
    public void setTrackWidth(int width) {
        setLineWidth(width);
    }

    /**
     * Set the track width property.  Used by persistence
     *
     * @param width  width for track (pixels)
     * @deprecated  use #setLineWidth(int)
     */
    public void setLineWidth(int width) {
        trackWidth = width;
        try {
            super.setLineWidth(width);
            if (trackDisplay != null) {
                trackDisplay.setLineWidth(width);
            }
        } catch (Exception ve) {
            logException("setting line width", ve);
        }
    }

    /**
     * Get the line width property.
     *
     * @return The line width
     */
    public int getLineWidth() {
        return trackWidth;
    }

    /**
     * Creates a station model from the supplied parameters.
     * @return
     */
    private StationModel makeLayoutModel() {
        StationModel layout = null;
        // check if we are unpersisting from an old bundle.
        if ( !(markerText.equals("")
                || markerSymbol.equals(ShapeUtility.NONE))) {
            layout = new StationModel("TrackLocation");
            LabelSymbol textSymbol = new LabelSymbol();
            textSymbol.setValue(markerText);

            ShapeSymbol shapeSymbol = new ShapeSymbol(0, 0);
            shapeSymbol.setShape(markerSymbol);

            if (getColor() != null) {
                shapeSymbol.setForeground(getColor());
                textSymbol.setForeground(getColor());
            } else {
                shapeSymbol.setForeground(Color.magenta);
                textSymbol.setForeground(Color.magenta);
            }

            shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
            shapeSymbol.setRectPoint(Glyph.PT_MM);

            boolean showId     = !markerText.equals("");
            boolean showSymbol = !markerSymbol.equals(ShapeUtility.NONE);
            if (showId) {
                layout.addSymbol(textSymbol);
            }
            if (showSymbol) {
                layout.addSymbol(shapeSymbol);
            }
            if (showSymbol && showId) {
                textSymbol.bounds = new java.awt.Rectangle(-11, -31, 72, 24);
                textSymbol.setRectPoint(Glyph.PT_LM);
            } else if (showId) {
                textSymbol.bounds = new java.awt.Rectangle(-11, -8, 72, 24);
                textSymbol.setRectPoint(Glyph.PT_MM);
            } else if (showSymbol) {
                //Same position as above
            }

        } else {

            String name = "Location";
            layout =
                getControlContext().getStationModelManager().getStationModel(
                    name);
            if (layout == null) {
                LogUtil.userErrorMessage("Unable to find layout model: "
                                         + name + ". Using default");
            }
        }

        if (layout == null) {
            layout =
                getControlContext().getStationModelManager()
                    .getDefaultStationModel();
        }
        return layout;
    }

    /**
     * Collect the time animation set from the displayables.
     * If none found then return null.
     *
     * @return Animation set
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Set getDataTimeSet() throws RemoteException, VisADException {
        return (lastIndicatorTime == null)
               ? null
               : DateTime.makeTimeSet(new DateTime[] { lastIndicatorTime });
    }

    /**
     * Get the track width property.  Used by persistence
     * @return  width
     */
    protected int getTrackWidth() {
        return getLineWidth();
    }

    /**
     * Set the track type for this control.  Used by persistence
     *
     * @param type    track type
     */
    public void setTrackType(String type) {
        trackType = type;
    }

    /**
     * Get the track type for this control.  Used by persistence
     *
     * @return the track type
     */
    public String getTrackType() {
        return trackType;
    }

    /**
     * Set the TimeSubsetMinutes property.
     *
     * @param value The new value for TimeSubsetMinutes
     * @deprecated
     */
    public void setTimeSubsetMinutes(double value) {
        timeSubsetMinutes = value;
    }


    /**
     * Set the TimeSubsetEnabled property.
     *
     * @deprecated
     * @param value The new value for TimeSubsetEnabled
     */
    public void setTimeSubsetEnabled(boolean value) {
        timeSubsetEnabled = value;
    }


    /**
     * Set the useTrackTime property.
     *
     * @param value The new value for useTrackTime
     * @deprecated
     */
    public void setUseTrackTime(boolean value) {
        useTrackTime = value;
    }

    /**
     * Set the useTrackTimes property.
     *
     * @param value The new value for useTrackTime
     * @deprecated
     */
    public void setUseTrackTimes(boolean value) {
        useTrackTimes = value;
        if (getHaveInitialized()) {
            try {
                setTrackTimes();
            } catch (Exception e) {
                logException("setTrackTimes", e);
            }
        }
    }


    /**
     * Get the useTrackTime property.
     *
     * @return The new value for useTrackTime
     * @deprecated
     */
    public boolean getUseTrackTimes() {
        return useTrackTimes;
    }

    /**
     * Set the TimeDeclutterMinutes property.
     *
     * @param value The new value for TimeDeclutterMinutes
     */
    public void setTimeDeclutterMinutes(double value) {
        timeDeclutterMinutes = value;
    }

    /**
     * Get the TimeDeclutterMinutes property.
     *
     * @return The TimeDeclutterMinutes
     */
    public double getTimeDeclutterMinutes() {
        return timeDeclutterMinutes;
    }


    /**
     * Set the TimeDeclutterEnabled property.
     *
     * @param value The new value for TimeDeclutterEnabled
     */
    public void setTimeDeclutterEnabled(boolean value) {
        timeDeclutterEnabled = value;
        if ((timeDeclutterCbx != null)
                && (value != timeDeclutterCbx.isSelected())) {
            ignoreTimeDeclutterEnabled = true;
            timeDeclutterCbx.setSelected(value);
            ignoreTimeDeclutterEnabled = false;
        }
    }

    /**
     * Get the TimeDeclutterEnabled property.
     *
     * @return The TimeDeclutterEnabled
     */
    public boolean getTimeDeclutterEnabled() {
        return timeDeclutterEnabled;
    }

    /**
     *  Set the AskedUserToDeclutterTime property.
     *
     *  @param value The new value for AskedUserToDeclutterTime
     */
    public void setAskedUserToDeclutterTime(boolean value) {
        askedUserToDeclutterTime = value;
    }

    /**
     *  Get the AskedUserToDeclutterTime property.
     *
     *  @return The AskedUserToDeclutterTime
     */
    public boolean getAskedUserToDeclutterTime() {
        return askedUserToDeclutterTime;
    }

    /**
     * Set the MarkerText property.
     * @param value The new value for MarkerText
     * @deprecated  now use #setMarkerLayout(StationModel)
     */
    public void setMarkerText(String value) {
        markerText    = value;
        markerVisible = true;
    }


    /**
     * Set the MarkerSymbol property.
     * @param value The new value for MarkerSymbol
     * @deprecated  now use #setMarkerLayout(StationModel)
     */
    public void setMarkerSymbol(String value) {
        markerSymbol  = value;
        markerVisible = true;
    }

    /**
     * Set the current station model view.
     *
     * @param model  station model layout
     */
    public void setMarkerLayout(StationModel model) {
        layoutModel = model;
        if (getHaveInitialized() && (indicator != null)) {
            try {
                indicator.setStationModel(layoutModel);
            } catch (Exception excp) {
                logException("setting marker layout", excp);
            }
        }
    }

    /**
     * Get the current layout model view.
     *
     * @return station model layout
     */
    public StationModel getMarkerLayout() {
        if (layoutModel == null) {
            layoutModel = makeLayoutModel();
        }
        return layoutModel;
    }

    /**
     * Get the scale the user can enter
     *
     * @return The scale
     */
    public float getMarkerScale() {
        return markerScale;
    }

    /**
     * Set the scale the user can enter
     *
     * @param f The scale
     */
    public void setMarkerScale(float f) {
        markerScale = f;
        if (indicator != null) {
            try {
                setScaleOnMarker();
            } catch (Exception exc) {
                logException("Setting scale ", exc);
            }
        }
    }

    /**
     *  A utility to set the scale on the marker dislayable
     *
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void setScaleOnMarker() throws RemoteException, VisADException {
        setScaleOnMarker(getDisplayScale() * markerScale);
    }

    /**
     *  A utility to set the scale on the marker dislayable
     *
     * @param f The new scale value
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void setScaleOnMarker(float f)
            throws RemoteException, VisADException {
        if (indicator != null) {
            indicator.setScale(f);
        }
    }


    /**
     * Popup the station model editor
     */
    public void editLayoutModel() {
        getControlContext().getStationModelManager().show(layoutModel);
    }

    /**
     * Set the marker visible
     *
     * @param value  true to be visible
     */
    public void setMarkerVisible(boolean value) {
        markerVisible = value;
        if (getHaveInitialized() && (indicator != null)) {
            try {
                indicator.setVisible(markerVisible);
            } catch (Exception excp) {
                logException("setting marker visible", excp);
            }
        }
    }

    /**
     * Get whether the marker is visible
     *
     * @return  true if visible
     */
    public boolean getMarkerVisible() {
        return markerVisible;
    }

    /**
     * Make the gui widget for setting the layout model
     *
     * @return the widget
     */
    protected JPanel makeLayoutModelWidget() {
        final JButton editButton =
            GuiUtils.getImageButton("/ucar/unidata/idv/images/edit.gif",
                                    getClass());
        editButton.setToolTipText("Show the layout model editor");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                editLayoutModel();
            }
        });
        editButton.setEnabled(markerVisible);

        StationModel marker = getMarkerLayout();

        changeButton = new JButton(getMarkerLayout().getDisplayName());
        changeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StationModelManager smm =
                    getControlContext().getStationModelManager();
                ObjectListener listener = new ObjectListener(null) {
                    public void actionPerformed(ActionEvent ae) {
                        Misc.run(new Runnable() {
                            public void run() {
                                showWaitCursor();
                                try {
                                    setMarkerLayout((StationModel) theObject);
                                    changeButton.setText(
                                        getMarkerLayout().getDisplayName());
                                } catch (Exception exc) {
                                    logException("Changing station model",
                                            exc);
                                }
                                showNormalCursor();
                            }
                        });
                    }
                };

                JPopupMenu popup =
                    GuiUtils.makePopupMenu(
                        StationModelCanvas.makeStationModelMenuItems(
                            smm.getStationModels(), listener, smm));
                popup.show(changeButton, changeButton.getSize().width / 2,
                           changeButton.getSize().height);
            }
        });
        changeButton.setEnabled(markerVisible);


        final ValueSliderWidget vsw = new ValueSliderWidget(this, 0, 50,
                                          "markerScale", "Scale", 10);
        vsw.setSnapToTicks(false);
        vsw.setEnabled(markerVisible);
        final JLabel vswLabel = GuiUtils.rLabel("   Scale: ");
        vswLabel.setEnabled(markerVisible);

        JCheckBox showMarker = new JCheckBox("", markerVisible);
        showMarker.setToolTipText("Show the marker");
        showMarker.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean isVisible = ((JCheckBox) e.getSource()).isSelected();
                setMarkerVisible(isVisible);
                changeButton.setEnabled(isVisible);
                editButton.setEnabled(isVisible);
                vsw.setEnabled(isVisible);
                vswLabel.setEnabled(isVisible);
            }
        });
        JPanel layoutModelPanel = GuiUtils.hflow(Misc.newList(showMarker,
                                      changeButton, editButton), 4, 0);

        JPanel layout = GuiUtils.hbox(layoutModelPanel, vswLabel,
                                      vsw.getContents(false));



        return layout;

    }



}

