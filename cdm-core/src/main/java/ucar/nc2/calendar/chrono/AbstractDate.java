/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar.chrono;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.YEAR;

/**
 * An abstract date based on a year, month and day.
 * from org.threeten.extra.chrono.AbstractDate
 */
abstract class AbstractDate implements ChronoLocalDate {

  /**
   * Creates an instance.
   */
  AbstractDate() {}

  // -----------------------------------------------------------------------
  abstract int getProlepticYear();

  abstract int getMonth();

  abstract int getDayOfMonth();

  abstract int getDayOfYear();

  AbstractDate withDayOfYear(int value) {
    return plusDays(value - getDayOfYear());
  }

  int lengthOfWeek() {
    return 7;
  }

  int lengthOfYearInMonths() {
    return 12;
  }

  abstract ValueRange rangeAlignedWeekOfMonth();

  abstract AbstractDate resolvePrevious(int newYear, int newMonth, int dayOfMonth);

  AbstractDate resolveEpochDay(long epochDay) {
    return (AbstractDate) getChronology().dateEpochDay(epochDay);
  }

  // -----------------------------------------------------------------------
  @Override
  public ValueRange range(TemporalField field) {
    if (field instanceof ChronoField) {
      if (isSupported(field)) {
        return rangeChrono((ChronoField) field);
      }
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.rangeRefinedBy(this);
  }

  ValueRange rangeChrono(ChronoField field) {
    switch (field) {
      case DAY_OF_MONTH:
        return ValueRange.of(1, lengthOfMonth());
      case DAY_OF_YEAR:
        return ValueRange.of(1, lengthOfYear());
      case ALIGNED_WEEK_OF_MONTH:
        return rangeAlignedWeekOfMonth();
      default:
        break;
    }
    return getChronology().range(field);
  }

  // -----------------------------------------------------------------------
  @Override
  public long getLong(TemporalField field) {
    if (field instanceof ChronoField) {
      switch ((ChronoField) field) {
        case DAY_OF_WEEK:
          return getDayOfWeek();
        case ALIGNED_DAY_OF_WEEK_IN_MONTH:
          return getAlignedDayOfWeekInMonth();
        case ALIGNED_DAY_OF_WEEK_IN_YEAR:
          return getAlignedDayOfWeekInYear();
        case DAY_OF_MONTH:
          return getDayOfMonth();
        case DAY_OF_YEAR:
          return getDayOfYear();
        case EPOCH_DAY:
          return toEpochDay();
        case ALIGNED_WEEK_OF_MONTH:
          return getAlignedWeekOfMonth();
        case ALIGNED_WEEK_OF_YEAR:
          return getAlignedWeekOfYear();
        case MONTH_OF_YEAR:
          return getMonth();
        case PROLEPTIC_MONTH:
          return getProlepticMonth();
        case YEAR_OF_ERA:
          return getYearOfEra();
        case YEAR:
          return getProlepticYear();
        case ERA:
          return (getProlepticYear() >= 1 ? 1 : 0);
        default:
          break;
      }
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.getFrom(this);
  }

  int getAlignedDayOfWeekInMonth() {
    return ((getDayOfMonth() - 1) % lengthOfWeek()) + 1;
  }

  int getAlignedDayOfWeekInYear() {
    return ((getDayOfYear() - 1) % lengthOfWeek()) + 1;
  }

  int getAlignedWeekOfMonth() {
    return ((getDayOfMonth() - 1) / lengthOfWeek()) + 1;
  }

  int getAlignedWeekOfYear() {
    return ((getDayOfYear() - 1) / lengthOfWeek()) + 1;
  }

  int getDayOfWeek() {
    return (int) (Math.floorMod(toEpochDay() + 3, 7) + 1);
  }

  long getProlepticMonth() {
    return getProlepticYear() * lengthOfYearInMonths() + getMonth() - 1;
  }

  int getYearOfEra() {
    return getProlepticYear() >= 1 ? getProlepticYear() : 1 - getProlepticYear();
  }

  // -------------------------------------------------------------------------
  @Override
  public AbstractDate with(TemporalField field, long newValue) {
    if (field instanceof ChronoField) {
      ChronoField f = (ChronoField) field;
      getChronology().range(f).checkValidValue(newValue, f);
      int nvalue = (int) newValue;
      switch (f) {
        case DAY_OF_WEEK:
          return plusDays(newValue - getDayOfWeek());
        case ALIGNED_DAY_OF_WEEK_IN_MONTH:
          return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
        case ALIGNED_DAY_OF_WEEK_IN_YEAR:
          return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
        case DAY_OF_MONTH:
          return resolvePrevious(getProlepticYear(), getMonth(), nvalue);
        case DAY_OF_YEAR:
          return withDayOfYear(nvalue);
        case EPOCH_DAY:
          return resolveEpochDay(newValue);
        case ALIGNED_WEEK_OF_MONTH:
          return plusDays((newValue - getLong(ALIGNED_WEEK_OF_MONTH)) * lengthOfWeek());
        case ALIGNED_WEEK_OF_YEAR:
          return plusDays((newValue - getLong(ALIGNED_WEEK_OF_YEAR)) * lengthOfWeek());
        case MONTH_OF_YEAR:
          return resolvePrevious(getProlepticYear(), nvalue, getDayOfMonth());
        case PROLEPTIC_MONTH:
          return plusMonths(newValue - getProlepticMonth());
        case YEAR_OF_ERA:
          return resolvePrevious(getProlepticYear() >= 1 ? nvalue : 1 - nvalue, getMonth(), getDayOfMonth());
        case YEAR:
          return resolvePrevious(nvalue, getMonth(), getDayOfMonth());
        case ERA:
          return newValue == getLong(ERA) ? this : resolvePrevious(1 - getProlepticYear(), getMonth(), getDayOfMonth());
        default:
          break;
      }
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.adjustInto(this, newValue);
  }

  @Override
  public AbstractDate plus(long amountToAdd, TemporalUnit unit) {
    if (unit instanceof ChronoUnit) {
      ChronoUnit f = (ChronoUnit) unit;
      switch (f) {
        case DAYS:
          return plusDays(amountToAdd);
        case WEEKS:
          return plusWeeks(amountToAdd);
        case MONTHS:
          return plusMonths(amountToAdd);
        case YEARS:
          return plusYears(amountToAdd);
        case DECADES:
          return plusYears(Math.multiplyExact(amountToAdd, 10));
        case CENTURIES:
          return plusYears(Math.multiplyExact(amountToAdd, 100));
        case MILLENNIA:
          return plusYears(Math.multiplyExact(amountToAdd, 1000));
        case ERAS:
          return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
        default:
          break;
      }
      throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    return unit.addTo(this, amountToAdd);
  }

  AbstractDate plusYears(long yearsToAdd) {
    if (yearsToAdd == 0) {
      return this;
    }
    int newYear = YEAR.checkValidIntValue(Math.addExact(getProlepticYear(), yearsToAdd));
    return resolvePrevious(newYear, getMonth(), getDayOfMonth());
  }

  AbstractDate plusMonths(long months) {
    if (months == 0) {
      return this;
    }
    long curEm = getProlepticMonth();
    long calcEm = Math.addExact(curEm, months);
    int newYear = Math.toIntExact(Math.floorDiv(calcEm, lengthOfYearInMonths()));
    int newMonth = (int) (Math.floorMod(calcEm, lengthOfYearInMonths()) + 1);
    return resolvePrevious(newYear, newMonth, getDayOfMonth());
  }

  AbstractDate plusWeeks(long amountToAdd) {
    return plusDays(Math.multiplyExact(amountToAdd, lengthOfWeek()));
  }

  AbstractDate plusDays(long days) {
    if (days == 0) {
      return this;
    }
    return resolveEpochDay(Math.addExact(toEpochDay(), days));
  }

  // -------------------------------------------------------------------------
  long until(AbstractDate end, TemporalUnit unit) {
    if (unit instanceof ChronoUnit) {
      switch ((ChronoUnit) unit) {
        case DAYS:
          return daysUntil(end);
        case WEEKS:
          return weeksUntil(end);
        case MONTHS:
          return monthsUntil(end);
        case YEARS:
          return monthsUntil(end) / lengthOfYearInMonths();
        case DECADES:
          return monthsUntil(end) / (lengthOfYearInMonths() * 10);
        case CENTURIES:
          return monthsUntil(end) / (lengthOfYearInMonths() * 100);
        case MILLENNIA:
          return monthsUntil(end) / (lengthOfYearInMonths() * 1000);
        case ERAS:
          return end.getLong(ERA) - getLong(ERA);
        default:
          break;
      }
      throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    return unit.between(this, end);
  }

  long daysUntil(ChronoLocalDate end) {
    return end.toEpochDay() - toEpochDay(); // no overflow
  }

  long weeksUntil(AbstractDate end) {
    return daysUntil(end) / lengthOfWeek();
  }

  long monthsUntil(AbstractDate end) {
    long monthd = end.getProlepticMonth() - getProlepticMonth();
    long dayd = end.getDayOfMonth() - getDayOfMonth();

    long packed1 = getProlepticMonth() * 256L + getDayOfMonth(); // no overflow
    long packed2 = end.getProlepticMonth() * 256L + end.getDayOfMonth(); // no overflow
    long result = (packed2 - packed1) / 256L;
    return result;
  }

  ChronoPeriod doUntil(AbstractDate end) {
    long totalMonths = end.getProlepticMonth() - this.getProlepticMonth(); // safe
    int days = end.getDayOfMonth() - this.getDayOfMonth();
    if (totalMonths > 0 && days < 0) {
      totalMonths--;
      AbstractDate calcDate = this.plusMonths(totalMonths);
      days = (int) (end.toEpochDay() - calcDate.toEpochDay()); // safe
    } else if (totalMonths < 0 && days > 0) {
      totalMonths++;
      days -= end.lengthOfMonth();
    }
    long years = totalMonths / lengthOfYearInMonths(); // safe
    int months = (int) (totalMonths % lengthOfYearInMonths()); // safe
    return getChronology().period(Math.toIntExact(years), months, days);
  }

  // -------------------------------------------------------------------------
  /**
   * Compares this date to another date, including the chronology.
   * <p>
   * Compares this date with another ensuring that the date is the same.
   * <p>
   * Only objects of this concrete type are compared, other types return false.
   * To compare the dates of two {@code TemporalAccessor} instances, including dates
   * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
   *
   * @param obj the object to check, null returns false
   * @return true if this is equal to the other date
   */
  @Override // override for performance
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj != null && this.getClass() == obj.getClass()) {
      AbstractDate otherDate = (AbstractDate) obj;
      return this.getProlepticYear() == otherDate.getProlepticYear() && this.getMonth() == otherDate.getMonth()
          && this.getDayOfMonth() == otherDate.getDayOfMonth();
    }
    return false;
  }

  /**
   * A hash code for this date.
   *
   * @return a suitable hash code based only on the Chronology and the date
   */
  @Override // override for performance
  public int hashCode() {
    return getChronology().getId().hashCode()
        ^ ((getProlepticYear() & 0xFFFFF800) ^ ((getProlepticYear() << 11) + (getMonth() << 6) + (getDayOfMonth())));
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(30);
    buf.append(getChronology().toString()).append(" ").append(getEra()).append(" ").append(getYearOfEra())
        .append(getMonth() < 10 ? "-0" : "-").append(getMonth()).append(getDayOfMonth() < 10 ? "-0" : "-")
        .append(getDayOfMonth());
    return buf.toString();
  }

}
