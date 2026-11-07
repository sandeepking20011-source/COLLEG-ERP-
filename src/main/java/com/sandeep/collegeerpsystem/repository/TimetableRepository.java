package com.sandeep.collegeerpsystem.repository;

import com.sandeep.collegeerpsystem.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    List<Timetable> findByTeacherId(Integer teacherId);

    List<Timetable> findByDayOfWeekIgnoreCaseOrderByStartTimeAsc(String dayOfWeek);
}
