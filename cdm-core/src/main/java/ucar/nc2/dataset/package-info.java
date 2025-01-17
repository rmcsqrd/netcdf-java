/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * An extension to the netCDF API which provides support for coordinate systems, scale/offset/missing data, and NcML.
 * <ul>
 * <li><i><b>Coordinate systems</b></i>
 * allow the specification and recognition of general and georeferencing coordinate systems within
 * a netCDF dataset.
 * <li><i><b>Scale/offset/missing data</b></i> handles the conversion of integer data to floating point, and the
 * interaction
 * with missing data values
 * <li><i><b>NcML</b></i>, the NetCDF Markup Language, is an XML
 * schema for showing, adding and changing netCDF metadata in XML. <i><b>NcML dataset</b></i> allows
 * you to create virtual netCDF datasets, including combining multiple netCDF files into one dataset.
 * </ul>
 */
package ucar.nc2.dataset;
