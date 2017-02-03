/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.project.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

import com.axelor.apps.base.db.Team;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.ProjectPlanning;
import com.axelor.apps.project.db.ProjectPlanningLine;
import com.axelor.apps.project.db.repo.ProjectPlanningLineRepository;
import com.axelor.apps.project.db.repo.ProjectPlanningRepository;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.apps.project.exception.IExceptionMessage;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ProjectPlanningService {

	@Inject
	protected ProjectPlanningLineRepository projectPlanningLineRepository;

	@Inject
	protected AppBaseService appBaseService;
	
	@Inject
	protected ProjectPlanningRepository projectPlanningRepo;

	@Transactional
	public ProjectPlanning createPlanning(int year, int week) throws AxelorException{
		ProjectPlanning planning = new ProjectPlanning();
		planning.setYear(year);
		planning.setWeek(week);

		projectPlanningRepo.save(planning);
		return planning;
	}

	public static String getNameForColumns(int year, int week, int day){
		LocalDate date = LocalDate.now().withYear(year).with(IsoFields
				.WEEK_OF_WEEK_BASED_YEAR, week).with(DayOfWeek.MONDAY);
		LocalDate newDate = date.plusDays(day - 1);
		return " " + Integer.toString(newDate.getDayOfMonth())+"/"+Integer.toString(newDate.getMonthValue());
	}

	@Transactional
	public List<ProjectPlanningLine> populateMyPlanning(ProjectPlanning planning, User user) throws AxelorException{
		List<ProjectPlanningLine> planningLineList = new ArrayList<ProjectPlanningLine>();
		String query = "self.assignedTo = ?1 OR ?1 MEMBER OF self.membersUserSet";
		List<Project> projectList = Beans.get(ProjectRepository.class).all().filter(query, user).fetch();
		if(projectList == null || projectList.isEmpty()){
			throw new AxelorException(String.format(I18n.get(IExceptionMessage.PROJECT_PLANNING_NO_TASK)), IException.CONFIGURATION_ERROR);
		}
		for (Project project : projectList) {
			ProjectPlanningLine projectPlanningLine = null;
			projectPlanningLine = projectPlanningLineRepository.all().filter("self.user = ?1 AND self.project = ?2 AND self.year = ?3 AND self.week = ?4", user, project, planning.getYear(), planning.getWeek()).fetchOne();
			if(projectPlanningLine == null){
				projectPlanningLine = new ProjectPlanningLine();
				projectPlanningLine.setUser(user);
				projectPlanningLine.setProject(project);
				projectPlanningLine.setYear(planning.getYear());
				projectPlanningLine.setWeek(planning.getWeek());
				projectPlanningLineRepository.save(projectPlanningLine);
			}
			planningLineList.add(projectPlanningLine);
		}
		return planningLineList;
	}

	@Transactional
	public List<ProjectPlanningLine> populateMyTeamPlanning(ProjectPlanning planning, Team team) throws AxelorException{
		List<ProjectPlanningLine> planningLineList = new ArrayList<ProjectPlanningLine>();
		List<Project> projectList = null;
		Set<User> userList = team.getUserSet();

		for (User user : userList) {
			String query = "self.assignedTo = ?1 OR ?1 MEMBER OF self.membersUserSet";
			projectList = Beans.get(ProjectRepository.class).all().filter(query, user).fetch();
			if(projectList != null && !projectList.isEmpty()){
				for (Project project : projectList) {
					ProjectPlanningLine projectPlanningLine = null;
					projectPlanningLine = projectPlanningLineRepository.all().filter("self.user = ?1 AND self.project = ?2 AND self.year = ?3 AND self.week = ?4", user, project, planning.getYear(), planning.getWeek()).fetchOne();
					if(projectPlanningLine == null){
						projectPlanningLine = new ProjectPlanningLine();
						projectPlanningLine.setUser(user);
						projectPlanningLine.setProject(project);
						projectPlanningLine.setYear(planning.getYear());
						projectPlanningLine.setWeek(planning.getWeek());
						projectPlanningLineRepository.save(projectPlanningLine);
					}
					planningLineList.add(projectPlanningLine);
				}
			}
		}

		if(planningLineList.isEmpty()){
			throw new AxelorException(String.format(I18n.get(IExceptionMessage.PROJECT_PLANNING_NO_TASK_TEAM)), IException.CONFIGURATION_ERROR);
		}
		return planningLineList;
	}

	public LocalDate getFromDate(){
		LocalDate todayDate = appBaseService.getTodayDate();
		return LocalDate.of(todayDate.getYear(), todayDate.getMonthValue(), 1);
	}

	public LocalDate getToDate(){
		LocalDate todayDate = appBaseService.getTodayDate();
		return LocalDate.of(todayDate.getYear(), todayDate.getMonthValue(), todayDate.lengthOfMonth());
	}
	
	public void getTasksForUser(ActionRequest request, ActionResponse response){
		List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
		try{
			LocalDate todayDate = appBaseService.getTodayDate();
			List<ProjectPlanningLine> linesList = Beans.get(ProjectPlanningLineRepository.class).all().
					filter("self.user.id = ?1 AND self.year >= ?2 AND self.week >= ?3", 
					AuthUtils.getUser().getId(), todayDate.getYear(), todayDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)).fetch();
			
			for (ProjectPlanningLine line : linesList) {
				if(line.getMonday().compareTo(BigDecimal.ZERO) != 0){
					LocalDate date = LocalDate.now().withYear(line.getYear()).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,line.getWeek()).with(DayOfWeek.MONDAY);
					if(date.isAfter(todayDate) || date.isEqual(todayDate)){
						Map<String, String> map = new HashMap<String,String>();
						map.put("taskId", line.getProject().getId().toString());
						map.put("name", line.getProject().getFullName());
						if(line.getProject().getProject() != null){
							map.put("projectName", line.getProject().getProject().getFullName());
						}
						else{
							map.put("projectName", "");
						}
						map.put("date", date.toString());
						map.put("duration", line.getMonday().toString());
						dataList.add(map);
					}
				}
				if(line.getTuesday().compareTo(BigDecimal.ZERO) != 0){
					LocalDate date = LocalDate.now().withYear(line.getYear()).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,line.getWeek()).with(DayOfWeek.TUESDAY);
					if(date.isAfter(todayDate) || date.isEqual(todayDate)){
						Map<String, String> map = new HashMap<String,String>();
						map.put("taskId", line.getProject().getId().toString());
						map.put("name", line.getProject().getFullName());
						if(line.getProject().getProject() != null){
							map.put("projectName", line.getProject().getProject().getFullName());
						}
						else{
							map.put("projectName", "");
						}
						map.put("date", date.toString());
						map.put("duration", line.getTuesday().toString());
						dataList.add(map);
					}
				}
				if(line.getWednesday().compareTo(BigDecimal.ZERO) != 0){
					LocalDate date = LocalDate.now().withYear(line.getYear()).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,line.getWeek()).with(DayOfWeek.WEDNESDAY);
					if(date.isAfter(todayDate) || date.isEqual(todayDate)){
						Map<String, String> map = new HashMap<String,String>();
						map.put("taskId", line.getProject().getId().toString());
						map.put("name", line.getProject().getFullName());
						if(line.getProject().getProject() != null){
							map.put("projectName", line.getProject().getProject().getFullName());
						}
						else{
							map.put("projectName", "");
						}
						map.put("date", date.toString());
						map.put("duration", line.getWednesday().toString());
						dataList.add(map);
					}
				}
				if(line.getThursday().compareTo(BigDecimal.ZERO) != 0){
					LocalDate date = LocalDate.now().withYear(line.getYear()).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,line.getWeek()).with(DayOfWeek.THURSDAY);
					if(date.isAfter(todayDate) || date.isEqual(todayDate)){
						Map<String, String> map = new HashMap<String,String>();
						map.put("taskId", line.getProject().getId().toString());
						map.put("name", line.getProject().getFullName());
						if(line.getProject().getProject() != null){
							map.put("projectName", line.getProject().getProject().getFullName());
						}
						else{
							map.put("projectName", "");
						}
						map.put("date", date.toString());
						map.put("duration", line.getThursday().toString());
						dataList.add(map);
					}
				}
				if(line.getFriday().compareTo(BigDecimal.ZERO) != 0){
					LocalDate date = LocalDate.now().withYear(line.getYear()).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,line.getWeek()).with(DayOfWeek.FRIDAY);
					if(date.isAfter(todayDate) || date.isEqual(todayDate)){
						Map<String, String> map = new HashMap<String,String>();
						map.put("taskId", line.getProject().getId().toString());
						map.put("name", line.getProject().getFullName());
						if(line.getProject().getProject() != null){
							map.put("projectName", line.getProject().getProject().getFullName());
						}
						else{
							map.put("projectName", "");
						}
						map.put("date", date.toString());
						map.put("duration", line.getFriday().toString());
						dataList.add(map);
					}
				}
				if(line.getSaturday().compareTo(BigDecimal.ZERO) != 0){
					LocalDate date = LocalDate.now().withYear(line.getYear()).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,line.getWeek()).with(DayOfWeek.SATURDAY);
					if(date.isAfter(todayDate) || date.isEqual(todayDate)){
						Map<String, String> map = new HashMap<String,String>();
						map.put("taskId", line.getProject().getId().toString());
						map.put("name", line.getProject().getFullName());
						if(line.getProject().getProject() != null){
							map.put("projectName", line.getProject().getProject().getFullName());
						}
						else{
							map.put("projectName", "");
						}
						map.put("date", date.toString());
						map.put("duration", line.getSaturday().toString());
						dataList.add(map);
					}
				}
				if(line.getSunday().compareTo(BigDecimal.ZERO) != 0){
					LocalDate date = LocalDate.now().withYear(line.getYear()).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,line.getWeek()).with(DayOfWeek.SUNDAY);
					if(date.isAfter(todayDate) || date.isEqual(todayDate)){
						Map<String, String> map = new HashMap<String,String>();
						map.put("taskId", line.getProject().getId().toString());
						map.put("name", line.getProject().getFullName());
						if(line.getProject().getProject() != null){
							map.put("projectName", line.getProject().getProject().getFullName());
						}
						else{
							map.put("projectName", "");
						}
						map.put("date", date.toString());
						map.put("duration", line.getSunday().toString());
						dataList.add(map);
					}
				}
			}
			response.setData(dataList);
		}
		catch(Exception e){
			response.setStatus(-1);
			response.setError(e.getMessage());
		}
	}

}
