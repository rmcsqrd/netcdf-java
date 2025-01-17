/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.widget;

import thredds.client.catalog.DateType;
import thredds.client.catalog.TimeCoverage;
import thredds.client.catalog.TimeDuration;
import thredds.ui.datatype.prefs.DateField;
import thredds.ui.datatype.prefs.DurationField;
import ucar.nc2.calendar.CalendarDate;
import ucar.ui.event.ActionSourceListener;
import ucar.ui.event.ActionValueEvent;
import ucar.ui.event.ActionValueListener;
import ucar.ui.prefs.Field;
import ucar.ui.prefs.FieldValidator;
import ucar.ui.prefs.PrefPanel;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;

import ucar.ui.widget.HelpWindow;

/**
 * Widget to select a point or a range from a date range.
 */
public class RangeDateSelector extends JPanel implements FieldValidator {
  public static final String TIME_START = "start";
  public static final String TIME_END = "end";
  public static final String TIME_DURATION = "duration";
  public static final String TIME_RESOLUTION = "resolution";

  private static final int SLIDER_RESOLUTION = 1000;
  private static final String actionName = "rangeDateSelection";

  private final String title;
  private final String helpMessage;
  private boolean acceptButton;
  private final boolean enableButton;
  private final boolean isPointOnly;
  private final boolean useLimits;

  private thredds.client.catalog.DateType minLimit, maxLimit; // min and max allowed values
  private thredds.client.catalog.TimeCoverage dateRange; // data model
  private Scale scale;

  // various widgets to manipulate the model
  private JSlider minSlider, maxSlider;
  private DateField minField, maxField;
  private DurationField durationField, resolutionField;

  // ui helpers
  private PrefPanel pp;
  private JLabel minLabel, maxLabel;
  private JButton helpButton;
  private JToggleButton disableButton;
  private HelpWindow helpWindow;

  // event management
  private ActionSourceListener actionSource;
  private boolean eventOK = true;

  private static boolean debugEvent, debugEvent2;

  /**
   * Constructor using info from thredds DQC.
   *
   * @param title widget title displayed to user, may be null
   * @param start starting date as a string
   * @param end ending date as a string
   * @param durationS duration as a String
   * @param minInterval minimum useful interval as a String, may be null.
   * @param acceptButton add an acceptButton
   * @param help optional help text
   * @param pointOnly if user can only select one point, otherwise can select a range of dates.
   */
  public RangeDateSelector(String title, String start, String end, String durationS, String minInterval,
      boolean enableButton, boolean acceptButton, String help, boolean pointOnly) throws Exception {

    this(title,
        TimeCoverage.create((start == null) ? null : DateType.parse(start), (end == null) ? null : DateType.parse(end),
            (durationS == null) ? null : TimeDuration.parse(durationS),
            (minInterval == null) ? null : TimeDuration.parse(minInterval)),
        enableButton, acceptButton, help, pointOnly, true);
  }

  /**
   * Constructor.
   * 
   * @param title widget title displayed to user, may be null
   * @param range range that the user can select from
   * @param acceptButton add an accept Button
   * @param enableButton add an enable Button
   * @param help optional help text
   * @param pointOnly if user can only select one point, otherwise can select a range of dates.
   */
  public RangeDateSelector(String title, TimeCoverage range, boolean enableButton, boolean acceptButton, String help,
      boolean pointOnly, boolean useLimits) {
    this.title = title;
    this.dateRange = range;
    this.acceptButton = acceptButton;
    this.enableButton = enableButton;
    this.helpMessage = help;
    this.isPointOnly = pointOnly;
    this.useLimits = useLimits;

    init();
  }

  private void init() {

    // UI
    // optional top panel
    JPanel topPanel;
    topPanel = new JPanel(new BorderLayout());
    JPanel butts = new JPanel();

    if (title != null)
      topPanel.add(BorderLayout.WEST, new JLabel("  " + title + ":"));

    if (helpMessage != null) {
      helpButton = new JButton("help");
      helpButton.addActionListener(e -> {
        if (helpWindow == null)
          helpWindow = new HelpWindow(null, "Help on " + title, helpMessage);
        helpWindow.show(helpButton);
      });
      butts.add(helpButton);
    }

    if (acceptButton) {
      JButton okButton = new JButton("accept");
      okButton.addActionListener(e -> {
        pp.accept();
        sendEvent();
      });

      butts.add(okButton);
      acceptButton = false; // dont need it in prefpanel
    }

    if (enableButton) {
      disableButton = new JToggleButton("disable", false);
      disableButton.addActionListener(e -> {
        boolean b = !disableButton.getModel().isSelected();
        minField.setEnabled(b);
        maxField.setEnabled(b);
        durationField.setEnabled(b);
        minSlider.setEnabled(b);
        maxSlider.setEnabled(b);
      });

      butts.add(disableButton);
    }

    topPanel.add(BorderLayout.EAST, butts);

    // the sliders
    JPanel sliderPanel = new JPanel();
    sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
    sliderPanel.setBorder(new LineBorder(Color.black, 1, true));

    minSlider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER_RESOLUTION, 0);
    maxSlider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER_RESOLUTION, SLIDER_RESOLUTION);

    Border b = BorderFactory.createEmptyBorder(0, 15, 0, 15);
    minSlider.setBorder(b);
    maxSlider.setBorder(b);

    // set this so we can call setDateRange();
    minLabel = new JLabel();
    maxLabel = new JLabel();
    minField = new DateField(TIME_START, isPointOnly ? "value" : "start", dateRange.getStart(), null);
    maxField = new DateField(TIME_END, "end", dateRange.getEnd(), null);
    durationField = new DurationField(TIME_DURATION, "duration", dateRange.getDuration(), null);
    resolutionField = new DurationField(TIME_RESOLUTION, "resolution", dateRange.getResolution(), null);

    minField.addValidator(this);
    maxField.addValidator(this);
    durationField.addValidator(this);
    setDateRange(dateRange);

    JPanel labelPanel = new JPanel(new BorderLayout());
    labelPanel.add(minLabel, BorderLayout.WEST);
    labelPanel.add(maxLabel, BorderLayout.EAST);

    // the fields use a PrefPanel
    pp = new PrefPanel(null, null);
    int row = 0;
    // if (tit != null) {
    // pp.addComponent(new JLabel(tit), col, 0, null);
    // col+=2;
    // }
    if (isPointOnly) {
      pp.addField(minField, 0, row, null);
    } else {
      pp.addField(minField, 0, row++, null);
      pp.addField(maxField, 0, row++, null);
      pp.addField(durationField, 0, row++, null);
      pp.addField(resolutionField, 0, row, null);
    }
    pp.finish(acceptButton, BorderLayout.EAST);

    setLayout(new BorderLayout()); // allow width expansion

    // overall layout
    sliderPanel.add(topPanel);
    sliderPanel.add(pp);

    if (useLimits) {
      if (!isPointOnly)
        sliderPanel.add(maxSlider);
      sliderPanel.add(minSlider);
      sliderPanel.add(labelPanel);
    }

    add(sliderPanel, BorderLayout.NORTH);

    /// event management

    // listen for changes from user manupulation
    maxSlider.addChangeListener(e -> {
      if (debugEvent2)
        System.out.println("maxSlider event= " + maxSlider.getValue());
      if (!eventOK)
        return;

      int pos = maxSlider.getValue();
      dateRange = dateRange.toBuilder().setEnd(scale.slider2world(pos)).build();
      synchUI(false);

      if (dateRange.isPoint())
        minSlider.setValue(pos); // drag min along */
    });

    minSlider.addChangeListener(e -> {
      if (debugEvent2)
        System.out.println("minSlider event= " + minSlider.getValue());
      if (!eventOK)
        return;

      int pos = minSlider.getValue();
      dateRange = dateRange.toBuilder().setStart(scale.slider2world(pos)).build();
      synchUI(false);

      if (dateRange.isPoint() && !isPointOnly)
        maxSlider.setValue(pos); // drag max along
    });

    minField.addPropertyChangeListener(e -> {
      if (debugEvent)
        System.out.println("minField event= " + e.getNewValue() + " " + e.getNewValue().getClass().getName());
      if (!eventOK)
        return;

      DateType val = (DateType) minField.getValue();
      dateRange = dateRange.toBuilder().setStart(val).build();
      synchUI(true);
    });

    if (maxField != null) {
      maxField.addPropertyChangeListener(e -> {
        if (debugEvent)
          System.out.println("maxField event= " + e.getNewValue());
        if (!eventOK)
          return;

        DateType val = (DateType) maxField.getValue();
        dateRange = dateRange.toBuilder().setEnd(val).build();
        synchUI(true);
      });
    }

    if (durationField != null) {
      durationField.addPropertyChangeListener(e -> {
        if (debugEvent)
          System.out.println("durationField event= " + e.getNewValue());
        if (!eventOK)
          return;

        TimeDuration val = durationField.getTimeDuration();
        dateRange = dateRange.toBuilder().setDuration(val).build();
        synchUI(true);
      });
    }

    if (resolutionField != null) {
      resolutionField.addPropertyChangeListener(e -> {
        if (debugEvent)
          System.out.println("resolutionField event= " + e.getNewValue());
        if (!eventOK)
          return;

        TimeDuration val = resolutionField.getTimeDuration();
        dateRange = dateRange.toBuilder().setResolution(val).build();
      });
    }

    // listen for outside changes
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        if (debugEvent)
          System.out.println(" actionSource event " + e);
        // ?? setSelectedByName( e.getValue().toString());
      }
    };
  }

  public boolean validate(Field fld, Object editValue, StringBuffer errMessages) {
    if (!useLimits) {
      return true;
    }

    DateType checkVal;

    if (fld == durationField) {
      TimeDuration duration = (TimeDuration) editValue;
      if (dateRange.getEnd().isPresent()) {
        checkVal = dateRange.getEnd().subtract(duration);
      } else {
        checkVal = dateRange.getStart().add(duration);
      }
    } else {
      checkVal = (DateType) editValue; // otherwise its one of the dates
    }

    // have to be inside the limits
    CalendarDate d = checkVal.toCalendarDate();
    if (d.isAfter(maxLimit.toCalendarDate()) || d.isBefore(minLimit.toCalendarDate())) {
      errMessages.append("Date ");
      errMessages.append(d.toString());
      errMessages.append(" must be between ");
      errMessages.append(minLimit.getText());
      errMessages.append(" and ");
      errMessages.append(maxLimit.getText());
      return false;
    }

    return true;
  }

  // set values on the UI
  private void synchUI(boolean slidersOK) {
    eventOK = false;
    if (slidersOK)
      minSlider.setValue(scale.world2slider(dateRange.getStart()));
    minField.setValue(dateRange.getStart());

    if (maxField != null) {
      if (slidersOK)
        maxSlider.setValue(scale.world2slider(dateRange.getEnd()));
      maxField.setValue(dateRange.getEnd());
    }

    if (durationField != null)
      durationField.setValue(dateRange.getDuration());

    eventOK = true;
  }

  public void setDateRange(TimeCoverage dateRange) {
    this.dateRange = dateRange;

    this.minLimit = dateRange.getStart();
    this.maxLimit = dateRange.getEnd();
    this.scale = new Scale(dateRange);

    minLabel.setText(" " + minLimit.getText() + " ");
    maxLabel.setText(" " + maxLimit.getText() + " ");

    if (isPointOnly) {
      minField.setValue(dateRange.getStart());
    } else {
      minField.setValue(dateRange.getStart());
      maxField.setValue(dateRange.getEnd());
      durationField.setValue(dateRange.getDuration());
      resolutionField.setValue(dateRange.getResolution());
    }
  }

  public DateField getMinDateField() {
    return minField;
  }

  public DateField getMaxDateField() {
    return maxField;
  }

  public DurationField getDurationField() {
    return durationField;
  }

  public DurationField getResolutionField() {
    return resolutionField;
  }

  public boolean isEnabled() {
    return (null == disableButton) || !disableButton.getModel().isSelected();
  }

  public TimeCoverage getDateRange() {
    if (!pp.accept())
      return null;
    return dateRange;
  }

  public void sendEvent() {
    // gotta do this after the dust settles
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        System.out.println("event range= " + dateRange);
        actionSource.fireActionValueEvent(actionName, this);
      } // run
    }); // invokeLater */
  }

  /** add ActionValueListener listener */
  public void addActionValueListener(ActionValueListener l) {
    actionSource.addActionValueListener(l);
  }

  /** remove ActionValueListener listener */
  public void removeActionValueListener(ActionValueListener l) {
    actionSource.removeActionValueListener(l);
  }

  private static class Scale {
    private final double min; // secs
    private final double scale; // pixels / secs

    Scale(TimeCoverage dateRange) {
      this.min = .001 * dateRange.getStart().toCalendarDate().getMillisFromEpoch();
      scale = SLIDER_RESOLUTION / dateRange.getDuration().getValueInSeconds();
    }

    private int world2slider(DateType val) {
      double msecs = .001 * val.toCalendarDate().getMillisFromEpoch() - min;
      return (int) (scale * msecs);
    }

    private DateType slider2world(int pval) {
      double val = pval / scale; // secs
      double msecs = 1000 * (min + val);
      return new DateType(CalendarDate.of((long) msecs));
    }
  }

}
