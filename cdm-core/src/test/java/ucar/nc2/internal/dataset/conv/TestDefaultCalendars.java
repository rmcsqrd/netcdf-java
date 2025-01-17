/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxisTimeHelper;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.String.format;

public class TestDefaultCalendars {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Calendar defaultCoardsCalendar = Calendar.gregorian;
  private final Calendar defaultCFCalendar = Calendar.gregorian;

  private final String coardsConvention = "COARDS";
  private final String cfConvention = "CF-1.X";


  @Test
  public void testCfDefaultCalendar() throws IOException {
    String failMessage, found, expected;
    boolean testCond;

    String tstFile = TestDir.cdmLocalTestDataDir + "dataset/cfMissingCalendarAttr.nc";

    // open the test file
    NetcdfDataset ncd = NetcdfDatasets.openDataset(tstFile);

    // make sure this dataset used the cfConvention
    expected = cfConvention;
    found = ncd.getConventionUsed();
    testCond = found.equals(expected);
    failMessage =
        format("This dataset used the %s convention, but should have used the %s convention.", found, expected);
    Assert.assertTrue(failMessage, testCond);

    // get the Time Coordinate Axis and read the values
    CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
    Array times = tca.read();
    ncd.close();

    // first date in this file is 90 [hours since 2015-12-18T06:00:00],
    // which is 2015-12-22 00:00:00\
    expected = "90";
    found = Integer.toString(times.getInt(0));
    testCond = found.equals(expected);
    failMessage = format("The first value in the times array should be %s. I got %s instead.", expected, found);
    Assert.assertTrue(failMessage, testCond);

    // look for the calendar attached to the time variable...if there isn't one,
    // then a default was not set and the assert will fail.
    Calendar cal = Calendar.get(tca.attributes().findAttributeIgnoreCase(CF.CALENDAR).getStringValue()).orElseThrow();
    expected = defaultCFCalendar.toString();
    found = cal.toString();
    testCond = found.equals(expected);
    failMessage =
        format("The calendar should equal %s, but got %s instead. Failed to set a default calendar.", expected, found);
    Assert.assertTrue(failMessage, testCond);

    // convert the time value to a CalendarDate
    CoordinateAxisTimeHelper coordAxisTimeHelper =
        new CoordinateAxisTimeHelper(cal, tca.attributes().findAttributeIgnoreCase("units").getStringValue());
    CalendarDate date = coordAxisTimeHelper.makeCalendarDateFromOffset(times.getInt(0));

    // create the correct date as requested from NCSS
    String correctIsoDateTimeString = "2015-12-22T00:00:00Z";
    CalendarDate correctDate =
        CalendarDate.fromUdunitIsoDate(defaultCFCalendar.toString(), correctIsoDateTimeString).orElseThrow();

    // If everything is correct, then the date and correct date should be the same
    expected = correctDate.toString();
    found = date.toString();
    testCond = date.equals(correctDate);
    failMessage = format("The correct date is %s, but I got %s instead.", expected, found);
    Assert.assertTrue(failMessage, testCond);
  }

  // LOOK rewrite this
  // @Test
  public void testCoardsDefaultCalendar() throws IOException {
    String failMessage, found, expected;
    boolean testCond;

    String tstFile = TestDir.cdmLocalTestDataDir + "dataset/coardsMissingCalendarAttr.nc";

    // open the test file
    NetcdfDataset ncd = NetcdfDatasets.openDataset(tstFile);

    // make sure this dataset used the coardsConvention
    found = ncd.getConventionUsed();
    expected = coardsConvention;
    testCond = found.equals(expected);
    failMessage =
        format("This dataset used the %s convention, but should have used the %s convention.", found, expected);
    Assert.assertTrue(failMessage, testCond);

    // get the Time Coordinate Axis and read the values
    CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
    Array times = tca.read();
    ncd.close();

    // first date in this file is 1.766292E7 [hours since 0001-01-01T00:00:00],
    // which is 2008-06-27 00:00:00
    found = Integer.toString(times.getInt(0));
    expected = "17662920";
    testCond = found.equals(expected);
    failMessage = format("The first value in the times array should be %s. I got %s instead.", expected, found);
    Assert.assertTrue(failMessage, testCond);

    // look for the calendar attached to the time variable...if there isn't one,
    // then a default was not set and the assert will fail.
    Calendar cal = Calendar.get(tca.attributes().findAttributeIgnoreCase(CF.CALENDAR).getStringValue()).orElseThrow();
    found = cal.toString();
    expected = defaultCoardsCalendar.toString();
    testCond = found.equals(expected);
    failMessage =
        format("The calendar should equal %s, but got %s instead. Failed to add a default calendar.", expected, found);
    Assert.assertTrue(failMessage, testCond);

    // convert the time value to a CalendarDate
    CoordinateAxisTimeHelper coordAxisTimeHelper =
        new CoordinateAxisTimeHelper(cal, tca.attributes().findAttributeIgnoreCase("units").getStringValue());
    CalendarDate date = coordAxisTimeHelper.makeCalendarDateFromOffset(times.getInt(0));

    // read the correct date from the time attribute and turn it into a CalendarDate
    String correctIsoDateTimeString =
        tca.attributes().findAttributeIgnoreCase("correct_iso_time_value_str").getStringValue();
    CalendarDate correctDate =
        CalendarDate.fromUdunitIsoDate(defaultCoardsCalendar.toString(), correctIsoDateTimeString).orElseThrow();

    // If everything is correct, then the date and correct date should be the same
    assertThat(date).isEqualTo(correctDate);
    found = date.toString();
    expected = correctDate.toString();
    testCond = found.equals(expected);
    failMessage = format("The correct date is %s, but I got %s instead.", expected, found);
    Assert.assertTrue(failMessage, testCond);
  }
}

