/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * <pre>
 * DEPRECATED API - THIS AND ALL SUBPACKAGES WILL BE REPLACED / MOVE:  Scientific feature types package
 * </pre>
 * <p>
 * These are interfaces that define access to specialized "scientific feature" datasets. There are three types of
 * interfaces:
 * <ol>
 * <li>FeatureType: atomic type usually defined by its dimensionality and topology.
 * <li>Collection: a homogeneous collection of atomic types, specifying the nature of the collection.
 * <li>Dataset: one or more Collections of a particular FeatureType, with dataset metadata.
 * </ol>
 * <p>
 * A FeatureDataset and VariableSimpleIF are lightweight abstractions of NetcdfDataset and Variable, which allows
 * implementations that are
 * not necessarily based on NetcdfDataset objects.
 * <h3>See:</h3>
 * <li><a href=
 * "https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/FeatureDatasets/Overview.html">FeatureDatasets
 * Overview</a></li>
 */
package ucar.nc2.ft;
