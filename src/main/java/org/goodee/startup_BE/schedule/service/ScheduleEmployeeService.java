package org.goodee.startup_BE.schedule.service;

import org.goodee.startup_BE.schedule.controller.ScheduleEmployeeController.EmployeeInfoDTO;
import java.util.List;

public interface ScheduleEmployeeService {

    List<EmployeeInfoDTO> getAllEmployees();
}