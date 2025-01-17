/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.internal.cache.FileCacheIF;
import ucar.unidata.geoloc.LatLonRect;
import java.io.IOException;
import java.util.*;

/**
 * Abstract superclass for implementations of FeatureDataset.
 * Subclass must implement getFeatureClass(), and add specific functionality.
 * Also set dataVariables
 * 
 * @author caron
 * @since Sep 7, 2007
 */
public abstract class FeatureDatasetImpl implements FeatureDataset {
  protected NetcdfDataset netcdfDataset;
  protected String title, desc, location;
  protected List<VariableSimpleIF> dataVariables;
  protected Formatter parseInfo = new Formatter();
  protected CalendarDateRange dateRange;
  protected LatLonRect boundingBox;
  protected FileCacheIF fileCache;

  // for subsetting
  protected FeatureDatasetImpl(FeatureDatasetImpl from) {
    this.netcdfDataset = from.netcdfDataset;
    this.title = from.title;
    this.desc = from.desc;
    this.location = from.location;
    this.dataVariables = new ArrayList<>(from.dataVariables);
    this.parseInfo = new Formatter();
    String fromInfo = from.parseInfo.toString().trim();
    if (!fromInfo.isEmpty())
      parseInfo.format("%s%n", fromInfo);
    this.parseInfo.format("Subsetted from original%n");
  }

  /** No-arg constuctor */
  public FeatureDatasetImpl() {}

  /**
   * Constructor when theres no NetcdfFile underneath.
   *
   * @param title title of the dataset.
   * @param description description of the dataset.
   * @param location URI of the dataset
   */
  public FeatureDatasetImpl(String title, String description, String location) {
    this.title = title;
    this.desc = description;
    this.location = location;
  }

  /**
   * Constructor when theres a NetcdfFile underneath
   * 
   * @param netcdfDataset adapt this NetcdfDataset
   */
  public FeatureDatasetImpl(NetcdfDataset netcdfDataset) {
    this.netcdfDataset = netcdfDataset;
    this.location = netcdfDataset.getLocation();

    this.title = netcdfDataset.getTitle();
    if (title == null)
      title = netcdfDataset.getRootGroup().findAttributeString("title", null);
    if (desc == null)
      desc = netcdfDataset.getRootGroup().findAttributeString("description", null);
  }

  protected void setTitle(String title) {
    this.title = title;
  }

  protected void setDescription(String desc) {
    this.desc = desc;
  }

  protected void setLocationURI(String location) {
    this.location = location;
  }

  public void setDateRange(CalendarDateRange dateRange) {
    this.dateRange = dateRange;
  }

  public void setBoundingBox(LatLonRect boundingBox) {
    this.boundingBox = boundingBox;
  }

  /*
   * protected void removeDataVariable( String varName) {
   * if (dataVariables == null) return;
   * Iterator iter = dataVariables.iterator();
   * while (iter.hasNext()) {
   * VariableSimpleIF v = (VariableSimpleIF) iter.next();
   * if (v.getName().equals( varName) )
   * iter.remove();
   * }
   * }
   */

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() {
    return netcdfDataset;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return desc;
  }

  public String getLocation() {
    return location;
  }

  @Override
  public AttributeContainer attributes() {
    return netcdfDataset != null ? netcdfDataset.getRootGroup().attributes()
        : new AttributeContainerMutable(title).toImmutable();
  }

  public void getDetailInfo(java.util.Formatter sf) {
    sf.format("FeatureDataset on location= %s%n", getLocation());
    sf.format("  featureType= %s%n", getFeatureType());
    sf.format("  title= %s%n", getTitle());
    sf.format("  desc= %s%n", getDescription());
    sf.format("  range= %s%n", getCalendarDateRange());
    sf.format("  start= %s%n", getCalendarDateEnd());
    sf.format("  end  = %s%n", getCalendarDateEnd());
    LatLonRect bb = getBoundingBox();
    sf.format("  bb   = %s%n", bb);
    if (bb != null)
      sf.format("  bb   = %s%n", getBoundingBox().toString2());

    sf.format("  has netcdf = %b%n", (getNetcdfFile() != null));
    if (!attributes().isEmpty()) {
      sf.format("  Attributes%n");
      for (Attribute a : attributes())
        sf.format("    %s%n", a);
    }

    List<VariableSimpleIF> vars = getDataVariables();
    sf.format("%n  Data Variables (%d)%n", vars.size());
    for (VariableSimpleIF v : vars) {
      sf.format("    name='%s' desc='%s' units=%s' type='%s' dims=(", v.getShortName(), v.getDescription(),
          v.getUnitsString(), v.getDataType());
      for (Dimension d : v.getDimensions())
        sf.format("%s ", d);
      sf.format(")%n");
    }

    if (!parseInfo.toString().isEmpty())
      sf.format("%nparseInfo=%n%s%n", parseInfo);
  }

  @Override
  public CalendarDateRange getCalendarDateRange() {
    return dateRange;
  }

  @Override
  public CalendarDate getCalendarDateStart() {
    return (dateRange == null) ? null : dateRange.getStart();
  }

  @Override
  public CalendarDate getCalendarDateEnd() {
    return (dateRange == null) ? null : dateRange.getEnd();
  }

  public LatLonRect getBoundingBox() {
    return boundingBox;
  }

  public List<VariableSimpleIF> getDataVariables() {
    return (dataVariables == null) ? new ArrayList<>() : dataVariables;
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    for (VariableSimpleIF s : getDataVariables()) {
      String ss = s.getShortName();
      if (shortName.equals(ss))
        return s;
    }
    return null;
  }

  public String getImplementationName() {
    return getClass().getName();
  }

  //////////////////////////////////////////////////
  // FileCacheable

  @Override
  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      if (fileCache.release(this))
        return;
    }

    try {
      if (netcdfDataset != null)
        netcdfDataset.close();
    } finally {
      netcdfDataset = null;
    }
  }

  /** @deprecated do not use */
  @Deprecated
  public void release() throws IOException {
    if (netcdfDataset != null)
      netcdfDataset.release();
  }

  /** @deprecated do not use */
  @Deprecated
  public void reacquire() throws IOException {
    if (netcdfDataset != null)
      netcdfDataset.reacquire();
  }

  @Override
  public long getLastModified() {
    return (netcdfDataset != null) ? netcdfDataset.getLastModified() : 0;
  }

  /** @deprecated do not use */
  @Deprecated
  @Override
  public synchronized void setFileCache(FileCacheIF fileCache) {
    this.fileCache = fileCache;
  }

}
