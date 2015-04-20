package com.axelor.apps.hr.service.timesheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import com.axelor.apps.hr.db.DayPlanning;
import com.axelor.apps.hr.db.Leave;
import com.axelor.apps.hr.db.LogTime;
import com.axelor.apps.hr.db.Timesheet;
import com.axelor.apps.hr.db.repo.LeaveRepository;
import com.axelor.apps.hr.db.repo.LogTimeRepository;
import com.axelor.apps.hr.db.repo.TimesheetRepository;
import com.axelor.apps.hr.service.employee.EmployeeService;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;


public class TimesheetServiceImp extends TimesheetRepository implements TimesheetService{

	@Inject
	private EmployeeService employeeService;

	@Transactional(rollbackOn={Exception.class})
	public void getTimeFromTask(Timesheet timesheet){

		List<LogTime> logTimeList = LogTimeRepository.of(LogTime.class).all().filter("self.user = ?1 AND self.affectedToTimeSheet = null AND self.task != null", timesheet.getUser()).fetch();
		for (LogTime logTime : logTimeList) {
			logTime.setProject(logTime.getTask().getProject());
			timesheet.addLogTime(logTime);
		}
		JPA.save(timesheet);
	}

	@Transactional(rollbackOn={Exception.class})
	public void cancelTimesheet(Timesheet timesheet){
		timesheet.setStatusSelect(5);
		List<LogTime> logTimeList = timesheet.getLogTimes();
		for (LogTime logTime : logTimeList) {
			if(logTime.getTask() != null){
				logTime.setAffectedToTimeSheet(null);
				JPA.save(logTime);
			}
		}
	}

	@Transactional(rollbackOn={Exception.class})
	public void generateLines(Timesheet timesheet) throws AxelorException{

		if(timesheet.getFromDate() == null) throw new AxelorException("Please enter from date", 1);
		if(timesheet.getToDate() == null) throw new AxelorException("Please enter to date", 1);
		if(timesheet.getActivity() == null) throw new AxelorException("Please enter an activity", 1);
		if(timesheet.getUser().getEmployee().getTimeLoggingPreferenceSelect() == null) throw new AxelorException("Please configure your Time logging preference in your employee form", 1);

		LocalDate fromDate = timesheet.getFromDate();
		LocalDate toDate = timesheet.getToDate();
		Map<Integer,String> correspMap = new HashMap<Integer,String>();
		correspMap.put(1, "monday");
		correspMap.put(2, "tuesday");
		correspMap.put(3, "wednesday");
		correspMap.put(4, "thursday");
		correspMap.put(5, "friday");
		correspMap.put(6, "saturday");
		correspMap.put(7, "sunday");
		List<DayPlanning> dayPlanningList = timesheet.getUser().getEmployee().getPlanning().getWeekDays();
		List<Leave> leaveList = LeaveRepository.of(Leave.class).all().filter("self.user = ?1 AND (self.statusSelect = 2 OR self.statusSelect = 3)", timesheet.getUser()).fetch();
		while(!fromDate.isAfter(toDate)){
			DayPlanning dayPlanningCurr = new DayPlanning();
			for (DayPlanning dayPlanning : dayPlanningList) {
				if(dayPlanning.getName().equals(correspMap.get(fromDate.getDayOfWeek()))){
					dayPlanningCurr = dayPlanning;
					break;
				}
			}
			if(dayPlanningCurr.getMorningFrom() != null || dayPlanningCurr.getMorningTo() != null || dayPlanningCurr.getAfternoonFrom() != null || dayPlanningCurr.getAfternoonTo() != null)
			{
				boolean noLeave = true;
				if(leaveList != null){
					for (Leave leave : leaveList) {
						if((leave.getDateFrom().isBefore(fromDate) && leave.getDateTo().isAfter(fromDate))
							|| leave.getDateFrom().isEqual(fromDate) || leave.getDateTo().isEqual(fromDate))
						{
							noLeave = false;
							break;
						}
					}
				}
				if(noLeave){
					LogTime logTime = new LogTime();
					logTime.setDate(fromDate);
					logTime.setUser(timesheet.getUser());
					if(timesheet.getProject()!=null)logTime.setProject(timesheet.getProject());
					if(timesheet.getTask()!=null)logTime.setTask(timesheet.getTask());
					logTime.setVisibleDuration(timesheet.getLogTime());
					logTime.setDurationStored(employeeService.getDurationHours(timesheet.getLogTime()));
					logTime.setActivity(timesheet.getActivity());
					timesheet.addLogTime(logTime);
				}
			}
			fromDate=fromDate.plusDays(1);
		}
		save(timesheet);
	}

}
