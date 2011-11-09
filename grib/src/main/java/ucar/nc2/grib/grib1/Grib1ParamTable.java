/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib1;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.GribTables;
import ucar.unidata.util.StringUtil2;

import java.io.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage Grib-1 parameter tables (table 2).
 */
public class Grib1ParamTable implements GribTables {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1ParamTable.class);

  static private Object lock = new Object();
  static private int standardTablesStart = 0; // heres where the standard tables start - keep track to user additions can go first

  static private boolean debug = false, warn = false;

  static private Lookup standardLookup;
  static private Grib1ParamTable defaultTable;

  // This is a mapping from (center,subcenter,version)-> Param table for any data that has been loaded
  //static private Map<Integer, Grib1ParamTable> tableMap = new ConcurrentHashMap<Integer, Grib1ParamTable>();

  // a list of all the tables
  //static private List<Grib1ParamTable> paramTables;

  static {
    try {
      standardLookup = new Lookup();
      standardLookup.readLookupTable("resources/grib1/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ecmwf/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ncl/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/dss/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ncep/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/wrf/lookupTables.txt"); // */
      // lookup.readLookupTable("resources/grib1/tablesOld/lookupTables.txt");  // too many problems - must check every one !
      standardLookup.tables = new CopyOnWriteArrayList<Grib1ParamTable>(standardLookup.tables); // in case user adds tables
      defaultTable = getParameterTable(0, -1, -1);

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static boolean strict = false;

  public static boolean isStrict() {
    return strict;
  }

  public static void setStrict(boolean strict) {
    Grib1ParamTable.strict = strict;
  }

  public static Grib1ParamTable getDefaultTable() {
    return defaultTable;
  }

  // debugging
  public static List<Grib1ParamTable> getStandardParameterTables() {
    return standardLookup.tables;
  }

  public static Grib1Parameter getParameter(Grib1Record record) {
    Grib1SectionProductDefinition pds = record.getPDSsection();
    return getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
  }

  public static Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
    return standardLookup.getParameter(center, subcenter, tableVersion, param_number);
  }

  /**
   * Looks for the parameter table which matches the center, subcenter and table version.
   *
   * @param center       - integer from PDS octet 5, representing Center.
   * @param subcenter    - integer from PDS octet 26, representing Subcenter
   * @param tableVersion - integer from PDS octet 4, representing Parameter Table Version
   * @return Grib1ParamTable matching center, subcenter, and number, or null if not found
   */
  public static Grib1ParamTable getParameterTable(int center, int subcenter, int tableVersion) {
    return standardLookup.getParameterTable(center, subcenter, tableVersion);
  }

  /**
   * Add all tables in list to standard tables
   *
   * @param lookupFilename filename containing list of tables
   * @return true if  read ok, false if file not found
   * @throws IOException if file found but read error
   */
  public static boolean addParameterTableLookup(String lookupFilename) throws IOException {
    Lookup lookup = new Lookup();
    if (!lookup.readLookupTable(lookupFilename))
      return false;

    synchronized (lock) {
      standardLookup.tables.addAll(standardTablesStart, lookup.tables);
      standardTablesStart += lookup.tables.size();
    }
    return true;
  }

  /**
   * Add table to standard tables for a specific center, subcenter and version.
   *
   * @param center        center id
   * @param subcenter     subcenter id, or -1 for all
   * @param tableVersion  table verssion, or -1 for all
   * @param tableFilename file to read parameter table from
   */
  public static void addParameterTable(int center, int subcenter, int tableVersion, String tableFilename) {
    Grib1ParamTable table = new Grib1ParamTable(center, subcenter, tableVersion, tableFilename);
    synchronized (lock) {
      standardLookup.tables.add(standardTablesStart, table);
      standardTablesStart++;
    }
  }

  //////////////////////////////////////////////////////////////////////////

  public static class Lookup {
    List<Grib1ParamTable> tables = new ArrayList<Grib1ParamTable>();
    Map<Integer, Grib1ParamTable> tableMap = new ConcurrentHashMap<Integer, Grib1ParamTable>();

    /**
     * read the lookup table from file
     *
     * @param resourceName read from file
     * @return true if successful
     * @throws IOException On badness
     */
    public boolean readLookupTable(String resourceName) throws IOException {
      InputStream inputStream = GribResourceReader.getInputStream(resourceName);
      if (inputStream == null) {
        logger.debug("Could not open table file:" + resourceName);
        return false;
      }
      return readLookupTable(inputStream, resourceName);
    }

    /**
     * read the lookup table from input stream
     *
     * @param is         The input stream
     * @param lookupFile full pathname of lookup file
     * @return true if successful
     * @throws IOException On badness
     */
    private boolean readLookupTable(InputStream is, String lookupFile) throws IOException {
      if (is == null)
        return false;

      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);

      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if ((line.length() == 0) || line.startsWith("#")) {
          continue;
        }
        String[] tableDefArr = line.split(":");

        int center = Integer.parseInt(tableDefArr[0].trim());
        int subcenter = Integer.parseInt(tableDefArr[1].trim());
        int version = Integer.parseInt(tableDefArr[2].trim());
        String filename = tableDefArr[3].trim();
        String path;
        if (filename.startsWith("/") || filename.startsWith("\\") || filename.startsWith("file:") || filename.startsWith("http://")) {
          path = filename;
        } else {
          path = GribResourceReader.getFileRoot(lookupFile) + "/" + filename;
        }

        Grib1ParamTable table = new Grib1ParamTable(center, subcenter, version, path);
        tables.add(table);
      }
      is.close();

      return true;
    }

    public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
      Grib1ParamTable pt = getParameterTable(center, subcenter, tableVersion);
      return (pt == null) ? null : pt.getParameter(param_number);
    }

    public  Grib1ParamTable getParameterTable(int center, int subcenter, int tableVersion) {
      // look in hash table
      int key = makeKey(center, subcenter, tableVersion);
      Grib1ParamTable table = tableMap.get(key);
      if (table != null)
        return table;

      // match from lookup tables(s)
      table = findParameterTable(center, subcenter, tableVersion);
      if (table == null) {
        if (warn)
          logger.warn("Could not find a table for GRIB file with center: " + center + " subCenter: " + subcenter + " version: " + tableVersion);
        return (strict) ? null : defaultTable;
      }

      tableMap.put(key, table);
      return table;
    }

    private Grib1ParamTable findParameterTable(int center, int subcenter, int version) {
      List<Grib1ParamTable> localCopy = tables; // thread safe
      for (Grib1ParamTable table : localCopy) {
        // look for a match
        if (center == table.center_id) {
          if ((table.subcenter_id == -1) || (subcenter == table.subcenter_id)) {
            //if ((table.subcenter_id == -1) || (table.subcenter_id == 0) || (subcenter == table.subcenter_id)) {
            if ((table.version == -1) || version == table.version) {  // match
              //  see if the parameters for this table have been read in yet.
              if (table.parameters == null) {
                table.readParameterTable();
                if (table.parameters == null) // failed - maybe theres another entry table in paramTables
                  continue;

                // success - initialize other tables parameters with the same name
                for (Grib1ParamTable table2 : localCopy) {
                  if (table2.path.equals(table.path))
                    table2.parameters = table.parameters;
                }
              }
              return table;
            }
          }
        }
      }

      return null;
    }


  }  // Lookup

  private static int makeKey(int center, int subcenter, int version) {
    if (center < 0) center = 255;
    if (subcenter < 0) subcenter = 255;
    if (version < 0) version = 255;
    return center * 1000 * 1000 + subcenter * 1000 + version;
  }

  //////////////////////////////////////////////////////////////////////////

  private int center_id;
  private int subcenter_id;
  private int version;
  private String name;  // name of the table
  private String path; // path of filename containing this table
  private Map<Integer, Grib1Parameter> parameters; // param number -> param

  public Grib1ParamTable(String path) throws IOException {
    this.path = StringUtil2.replace(path, "\\", "/");
    this.name = GribResourceReader.getFilename(path);
    this.parameters = readParameterTable(); // LOOK should cache ?
  }

  public Grib1ParamTable(int center_id, int subcenter_id, int version, String path) {
    this.center_id = center_id;
    this.subcenter_id = subcenter_id;
    this.version = version;
    this.path = path;
    this.name = GribResourceReader.getFilename(path);
  }

  public int getCenter_id() {
    return center_id;
  }

  public int getSubcenter_id() {
    return subcenter_id;
  }

  public int getVersion() {
    return version;
  }

  public String getName() {
    return name;
  }

  public int getKey() {
    return makeKey(center_id, subcenter_id, version);
  }

  public String getPath() {
    return path;
  }

  // debugging
  public Map<Integer, Grib1Parameter> getParameters() {
    if (parameters == null)
      readParameterTable();
    return parameters;
  }

  /**
   * Get the parameter with id. If not found, look in default table.
   *
   * @param id the parameter number
   * @return the Grib1Parameter, or null if not found
   */
  public Grib1Parameter getParameter(int id) {
    if (parameters == null)
      readParameterTable();

    Grib1Parameter p = parameters.get(id);
    if (p != null) return p;

    // get out of the wmo table if possible
    p = defaultTable.parameters.get(id);
    return p;

    /* warning
    logger.warn("Grib1ParamTable: Could not find parameter " + id + " for center:" + center_id
            + " subcenter:" + subcenter_id + " number:" + version + " table " + filename);
    String unknown = "UnknownParameter_" + Integer.toString(id) + "_table_" + filename;
    return new Grib1Parameter(id, unknown, unknown, "Unknown", null); */
  }

  /**
   * Get the parameter with id, but dont look in default table.
   *
   * @param id the parameter number
   * @return the Grib1Parameter, or null if not found
   */
  public Grib1Parameter getLocalParameter(int id) {
    if (parameters == null)
      readParameterTable();
    return parameters.get(id);
  }


  @Override
  public String getLevelNameShort(int code) {
    return Grib1ParamLevel.getNameShort(code);
  }


  @Override
  public String toString() {
    return "Grib1ParamTable{" +
            "center_id=" + center_id +
            ", subcenter_id=" + subcenter_id +
            ", version=" + version +
            ", name='" + name + '\'' +
            ", path='" + path + '\'' +
            '}';
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // reading

  private Map<Integer, Grib1Parameter> readParameterTable() {
    if (name.endsWith(".tab"))
      readParameterTableTab();                               // wgrib format
    else if (name.endsWith(".wrf"))
      readParameterTableSplit("\\|", new int[]{0, 3, 1, 2}); // WRF AMPS
    else if (name.endsWith(".h"))
      readParameterTableNcl(); // NCL
    else if (name.endsWith(".dss"))
      readParameterTableSplit("\t", new int[]{0, -1, 1, 2}); // NCAR DSS
    else if (name.endsWith(".xml"))
      readParameterTableXml();                               // NCAR DSS XML format
    else if (name.endsWith(".htm"))
      readParameterTableNcepScrape();                        // NCEP screen scrape
    else if (name.startsWith("table_2_") || name.startsWith("local_table_2_"))
      readParameterTableEcmwf(); // ecmwf
    else
      logger.warn("Dont know how to read " + name + " file=" + path);
    return parameters;
  }

/*
 * Brazilian Space Agency - INPE/CPTEC
 * Center: 46
 * Subcenter: 0
 * Parameter table version: 254
 *

TBLE2 cptec_254_params[] = {
{1, "Pressure", "hPa", "PRES"},
{2, "Pressure reduced to MSL", "hPa", "PSNM"},
{3, "Pressure tendency", "Pa/s", "TSPS"},
{6, "Geopotential", "dam", "GEOP"},
{7, "Geopotential height", "gpm", "ZGEO"},
{8, "Geometric height", "m", "GZGE"},
{11, "ABSOLUTE TEMPERATURE", "K", "TEMP"},

   */

  static private final Pattern nclPattern = Pattern.compile("\\{(\\d*)\\,\\s*\"([^\"]*)\"\\,\\s*\"([^\"]*)\"\\,\\s*\"([^\"]*)\".*");

  private boolean readParameterTableNcl() {
    HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      // Ignore header
      int count = 0;
      while (true) {
        String line = br.readLine();
        if (line == null) break; // done with the file
        if (line.startsWith("TBLE2")) break;

        if (line.contains("Center:"))
          center_id = extract(line, "Center:");
        else if (line.contains("Subcenter:"))
          subcenter_id = extract(line, "Subcenter:");
        else if (line.contains("version:"))
          version = extract(line, "version:");
        count++;
      }

      while (true) {
        String line = br.readLine();
        if (line == null) break; // done with the file
        if ((line.length() == 0) || line.startsWith("#")) continue;

        Matcher m = nclPattern.matcher(line);
        if (!m.matches()) continue;

        int p1;
        try {
          p1 = Integer.parseInt(m.group(1));
        } catch (Exception e) {
          logger.warn("Cant parse " + m.group(1) + " in file " + path);
          continue;
        }
        Grib1Parameter parameter = new Grib1Parameter(this, p1, m.group(4), m.group(2), m.group(3));
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }

      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table "
              + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  private int extract(String line, String key) {
    int pos = line.indexOf(key);
    if (pos < 0) return -1;
    String want = line.substring(pos + key.length());
    try {
      return Integer.parseInt(want.trim());
    } catch (NumberFormatException e) {
      System.out.printf("BAD %s (%s)%n", line, path);
      return -1;
    }
  }

  /*
  WMO standard table 2: Version Number 3.
  Codes and data units for FM 92-X Ext.GRIB.
  ......................
  001
  P
  Pressure
  Pa
  Pa
  ......................
  002
  MSL
  Mean sea level pressure
  Pa
  Pa
  ......................
  003
  None
  Pressure tendency
  Pa s**-1
  Pa s**-1
  ......................
  004
  PV
  Potential vorticity
  K m**2 kg**-1 s**-1
  K m**2 kg**-1 s**-1
  ......................
  005
  None
  ICAO Standard Atmosphere reference height
  m
  m
   */

  private boolean readParameterTableEcmwf() {
    HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line = br.readLine();
      if (!line.startsWith("...")) name = line; // maybe ??
      while (!line.startsWith("..."))
        line = br.readLine(); // skip

      while (true) {
        line = br.readLine();
        if (line == null) break; // done with the file
        if ((line.length() == 0) || line.startsWith("#")) continue;
        if (line.startsWith("...")) { // ...  may have already been read
          line = br.readLine();
          if (line == null) break;
        }
        String num = line.trim();
        line = br.readLine();
        String name = (line != null) ? line.trim() : null;
        line = br.readLine();
        String desc = (line != null) ? line.trim() : null;
        line = br.readLine();
        String units1 = (line != null) ? line.trim() : null;

        // optional notes
        line = br.readLine();
        String notes = (line == null || line.startsWith("...")) ? null : line.trim();

        if (desc.equalsIgnoreCase("undefined")) continue; // skip

        int p1;
        try {
          p1 = Integer.parseInt(num);
        } catch (Exception e) {
          logger.warn("Cant parse " + num + " in file " + path);
          continue;
        }
        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units1);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }

      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table "
              + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }


  /*
    <tr>
      <td>
      <center>243</center>
      </td>
      <td>Deep convective moistening rate</td>
      <td>kg/kg/s</td>
      <td>CNVMR</td>
    </tr>
   */
  private boolean readParameterTableNcepScrape() {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

      List<Element> params = root.getChildren("tr");
      for (Element elem1 : params) {
        List<Element> elems = elem1.getChildren("td");
        Element e1 = elems.get(0);
        String codeS = e1.getChildText("center");
        int code = Integer.parseInt(codeS);
        String desc = elems.get(1).getText();
        String units = elems.get(2).getText();
        String name = elems.get(3).getText();

        Grib1Parameter parameter = new Grib1Parameter(this, code, name, desc, units);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }
      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;


    } catch (JDOMException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  /* http://dss.ucar.edu/metadata/ParameterTables/WMO_GRIB1.60-1.3.xml
   <parameter code="5">
  <description>ICAO Standard Atmosphere reference height</description>
  <units>m</units>
  </parameter>
  */
  private boolean readParameterTableXml() {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        if (desc == null) continue;
        String units = elem1.getChildText("units");
        if (units == null) units = "";
        String name = elem1.getChildText("shortName");
        String cf = elem1.getChildText("CF");
        Grib1Parameter parameter = new Grib1Parameter(this, code, name, desc, units, cf);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }
      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;


    } catch (JDOMException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  // order: num, name, desc, unit
  private boolean readParameterTableSplit(String regexp, int[] order) {
    HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      // rdg - added the 0 line length check to cover the case of blank lines at
      //       the end of the parameter table file.
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] flds = line.split(regexp);

        int p1 = Integer.parseInt(flds[order[0]].trim()); // must have a number
        String name = (order[1] >= 0) ? flds[order[1]].trim() : null;
        String desc = flds[order[2]].trim();
        String units = (flds.length > order[3]) ? flds[order[3]].trim() : "";

        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }

      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table "
              + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  private boolean readParameterTableTab() {
    if (path == null) {
      logger.error("Grib1ParamTable: unknown path for " + this);
      return false;
    }

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("Grib1ParamTable: error getInputStream on " + this);
        return false;
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      br.readLine(); // skip a line

      HashMap<Integer, Grib1Parameter> params = new HashMap<Integer, Grib1Parameter>(); // thread safe - local var
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] tableDefArr = line.split(":");

        int p1 = Integer.parseInt(tableDefArr[0].trim());
        String name = tableDefArr[1].trim();
        String desc ,units;
        // check to see if unit defined, if not, parameter is undefined
        if (tableDefArr[2].indexOf('[') == -1) {
          // Undefined unit
          desc = tableDefArr[2].trim();
          units = "";
        } else {
          String[] arr2 = tableDefArr[2].split("\\[");
          desc =  arr2[0].trim();
          units = arr2[1].substring(0, arr2[1].lastIndexOf(']')).trim();
        }

        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units);
        if (!parameter.getDescription().equalsIgnoreCase("undefined"))
          params.put(parameter.getNumber(), parameter);
        if (debug)
          System.out.println(parameter.getNumber() + " " + parameter.getDescription() + " " + parameter.getUnit());
      }

      this.parameters = params; // thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table " + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  static public void main(String[] args) throws IOException {
    String dirS = "C:\\dev\\github\\thredds\\grib\\src\\main\\resources\\resources\\grib1\\ncl";
    File dir = new File(dirS);
    for (File f : dir.listFiles()) {
      if (!f.getName().endsWith(".h")) continue;
      Grib1ParamTable table = new Grib1ParamTable(f.getPath());

      //  60:	 1:		180:	WMO_GRIB1.60-1.180.xml
      System.out.printf("%5d: %5d: %5d: %s%n", table.getCenter_id(), table.getSubcenter_id(), table.getVersion(), table.getName());
    }
  }
}
