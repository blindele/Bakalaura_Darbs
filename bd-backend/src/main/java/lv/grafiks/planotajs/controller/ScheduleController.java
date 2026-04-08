package lv.grafiks.planotajs.controller;

import lv.grafiks.planotajs.model.Schedule;
import lv.grafiks.planotajs.model.Shift;
import lv.grafiks.planotajs.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public Schedule create(@RequestBody Schedule schedule) {
        return scheduleService.save(schedule);
    }

    @GetMapping("/{id}")
    public Schedule getById(@PathVariable long id) {
        return scheduleService.getById(id);
    }

    @GetMapping("/{year}/{month}")
    public Schedule getByYearAndMonth(@PathVariable int year, @PathVariable int month) {
        return scheduleService.getByYearAndMonth(year, month);
    }

    @GetMapping("/{id}/shifts")
    public List<Shift> getShifts(@PathVariable long id) {
        return scheduleService.getShiftsBySchedule(id);
    }

    @GetMapping("/shifts")
    public Shift createShift(@RequestBody Shift shift) {
        return scheduleService.saveShift(shift);
    }

    @DeleteMapping("/shifts/{id}")
    public void deleteShift(@PathVariable long id) {
        scheduleService.deleteShift(id);
    }



}
