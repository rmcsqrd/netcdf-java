/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;

/**
 * Create a RotatedPole Projection from the information in the Coordinate Transform Variable.
 * This is from CF. Grib is RotatedLatLon
 */
public class RotatedPole extends AbstractProjectionCT implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.ROTATED_LATITUDE_LONGITUDE;
  }

  public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon = ctv.findAttributeDouble(CF.GRID_NORTH_POLE_LONGITUDE, Double.NaN);
    double lat = ctv.findAttributeDouble(CF.GRID_NORTH_POLE_LATITUDE, Double.NaN);

    ucar.unidata.geoloc.projection.RotatedPole proj = new ucar.unidata.geoloc.projection.RotatedPole(lat, lon);
    return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
  }

}
