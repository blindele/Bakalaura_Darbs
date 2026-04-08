package lv.grafiks.planotajs.controller;

import lv.grafiks.planotajs.model.MonthNorm;
import lv.grafiks.planotajs.service.MonthNormService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/norms")
public class MonthNormController {

    private MonthNormService monthNormService;

    public MonthNormController(MonthNormService monthNormService) {
        this.monthNormService = monthNormService;
    }

    @GetMapping
    public List<MonthNorm> getAll(){
        return monthNormService.getAll();
    }

    @GetMapping("/{year}/{month}")
    public MonthNorm getByYearAndMonth(@PathVariable int year, @PathVariable int month) {
        return monthNormService.getByYearAndMonth(year, month);
    }

    @PostMapping
    public MonthNorm create(@RequestBody MonthNorm monthNorm) {
        return monthNormService.save(monthNorm);
    }



}
