package org.activiti5.engine.impl.calendar;

import org.activiti5.engine.runtime.ClockReader;

import java.util.Date;

/**
 * This class implements business calendar based on internal clock
 */
public abstract class BusinessCalendarImpl implements BusinessCalendar {

  protected ClockReader clockReader;

  public BusinessCalendarImpl(ClockReader clockReader) {
    this.clockReader = clockReader;
  }

  @Override
  public abstract Date resolveDuedate(String duedateDescription);
}
