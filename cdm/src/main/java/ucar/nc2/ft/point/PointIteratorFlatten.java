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
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Adapt a PointFeatureCollectionIterator to a PointFeatureIterator, by flattening all the iterators in the collection
 * into a single iterator over PointFeatures. Optionally add date and space filters.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public class PointIteratorFlatten extends PointIteratorAbstract {
  private IOIterator<PointFeatureCollection> collectionIter;
  private Filter filter = null;

  private PointFeatureCollection currCollection;
  private PointFeatureIterator pfiter; // iterator over the current PointFeatureCollection
  private PointFeature pointFeature; // current PointFeature in the current PointFeatureCollection
  private boolean finished = false;

  /**
   * Constructor.
   *
   * @param collectionIter iterator over the collections
   * @param filter_bb      boundingbox, or null
   * @param filter_date    data range, or null
   */
  PointIteratorFlatten(IOIterator<PointFeatureCollection> collectionIter, LatLonRect filter_bb, CalendarDateRange filter_date) {
    this.collectionIter = collectionIter;
    if ((filter_bb != null) || (filter_date != null))
      this.filter = new PointIteratorFiltered.SpaceAndTimeFilter(filter_bb, filter_date);
  }

  @Override
  public void close() {
    if (finished) return;
    if (pfiter != null) pfiter.close();
    // collectionIter.close();
    finishCalcBounds();
    finished = true;
  }

  @Override
  public boolean hasNext() {
    try {
      pointFeature = nextFilteredDataPoint();
      if (pointFeature != null) return true;

      PointFeatureCollection feature = nextCollection();
      if (feature == null) {
        close();
        return false;
      }

      currCollection = feature;
      pfiter = feature.getPointFeatureIterator();
      return hasNext();

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public PointFeature next() {
    if (pointFeature == null) return null;
    calcBounds(pointFeature);
    return pointFeature;
  }

  private PointFeatureCollection nextCollection() throws IOException {
    if (!collectionIter.hasNext()) return null;
    return collectionIter.next();
  }

  private PointFeature nextFilteredDataPoint() throws IOException {
    if (pfiter == null) return null;
    if (!pfiter.hasNext()) return null;
    PointFeature pdata = pfiter.next();

    if (filter == null)
      return pdata;

    while (!filter.filter(pdata)) {
      if (!pfiter.hasNext()) return null;
      pdata = pfiter.next();
    }
    return pdata;
  }

}
