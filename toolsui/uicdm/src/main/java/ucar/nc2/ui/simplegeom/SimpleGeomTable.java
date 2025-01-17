/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.simplegeom;

import ucar.nc2.*;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.dataset.*;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.ui.widget.*;
import ucar.ui.widget.PopupMenu;
import ucar.unidata.geoloc.Projection;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.IOException;
import javax.swing.*;

/**
 * A Swing widget to examine a SimpleGeom.
 *
 * @author skaymen
 */
public class SimpleGeomTable extends JPanel {
  private final PreferencesExt prefs;
  private GridDataset gridDataset;

  private final BeanTable<GeoGridBean> varTable;
  private BeanTable<GeoCoordinateSystemBean> csTable;
  private BeanTable<GeoAxisBean> axisTable;
  private BeanTable<SimpleGeomBean> simpleGeomTable;
  private JSplitPane split;
  private JSplitPane split2;
  private final TextHistoryPane infoTA;
  private final IndependentWindow infoWindow;
  private NetcdfOutputChooser outChooser;

  public SimpleGeomTable(PreferencesExt prefs, boolean showCS) {
    this.prefs = prefs;

    varTable = new BeanTable<>(GeoGridBean.class, (PreferencesExt) prefs.node("GeogridBeans"), false);
    JTable jtable = varTable.getJTable();

    PopupMenu csPopup = new ucar.ui.widget.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GeoGridBean vb = varTable.getSelectedBean();
        if (vb != null) {
          Variable v = vb.geogrid.getVariable();
          infoTA.clear();
          if (v == null)
            infoTA.appendLine("Cant find variable " + vb.getName() + " escaped= ("
                + NetcdfFiles.makeValidPathName(vb.getName()) + ")");
          else {
            infoTA.appendLine("Variable " + v.getFullName() + " :");
            infoTA.appendLine(v.toString());
          }
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    csPopup.addAction("Show Coordinates", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GeoGridBean vb = varTable.getSelectedBean();
        if (vb != null) {
          Formatter f = new Formatter();
          showCoordinates(vb, f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // optionally show coordinate systems and axis
    Component comp = varTable;
    if (showCS) {
      csTable =
          new BeanTable<>(GeoCoordinateSystemBean.class, (PreferencesExt) prefs.node("GeoCoordinateSystemBean"), false);
      axisTable = new BeanTable<>(GeoAxisBean.class, (PreferencesExt) prefs.node("GeoCoordinateAxisBean"), false);
      simpleGeomTable = new BeanTable<>(SimpleGeomBean.class, (PreferencesExt) prefs.node("SimpleGeomBean"), false);

      split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, csTable);
      split.setDividerLocation(prefs.getInt("splitPos", 500));

      split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, axisTable);
      split2.setDividerLocation(prefs.getInt("splitPos2", 500));

      JSplitPane split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, simpleGeomTable);
      split3.setDividerLocation(prefs.getInt("splitPos3", 500));

      comp = split3;
    }

    setLayout(new BorderLayout());
    add(comp, BorderLayout.CENTER);
  }

  public void addExtra(JPanel buttPanel, FileManager fileChooser) {

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
    infoButton.addActionListener(e -> {
      if ((gridDataset != null) && (gridDataset instanceof ucar.nc2.dt.grid.GridDataset)) {
        ucar.nc2.dt.grid.GridDataset gdsImpl = (ucar.nc2.dt.grid.GridDataset) gridDataset;
        infoTA.clear();
        infoTA.appendLine(gdsImpl.getDetailInfo());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    buttPanel.add(infoButton);

    AbstractAction netcdfAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (gridDataset == null)
          return;
        List<String> gridList = getSelectedGrids();
        if (gridList.isEmpty()) {
          JOptionPane.showMessageDialog(SimpleGeomTable.this, "No Simple Geometries are selected");
          return;
        }

        if (outChooser == null) {
          outChooser = new NetcdfOutputChooser((Frame) null);
          outChooser.addPropertyChangeListener("OK", evt -> {
            // writeNetcdf((NetcdfOutputChooser.Data) evt.getNewValue());
          });
        }
        outChooser.setOutputFilename(gridDataset.getLocation());
        outChooser.setVisible(true);
      }
    };
    BAMutil.setActionProperties(netcdfAction, "nj22/Netcdf", "Write netCDF-CF file", false, 'S', -1);
    BAMutil.addActionToContainer(buttPanel, netcdfAction);
  }

  private void showCoordinates(GeoGridBean vb, Formatter f) {
    GridCoordSystem gcs = vb.geogrid.getCoordinateSystem();
    gcs.show(f, true);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    varTable.saveState(false);
    csTable.saveState(false);
    axisTable.saveState(false);
    simpleGeomTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null)
      prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null)
      prefs.putInt("splitPos2", split2.getDividerLocation());
    if (csTable != null)
      csTable.saveState(false);
    if (axisTable != null)
      axisTable.saveState(false);
  }

  public void clear() {
    this.gridDataset = null;
    varTable.clearBeans();
    csTable.clearBeans();
    axisTable.clearBeans();
  }

  public void setDataset(NetcdfDataset ds, Formatter parseInfo) throws IOException {
    this.gridDataset = new ucar.nc2.dt.grid.GridDataset(ds, parseInfo);

    List<GeoGridBean> beanList = new ArrayList<>();
    java.util.List<GridDatatype> list = gridDataset.getGrids();
    for (GridDatatype g : list)
      beanList.add(new GeoGridBean(g));
    varTable.setBeans(beanList);

    List<SimpleGeomBean> sgList = new ArrayList<>();
    java.util.List<GridDatatype> list2 = gridDataset.getGrids();
    for (GridDatatype g : list2)
      sgList.add(new SimpleGeomBean(g));
    simpleGeomTable.setBeans(sgList);

    if (csTable != null) {
      List<GeoCoordinateSystemBean> csList = new ArrayList<>();
      List<GeoAxisBean> axisList;
      axisList = new ArrayList<>();
      for (GridDataset.Gridset gset : gridDataset.getGridsets()) {
        csList.add(new GeoCoordinateSystemBean(gset));
        GridCoordSystem gsys = gset.getGeoCoordSystem();
        List<CoordinateAxis> axes = gsys.getCoordinateAxes();
        for (CoordinateAxis axis : axes) {
          GeoAxisBean axisBean = new GeoAxisBean(axis);
          if (!contains(axisList, axisBean.getName())) {
            axisList.add(axisBean);
          }
        }
      }
      csTable.setBeans(csList);
      axisTable.setBeans(axisList);
    }
  }

  public void setDataset(GridDataset gds) {
    this.gridDataset = gds;

    List<GeoGridBean> beanList = new ArrayList<>();
    java.util.List<GridDatatype> list = gridDataset.getGrids();
    for (GridDatatype g : list)
      beanList.add(new GeoGridBean(g));
    varTable.setBeans(beanList);

    if (csTable != null) {
      List<GeoCoordinateSystemBean> csList = new ArrayList<>();
      List<GeoAxisBean> axisList;
      axisList = new ArrayList<>();
      for (GridDataset.Gridset gset : gridDataset.getGridsets()) {
        csList.add(new GeoCoordinateSystemBean(gset));
        GridCoordSystem gsys = gset.getGeoCoordSystem();
        List<CoordinateAxis> axes = gsys.getCoordinateAxes();
        for (CoordinateAxis axis : axes) {
          GeoAxisBean axisBean = new GeoAxisBean(axis);
          if (!contains(axisList, axisBean.getName())) {
            axisList.add(axisBean);
          }
        }
      }
      csTable.setBeans(csList);
      axisTable.setBeans(axisList);
    }
  }

  private boolean contains(List<GeoAxisBean> axisList, String name) {
    for (GeoAxisBean axis : axisList)
      if (axis.getName().equals(name))
        return true;
    return false;
  }

  public GridDataset getGridDataset() {
    return gridDataset;
  }

  public List<String> getSelectedGrids() {
    List<GeoGridBean> grids = varTable.getSelectedBeans();
    List<String> result = new ArrayList<>();
    for (Object bean : grids) {
      GeoGridBean gbean = (GeoGridBean) bean;
      result.add(gbean.getName());
    }
    return result;
  }


  public GridDatatype getGrid() {
    GeoGridBean vb = varTable.getSelectedBean();
    if (vb == null) {
      List<GridDatatype> grids = gridDataset.getGrids();
      if (!grids.isEmpty())
        return grids.get(0);
      else
        return null;
    }
    return gridDataset.findGridDatatype(vb.getName());
  }

  public static class GeoGridBean {
    GridDatatype geogrid;
    String name, desc, units, csys;
    String dims, x, y, z, t, ens, rt;

    // no-arg constructor
    public GeoGridBean() {}

    // create from a dataset
    public GeoGridBean(GridDatatype geogrid) {
      this.geogrid = geogrid;
      setName(geogrid.getFullName());
      setDescription(geogrid.getDescription());
      setUnits(geogrid.getUnitsString());

      GridCoordSystem gcs = geogrid.getCoordinateSystem();
      setCoordSystem(gcs.getName());

      // collect dimensions
      StringBuilder buff = new StringBuilder();
      java.util.List<Dimension> dims = geogrid.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        ucar.nc2.Dimension dim = dims.get(j);
        if (j > 0)
          buff.append(",");
        buff.append(dim.getLength());
      }
      setShape(buff.toString());

      x = getAxisName(gcs.getXHorizAxis());
      y = getAxisName(gcs.getYHorizAxis());
      z = getAxisName(gcs.getVerticalAxis());
      t = getAxisName(gcs.getTimeAxis());
      rt = getAxisName(gcs.getRunTimeAxis());
      ens = getAxisName(gcs.getEnsembleAxis());
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return desc;
    }

    public void setDescription(String desc) {
      this.desc = desc;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = units;
    }

    public String getCoordSystem() {
      return csys;
    }

    public void setCoordSystem(String csys) {
      this.csys = csys;
    }

    public String getX() {
      return x;
    }

    public String getY() {
      return y;
    }

    public String getZ() {
      return z;
    }

    public String getT() {
      return t;
    }

    public String getEns() {
      return ens;
    }

    public String getRt() {
      return rt;
    }

    public String getShape() {
      return dims;
    }

    public void setShape(String dims) {
      this.dims = dims;
    }

    private String getAxisName(CoordinateAxis axis) {
      if (axis != null)
        return (axis.isCoordinateVariable()) ? axis.getShortName() : axis.getNameAndDimensions();
      return "";
    }
  }

  public static class GeoCoordinateSystemBean {
    private GridCoordSystem gcs;
    private String proj, coordTrans;
    private int ngrids = -1;

    // no-arg constructor
    public GeoCoordinateSystemBean() {}

    public GeoCoordinateSystemBean(GridDataset.Gridset gset) {
      gcs = gset.getGeoCoordSystem();

      setNGrids(gset.getGrids().size());

      Projection proj = gcs.getProjection();
      if (proj != null)
        setProjection(proj.getClassName());

      int count = 0;
      StringBuilder buff = new StringBuilder();
      for (CoordinateTransform ct : gcs.getCoordinateTransforms()) {
        if (count > 0) {
          buff.append("; ");
        }
        if (ct instanceof VerticalCT) {
          buff.append(((VerticalCT) ct).getVerticalTransformType());
          count++;
        }
        if (ct instanceof ProjectionCT) {
          ProjectionCT pct = (ProjectionCT) ct;
          if (pct.getProjection() == null) { // only show CT if theres no projection
            buff.append("-").append(pct.getName());
            count++;
          }
        }
      }
      setCoordTransforms(buff.toString());
    }

    public String getName() {
      return gcs.getName();
    }

    public boolean isRegularSpatial() {
      return gcs.isRegularSpatial();
    }

    public boolean isLatLon() {
      return gcs.isLatLon();
    }

    public boolean isGeoXY() {
      return ((GridCoordSys) gcs).isGeoXY();
    }

    public int getDomainRank() {
      return gcs.getDomain().size();
    }

    public int getRangeRank() {
      return gcs.getCoordinateAxes().size();
    }

    public int getNGrids() {
      return ngrids;
    }

    public void setNGrids(int ngrids) {
      this.ngrids = ngrids;
    }

    public String getProjection() {
      return proj;
    }

    public void setProjection(String proj) {
      this.proj = proj;
    }

    public String getCoordTransforms() {
      return coordTrans;
    }

    public void setCoordTransforms(String coordTrans) {
      this.coordTrans = coordTrans;
    }
  }

  public static class GeoAxisBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateAxis axis;
    CoordinateSystem firstCoordSys;
    String name, desc, units, axisType = "", positive = "", incr = "";
    String dims, shape, csNames;
    boolean isCoordVar;

    // no-arg constructor
    public GeoAxisBean() {}

    // create from a dataset
    public GeoAxisBean(CoordinateAxis v) {
      this.axis = v;

      setName(v.getFullName());
      setCoordVar(v.isCoordinateVariable());
      setDescription(v.getDescription());
      setUnits(v.getUnitsString());

      // collect dimensions
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      java.util.List<Dimension> dims = v.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        ucar.nc2.Dimension dim = dims.get(j);
        if (j > 0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getShortName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDims(names.toString());
      setShape(lens.toString());

      AxisType at = v.getAxisType();
      if (at != null)
        setAxisType(at.toString());
      String p = v.getPositive();
      if (p != null)
        setPositive(p);

      if (v instanceof CoordinateAxis1D) {
        CoordinateAxis1D v1 = (CoordinateAxis1D) v;
        if (v1.isRegular())
          setRegular(Double.toString(v1.getIncrement()));
      }
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isCoordVar() {
      return isCoordVar;
    }

    public void setCoordVar(boolean isCoordVar) {
      this.isCoordVar = isCoordVar;
    }

    public String getShape() {
      return shape;
    }

    public void setShape(String shape) {
      this.shape = shape;
    }

    public String getAxisType() {
      return axisType;
    }

    public void setAxisType(String axisType) {
      this.axisType = axisType;
    }

    public String getDims() {
      return dims;
    }

    public void setDims(String dims) {
      this.dims = dims;
    }

    public String getDescription() {
      return desc;
    }

    public void setDescription(String desc) {
      this.desc = desc;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = (units == null) ? "null" : units;
    }

    public String getPositive() {
      return positive;
    }

    public void setPositive(String positive) {
      this.positive = positive;
    }

    public String getRegular() {
      return incr;
    }

    public void setRegular(String incr) {
      this.incr = incr;
    }
  }

  public static class SimpleGeomBean {
    // static public String editableProperties() { return "title include logging freq"; }

    GridDatatype v;
    String line, polygon, geoX, geoY;


    // no-arg constructor
    public SimpleGeomBean() {}

    public String getName() {
      // TODO Auto-generated method stub
      return "";
    }

    // create from a dataset
    public SimpleGeomBean(GridDatatype v) {
      this.v = v;

      setLine("");
      setPolygon("");
      setGeoX("");
      setGeoY("");
    }


    public String getLine() {
      return line;
    }

    public void setLine(String line) {
      this.line = line;
    }

    public String getPolygon() {
      return polygon;
    }

    public void setPolygon(String polygon) {
      this.polygon = polygon;
    }

    public String getGeoX() {
      return geoX;
    }

    public void setGeoX(String geoX) {
      this.geoX = geoX;
    }

    public String getGeoY() {
      return geoY;
    }

    public void setGeoY(String geoY) {
      this.geoY = geoY;
    }
  }



  /**
   * Wrap this in a JDialog component.
   *
   * @param parent JFrame (application) or JApplet (applet) or null
   * @param title dialog window title
   * @param modal modal dialog or not
   * @return JDialog
   */
  public JDialog makeDialog(RootPaneContainer parent, String title, boolean modal) {
    return new Dialog(parent, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof Frame ? (Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener(e -> {
        if (e.getPropertyName().equals("lookAndFeel"))
          SwingUtilities.updateComponentTreeUI(Dialog.this);
      });

      // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add(SimpleGeomTable.this, BorderLayout.CENTER);
      pack();
    }
  }
}
