package lv.grafiks.planotajs.controller;


import lv.grafiks.planotajs.model.DayOffRequest;
import lv.grafiks.planotajs.model.DayOffStatus;
import lv.grafiks.planotajs.repository.DayOffRequestRepository;
import lv.grafiks.planotajs.service.DayOffRequestService;
import lv.grafiks.planotajs.service.JwtService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dayoff")
public class DayOffRequestController {

    private final DayOffRequestService dayOffRequestService;
    private final JwtService jwtService;
    private final DayOffRequestRepository dayOffRequestRepository;

    public DayOffRequestController(DayOffRequestService dayOffRequestService, JwtService jwtService, DayOffRequestRepository dayOffRequestRepository) {
        this.dayOffRequestService = dayOffRequestService;
        this.jwtService = jwtService;
        this.dayOffRequestRepository = dayOffRequestRepository;
    }

    @PostMapping
    public DayOffRequest create(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String email = jwtService.extractEmail(token);
        LocalDate date = LocalDate.parse(body.get("date"));
        return dayOffRequestService.create(email, date);
    }

    @GetMapping("/my-requests")
    public List<DayOffRequest> getMyRequests(jakarta.servlet.http.HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String email = jwtService.extractEmail(token);
        return dayOffRequestService.getMyRequests(email);
    }

    @GetMapping("/pending")
    public List<DayOffRequest> getPending() {
        return dayOffRequestService.getPendingRequests();
    }

    @PutMapping("/{id}")
    public DayOffRequest updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body){
        DayOffStatus status = DayOffStatus.valueOf(body.get("status"));
        return dayOffRequestService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        dayOffRequestRepository.deleteById(id);
    }

}
