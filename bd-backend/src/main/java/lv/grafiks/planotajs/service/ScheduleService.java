package lv.grafiks.planotajs.service;

import lv.grafiks.planotajs.model.Schedule;
import lv.grafiks.planotajs.model.Shift;
import lv.grafiks.planotajs.repository.ScheduleRepository;
import lv.grafiks.planotajs.repository.ShiftRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    private ScheduleRepository scheduleRepository;
    private ShiftRepository shiftRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, ShiftRepository shiftRepository) {
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
    }

    public Schedule save(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    public Schedule getById(Long id) {
        return scheduleRepository.findById(id).orElseThrow(() -> new RuntimeException("Nav atrasts"));
    }

    public Schedule getByYearAndMonth(int year, int month) {
        return scheduleRepository.findByYearAndMonth(year, month).orElseThrow(() -> new RuntimeException("Nav atrasts"));
    }

    public List<Shift> getShiftsBySchedule(Long scheduleId) {
        return shiftRepository.findByScheduleId(scheduleId);
    }

    public Shift saveShift(Shift shift) {
        return shiftRepository.save(shift);
    }

    public void deleteShift(Long shiftId) {
        shiftRepository.deleteById(shiftId);
    }
}
