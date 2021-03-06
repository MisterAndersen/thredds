/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;

/**
 * Abstract superclass for implementations of StationProfileFeature.
 * Subclass must implement getPointFeatureCollectionIterator();
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class StationProfileFeatureImpl extends PointFeatureCCImpl implements StationProfileFeature {
  protected int timeSeriesNpts;
  protected Station station;
  protected PointFeatureCollectionIterator localIterator;

  public StationProfileFeatureImpl(String name, String desc, String wmoId, double lat, double lon, double alt, CalendarDateUnit timeUnit, String altUnits, int npts) {
    super( name, timeUnit, altUnits, FeatureType.STATION_PROFILE);
    station = new StationImpl(name, desc, wmoId, lat, lon, alt, npts);
    this.timeSeriesNpts = npts;
  }

  public StationProfileFeatureImpl(Station s, CalendarDateUnit timeUnit, String altUnits, int npts) {
    super( s.getName(), timeUnit, altUnits, FeatureType.STATION_PROFILE);
    this.station = s;
    this.timeSeriesNpts = npts;
  }

  @Override
  public int getNobs() {
    return this.timeSeriesNpts;
  }

  @Override
  public String getWmoId() {
    return station.getWmoId();
  }

  @Override
  public int size() {
    return timeSeriesNpts;
  }

  @Nonnull
  @Override
  public String getName() {
    return station.getName();
  }

  @Override
  public String getDescription() {
    return station.getDescription();
  }

  @Override
  public double getLatitude() {
    return station.getLatitude();
  }

  @Override
  public double getLongitude() {
    return station.getLongitude();
  }

  @Override
  public double getAltitude() {
    return station.getAltitude();
  }

  @Override
  public LatLonPoint getLatLon() {
    return station.getLatLon();
  }
  
  @Override
  public boolean isMissing() {
    return Double.isNaN(getLatitude()) || Double.isNaN(getLongitude());
  }

  @Override
  public int compareTo(@Nonnull Station so) {
    return station.getName().compareTo(so.getName());
  }

  // @Override
  public StationProfileFeature subset(LatLonRect boundingBox) throws IOException {
    return this; // only one station - we could check if its in the bb
  }

  @Override
  public StationProfileFeature subset(CalendarDateRange dateRange) throws IOException {
    return new StationProfileFeatureSubset(this, dateRange);
  }

  public static class StationProfileFeatureSubset extends StationProfileFeatureImpl {
    private final StationProfileFeature from;
    private final CalendarDateRange dateRange;

    public StationProfileFeatureSubset(StationProfileFeatureImpl from, CalendarDateRange filter_date) {
      super(from.station, from.getTimeUnit(), from.getAltUnits(), -1);
      this.from = from;
      this.dateRange = filter_date;
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return from.getFeatureData();
    }

    @Override
    public List<CalendarDate> getTimes() throws IOException {
      List<CalendarDate> result = new ArrayList<>();
      for (ProfileFeature pf : this) {
        if (dateRange.includes(pf.getTime()))
          result.add(pf.getTime());
      }
      return result;
    }

    @Override
    public ProfileFeature getProfileByDate(CalendarDate date) throws IOException {
      return from.getProfileByDate(date);
    }

    @Override // new way
    public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
      return new PointCollectionIteratorFiltered( from.getPointFeatureCollectionIterator(), new DateFilter());
    }

    @Override // old way
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
      return new PointCollectionIteratorFiltered( from.getPointFeatureCollectionIterator(), new DateFilter());
    }

    private class DateFilter implements PointFeatureCollectionIterator.Filter {
      @Override
      public boolean filter(PointFeatureCollection pointFeatureCollection) {
        ProfileFeature profileFeature = (ProfileFeature) pointFeatureCollection;
        return dateRange.includes(profileFeature.getTime());
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<ProfileFeature> iterator() {
    return new ProfileFeatureIterator();
  }

  private class ProfileFeatureIterator implements Iterator<ProfileFeature> {
    PointFeatureCollectionIterator pfIterator;

    public ProfileFeatureIterator() {
      try {
        this.pfIterator = getPointFeatureCollectionIterator();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean hasNext() {
      try {
        return pfIterator.hasNext();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ProfileFeature next() {
      try {
        return (ProfileFeature) pfIterator.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }


  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated


  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public ProfileFeature next() throws IOException {
    return (ProfileFeature) localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getPointFeatureCollectionIterator();
  }

}
