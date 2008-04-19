/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.storm;


import visad.*;
import visad.georef.EarthLocation;
import java.util.ArrayList;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 5:00:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormTrack {

    /** _more_ */
    private String trackId;

    /** _more_ */
    private StormInfo stormInfo;

    /** _more_ */
    private Way way;

    /** _more_ */
    private List<StormTrackPoint> trackPoints;

    //private Date trackStartTime;



    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param pts _more_
     */
    public StormTrack(StormInfo stormInfo, Way way, List<StormTrackPoint> pts) {

        this.stormInfo   = stormInfo;
        this.way         = way;
        this.trackPoints = new ArrayList(pts);
        StormTrackPoint firstPoint = (StormTrackPoint)pts.get(0);
        DateTime trackStartTime = firstPoint.getTrackPointTime();
        this.trackId = stormInfo.toString() + "_" + way + "_"
                       + trackStartTime.getValue();
    }


    public boolean isObservation() {
        return way.isObservation();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return trackId.hashCode();
    }

    /**
     * _more_
     *
     * @param id _more_
     */
    public void setTrackId(String id) {
        this.trackId = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTrackId() {
        return trackId;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getTrackStartTime() {
        StormTrackPoint firstPoint = trackPoints.get(0);
        return firstPoint.getTrackPointTime();

    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     */
    public void setStormInfo(StormInfo stormInfo) {
        this.stormInfo = stormInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public StormInfo getStormInfo() {
        return stormInfo;
    }

    /**
     * _more_
     *
     * @param way _more_
     */
    public void setWay(Way way) {
        this.way = way;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Way getWay() {
        return way;
    }

    /**
     * _more_
     *
     * @param pts _more_
     */
    public void setTrackPoints(List<StormTrackPoint> pts) {
        this.trackPoints = new ArrayList<StormTrackPoint>(pts);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormTrackPoint> getTrackPoints() {
        return trackPoints;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<DateTime> getTrackTimes() {
        List<DateTime> trackTimes = new ArrayList();
        for(StormTrackPoint stp: trackPoints){
            trackTimes.add(stp.getTrackPointTime());
        }
        return trackTimes;
    }


    public String toString() {
        return trackId;
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if ( !(o instanceof StormTrack)) {
            return false;
        }
        StormTrack other = (StormTrack) o;
        return ((trackId.equals(other.trackId)));
    }
}

