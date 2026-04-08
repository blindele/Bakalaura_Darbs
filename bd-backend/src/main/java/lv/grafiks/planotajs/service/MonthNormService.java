package lv.grafiks.planotajs.service;

import lv.grafiks.planotajs.model.MonthNorm;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.MonthNormRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MonthNormService {
    private MonthNormRepository monthNormRepository;


    public MonthNormService(MonthNormRepository monthNormRepository) {
        this.monthNormRepository = monthNormRepository;
    }

    public MonthNorm getByYearAndMonth(int year, int month) {
        return monthNormRepository.findByYearAndMonth(year, month).orElseThrow(() -> new RuntimeException("Nav atrasts"));
    }

    public MonthNorm save(MonthNorm monthNorm) {
        return monthNormRepository.save(monthNorm);
    }

    public List<MonthNorm> getAll() {
        return monthNormRepository.findAll();
    }


}
