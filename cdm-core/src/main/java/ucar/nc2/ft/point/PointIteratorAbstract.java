/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import java.util.Iterator;

/**
 * Abstract superclass for PointFeatureIterator.
 * Mostly implements the bounds calculations.
 *
 * @author caron
 * @since May 11, 2009
 */
public abstract class PointIteratorAbstract implements PointFeatureIterator, Iterator<PointFeature> {
  protected boolean calcBounds;
  protected CollectionInfo info;

  private LatLonRect.Builder bb;
  private double minTime = Double.MAX_VALUE;
  private double maxTime = -Double.MAX_VALUE;
  private int count;

  protected PointIteratorAbstract() {}

  public void setCalculateBounds(CollectionInfo info) {
    this.calcBounds = (info != null);
    this.info = info;
  }

  protected void calcBounds(PointFeature pf) {
    count++;
    if (!calcBounds)
      return;
    if (pf == null)
      return;

    if (bb == null)
      bb = new LatLonRect.Builder(pf.getLocation().getLatLon(), .001, .001);
    else
      bb.extend(pf.getLocation().getLatLon());

    double obsTime = pf.getObservationTime();
    minTime = Math.min(minTime, obsTime);
    maxTime = Math.max(maxTime, obsTime);
  }

  protected void finishCalcBounds() {
    if (!calcBounds || bb == null) {
      return;
    }

    LatLonRect llrect = bb.build();

    if (llrect.crossDateline() && (llrect.getWidth() > 350.0)) { // call it global - less confusing
      double lat_min = llrect.getLowerLeftPoint().getLatitude();
      double deltaLat = llrect.getUpperLeftPoint().getLatitude() - lat_min;
      llrect = new LatLonRect.Builder(LatLonPoint.create(lat_min, -180.0), deltaLat, 360.0).build();
    }

    info.bbox = llrect;
    info.minTime = minTime;
    info.maxTime = maxTime;

    info.nobs = count;
    info.nfeatures = count;
    info.setComplete();
  }

}
