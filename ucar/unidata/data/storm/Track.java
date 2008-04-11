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


import visad.georef.EarthLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 5:00:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class Track {

    /** _more_          */
    private String trackID;

    /** _more_          */
    private StormInfo stormInfo;

    /** _more_          */
    private Way way;

    /** _more_          */
    private List<EarthLocation[]> trackPoints;

    /** _more_          */
    private List trackTimes;

    /** _more_          */
    private List attributes;

    private Date trackStartTime;

    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param pts _more_
     * @param times _more_
     * @param attrs _more_
     */
    public Track(StormInfo stormInfo, Date sTime, Way way, List pts, List times,
                 List attrs) {

        this.stormInfo   = stormInfo;
        this.trackStartTime = sTime;
        this.way         = way;
        this.trackPoints = new ArrayList(pts);
        this.trackTimes  = new ArrayList(times);
        if (attrs != null) {
            this.attributes = new ArrayList(attrs);
        }
        this.trackID = stormInfo.toString() + "_" + way;
    }
    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return trackID.hashCode();
    }

    /**
     * _more_
     *
     * @param id _more_
     */
    public void setTrackID(String id) {
        this.trackID = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTrackID() {
        return trackID;
    }

    /**
     * _more_
     *
     * @param time _more_
     */
    public void setTrackStartTime(Date time) {
        this.trackStartTime = time;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Date getTrackStartTime() {
        return trackStartTime;
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
    public StormInfo getStormID() {
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
    public void setTrackPoints(List pts) {
        this.trackPoints = new ArrayList(pts);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTrackPoints() {
        return trackPoints;
    }

    /**
     * _more_
     *
     * @param pts _more_
     */
    public void setTrackTimes(List pts) {
        this.trackTimes = new ArrayList(pts);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTrackTimes() {
        return trackTimes;
    }

    /**
     * _more_
     *
     * @param attrs _more_
     */
    public void setTrackAttributes(List attrs) {
        this.attributes = new ArrayList(attrs);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTrackAttributes() {
        return attributes;
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
        if ( !(o instanceof Track)) {
            return false;
        }
        Track other = (Track) o;
        return ((stormInfo == other.stormInfo) && (way == other.way) && (trackStartTime == other.trackStartTime));
    }
}

