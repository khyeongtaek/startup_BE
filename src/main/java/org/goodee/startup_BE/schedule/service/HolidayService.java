package org.goodee.startup_BE.schedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

public interface HolidayService {
    List<HashMap<String, Object>> getHolidays(String year, String month);

    public boolean isHoliday(LocalDateTime dateTime);
}
