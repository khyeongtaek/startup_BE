package org.goodee.startup_BE.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
    @RequiredArgsConstructor
    public class HolidayServiceImpl implements HolidayService {

    private final HolidayRequestAPI holidayRequestAPI;

    @Override
        public List<HashMap<String, Object>> getHolidays(String year, String month) {

            ArrayList<HashMap<String, Object>> responseHolidayArr = new ArrayList<>();

            try {
                Map<String, Object> holidayMap = holidayRequestAPI.holidayInfoAPI(year, month);
                Map<String, Object> response = (Map<String, Object>) holidayMap.get("response");
                Map<String, Object> body = (Map<String, Object>) response.get("body");

                // System.out.println("body = " + body);

                int totalCount = (int) body.get("totalCount");

                if (totalCount <= 0) {
                    //System.out.println("공휴일 없음");
                }

                if (totalCount == 1) {
                    HashMap<String, Object> items = (HashMap<String, Object>) body.get("items");
                    HashMap<String, Object> item = (HashMap<String, Object>) items.get("item");
                    responseHolidayArr.add(item);
                    //System.out.println("item = " + item);
                }

                if (totalCount > 1) {
                    HashMap<String, Object> items = (HashMap<String, Object>) body.get("items");
                    ArrayList<HashMap<String, Object>> itemList =
                            (ArrayList<HashMap<String, Object>>) items.get("item");

                    for (HashMap<String, Object> itemMap : itemList) {
                        //System.out.println("itemMap = " + itemMap);
                        responseHolidayArr.add(itemMap);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return responseHolidayArr;
        }
    @Override
    public boolean isHoliday(LocalDateTime dateTime) {

        LocalDate date = dateTime.toLocalDate();

        String year = String.valueOf(date.getYear());
        String month = String.format("%02d", date.getMonthValue());

        List<HashMap<String, Object>> holidays = getHolidays(year, month);

        String ymd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return holidays.stream()
                .anyMatch(h -> String.valueOf(h.get("locdate")).equals(ymd));
    }

    }
