package ucar.nc2.grid2;

import org.junit.Test;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link GridSubset} */
public class TestGridSubset {

  @Test
  public void testHorizStride() {
    GridSubset subset = GridSubset.create();
    Integer strideo = subset.getHorizStride();
    assertThat(strideo).isNull();

    try {
      int stride = subset.getHorizStride();
      fail();
    } catch (NullPointerException e) {
      // correct
    }

    subset.setHorizStride(999);
    assertThat(subset.getHorizStride()).isEqualTo(999);
    int stride = subset.getHorizStride();
    assertThat(stride).isEqualTo(999);

    subset.setHorizStride((byte) 999);
    assertThat(subset.getHorizStride()).isEqualTo(-25);
  }

  @Test
  public void testEns() {
    GridSubset subset = GridSubset.create();
    Number enso = subset.getEnsCoord();
    assertThat(enso).isNull();

    try {
      double ens = subset.getEnsCoord().doubleValue();
      fail();
    } catch (NullPointerException e) {
      // correct
    }

    subset.setEnsCoord(999.0);
    assertThat(subset.getEnsCoord()).isEqualTo(999);
    double ens = subset.getEnsCoord().doubleValue();
    assertThat(ens).isEqualTo(999);
  }

  @Test
  public void testLatLonBoundingBox() {
    GridSubset subset = GridSubset.create();
    LatLonRect llbb = subset.getLatLonBoundingBox();
    assertThat(llbb).isNull();

    LatLonRect llbb1 = new LatLonRect();
    subset.setLatLonBoundingBox(llbb1);
    assertThat(subset.getLatLonBoundingBox()).isEqualTo(llbb1);
    assertThat(subset.getLatLonBoundingBox() == llbb1).isTrue();
  }

  @Test
  public void testLatLonPoint() {
    GridSubset subset = GridSubset.create();
    LatLonPoint pt = subset.getLatLonPoint();
    assertThat(pt).isNull();

    LatLonPoint llpt = LatLonPoint.create(99.0, .5);
    subset.setLatLonPoint(llpt);
    assertThat(subset.getLatLonPoint()).isEqualTo(llpt);
    assertThat(subset.getLatLonPoint()).isEqualTo(LatLonPoint.create(99.0, .5));
    assertThat(subset.getLatLonPoint() == llpt).isTrue();
  }

  @Test
  public void testProjectionBoundingBox() {
    GridSubset subset = GridSubset.create();
    ProjectionRect rect = subset.getProjectionBoundingBox();
    assertThat(rect).isNull();

    ProjectionRect rect1 = new ProjectionRect();
    subset.setProjectionBoundingBox(rect1);
    assertThat(subset.getProjectionBoundingBox()).isEqualTo(rect1);
    assertThat(subset.getProjectionBoundingBox() == rect1).isTrue();
  }

  @Test
  public void testRuntime() {
    GridSubset subset = GridSubset.create();
    CalendarDate cd = subset.getRunTime();
    assertThat(cd).isNull();

    CalendarDate cd1 = CalendarDate.present();
    subset.setRunTime(cd1);
    assertThat(subset.getRunTime()).isEqualTo(cd1);
    assertThat(subset.getRunTime() == cd1).isTrue();

    CalendarDate cd2 = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    subset.setRunTimeCoord(cd2);
    assertThat(subset.getRunTime()).isEqualTo(cd2);
    assertThat(subset.getRunTime() == cd2).isTrue();

    try {
      subset.setRunTimeCoord("bad");
      fail();
    } catch (Exception e) {
      // ok
    }

    try {
      subset.setRunTimeCoord(99);
      fail();
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  public void testRuntimeLatest() {
    GridSubset subset = GridSubset.create();
    Boolean latest = subset.getRunTimeLatest();
    assertThat(latest).isEqualTo(false);

    boolean late = subset.getRunTimeLatest();
    assertThat(late).isEqualTo(false);

    subset.setRunTimeLatest();
    assertThat(subset.getRunTimeLatest()).isEqualTo(true);
  }

  @Test
  public void testRuntimeAll() {
    GridSubset subset = GridSubset.create();
    Boolean latest = subset.getRunTimeAll();
    assertThat(latest).isEqualTo(false);

    boolean late = subset.getRunTimeAll();
    assertThat(late).isEqualTo(false);

    subset.setRunTimeAll();
    assertThat(subset.getRunTimeAll()).isEqualTo(true);
  }

  @Test
  public void testTime() {
    GridSubset subset = GridSubset.create();
    CalendarDate cd = subset.getTime();
    assertThat(cd).isNull();

    CalendarDate cd1 = CalendarDate.present();
    subset.setTime(cd1);
    assertThat(subset.getTime()).isEqualTo(cd1);
    assertThat(subset.getTime() == cd1).isTrue();
  }

  @Test
  public void testTimeCoord() {
    GridSubset subset = GridSubset.create();
    CoordInterval offsetv = CoordInterval.create(34.56, 78.9);
    subset.setTimeCoord(offsetv);
    assertThat(subset.getTimePoint()).isNull();
    assertThat(subset.getTimeIntv()).isEqualTo(offsetv);
    assertThat(subset.getTimeIntv() == offsetv).isTrue();

    subset.setTimeCoord(123.456);
    assertThat(subset.getTimePoint()).isEqualTo(123.456);

    subset.setTimeCoord(999);
    assertThat(subset.getTimePoint()).isEqualTo(999);

    try {
      subset.setTimeCoord(999);
    } catch (Exception e) {
      // correct
    }

    try {
      subset.setTimeCoord("999.9");
    } catch (Exception e) {
      // correct
    }
  }

  @Test
  public void testTimeStride() {
    GridSubset subset = GridSubset.create();
    Integer strideo = subset.getTimeStride();
    assertThat(strideo).isNull();

    try {
      int stride = subset.getTimeStride();
      fail();
    } catch (NullPointerException e) {
      // correct
    }

    subset.setTimeStride(999);
    assertThat(subset.getTimeStride()).isEqualTo(999);
    int stride = subset.getTimeStride();
    assertThat(stride).isEqualTo(999);

    subset.setTimeStride((byte) 999);
    assertThat(subset.getTimeStride()).isEqualTo(-25);
  }

  @Test
  public void testTimeRange() {
    GridSubset subset = GridSubset.create();
    CalendarDateRange cd = subset.getTimeRange();
    assertThat(cd).isNull();

    CalendarDate start = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    CalendarDateRange range = CalendarDateRange.of(start, 3600);
    subset.setTimeRange(range);
    assertThat(subset.getTimeRange()).isEqualTo(range);
    assertThat(subset.getTimeRange() == range).isTrue();
  }

  @Test
  public void testTimeOffsetCoord() {
    GridSubset subset = GridSubset.create();
    CoordInterval offsetv = CoordInterval.create(34.56, 78.9);
    subset.setTimeOffsetCoord(offsetv);
    assertThat(subset.getTimeOffset()).isNull();
    assertThat(subset.getTimeOffsetIntv()).isEqualTo(offsetv);
    assertThat(subset.getTimeOffsetIntv() == offsetv).isTrue();

    subset.setTimeOffsetCoord(123.456);
    assertThat(subset.getTimeOffset()).isEqualTo(123.456);

    subset.setTimeOffsetCoord(999);
    assertThat(subset.getTimeOffset()).isEqualTo(999);

    try {
      subset.setTimeOffsetCoord(999);
    } catch (Exception e) {
      // correct
    }

    try {
      subset.setTimeOffsetCoord("999.9");
    } catch (Exception e) {
      // correct
    }
  }

  @Test
  public void testTimeOffsetFirst() {
    GridSubset subset = GridSubset.create();
    Boolean latest = subset.getTimeOffsetFirst();
    assertThat(latest).isEqualTo(false);

    boolean late = subset.getTimeOffsetFirst();
    assertThat(late).isEqualTo(false);

    subset.setTimeOffsetFirst();
    assertThat(subset.getTimeOffsetFirst()).isEqualTo(true);
  }

  @Test
  public void testTimePresent() {
    GridSubset subset = GridSubset.create();
    Boolean latest = subset.getTimePresent();
    assertThat(latest).isEqualTo(false);

    boolean late = subset.getTimePresent();
    assertThat(late).isEqualTo(false);

    subset.setTimePresent();
    assertThat(subset.getTimePresent()).isEqualTo(true);
  }

  @Test
  public void testVertCoord() {
    GridSubset subset = GridSubset.create();
    CoordInterval offsetv = CoordInterval.create(34.56, 78.9);
    subset.setVertCoord(offsetv);
    assertThat(subset.getVertIntv()).isEqualTo(offsetv);
    assertThat(subset.getVertIntv() == offsetv).isTrue();

    subset.setVertCoord(123.456);
    assertThat(subset.getVertPoint()).isEqualTo(123.456);
  }

  @Test
  public void testStringConstructor() {
    GridSubset subset = GridSubset.create();
    CoordInterval offsetv = CoordInterval.create(34.5, 78.9);
    subset.setVertCoord(offsetv); // CoordInterval

    CalendarDate cd1 = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    subset.setRunTime(cd1); // CalendarDate

    subset.setEnsCoord(999.0); // Double
    subset.setHorizStride(999); // Integer
    subset.setRunTimeLatest(); // Boolean
    subset.setGridName("gridName"); // String

    CalendarDate start = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    CalendarDateRange range = CalendarDateRange.of(start, 3600);
    subset.setTimeRange(range); // CalendarDateRange

    subset.setLatLonPoint(LatLonPoint.create(99.0, .5)); // LatLonPoint
    subset.setLatLonBoundingBox(new LatLonRect()); // LatLonRect
    subset.setProjectionBoundingBox(new ProjectionRect()); // ProjectionRect

    HashMap<String, String> smap = new HashMap<>();
    for (Map.Entry<String, Object> entry : subset.getEntries()) {
      smap.put(entry.getKey(), entry.getValue().toString());
    }

    GridSubset copy = GridSubset.fromStringMap(smap);
    assertThat(subset.getVertIntv()).isEqualTo(copy.getVertIntv());
    assertThat(subset.getRunTime()).isEqualTo(copy.getRunTime());
    assertThat(subset.getEnsCoord().doubleValue()).isEqualTo(copy.getEnsCoord().doubleValue());
    assertThat(subset.getHorizStride()).isEqualTo(copy.getHorizStride());
    assertThat(subset.getRunTimeLatest()).isEqualTo(copy.getRunTimeLatest());
    assertThat(subset.getGridName()).isEqualTo(copy.getGridName());
    assertThat(subset.getTimeRange()).isEqualTo(copy.getTimeRange());

    assertThat(subset.getLatLonPoint()).isEqualTo(copy.getLatLonPoint());
    assertThat(subset.getLatLonBoundingBox()).isEqualTo(copy.getLatLonBoundingBox());
    assertThat(subset.getProjectionBoundingBox()).isEqualTo(copy.getProjectionBoundingBox());

    assertThat(subset.toString()).isEqualTo(copy.toString());
  }
}
