/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

/**
 * The way that the Axis depends on other axes.
 */
public enum GridAxisDependenceType {
  // If making changes, update ucar.gcdm.GcdmGridConverter#convertAxisDependenceType(GridAxis.DependenceType)
  // and consider if need for addition to gcdm_grid.proto.
  /**
   * Has its own dimension, so is a coordinate variable, eg x(x).
   */
  independent,
  /**
   * Auxilary coordinate, eg reftime(time) or time_bounds(time).
   */
  dependent,
  /**
   * A scalar doesnt involve indices. Eg the reference time is often a scalar.
   */
  scalar,
  /**
   * A coordinate needing two dimensions, eg lat(x,y).
   */
  twoD,
  /**
   * Eg time(reftime, hourOfDay).
   */
  fmrcReg,
  /**
   * Eg swath(scan, scanAcross).
   */
  dimension
}
