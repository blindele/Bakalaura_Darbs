package lv.grafiks.planotajs.controller;


import lv.grafiks.planotajs.dto.ChangePasswordRequest;
import lv.grafiks.planotajs.dto.LoginRequest;
import lv.grafiks.planotajs.dto.LoginResponse;
import lv.grafiks.planotajs.model.User;
import lv.grafiks.planotajs.repository.UserRepository;
import lv.grafiks.planotajs.service.AuthService;
import lv.grafiks.planotajs.service.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, JwtService jwtService, UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/profile")
    public Map<String, Object> getCurrentUser(jakarta.servlet.http.HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        String email = jwtService.extractEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lietotājs nav atrasts"));

        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        if(user.getEmployee() != null) {
            response.put("name", user.getEmployee().getName());
            response.put("surname", user.getEmployee().getSurname());
        }


        return response;

    }

    @PutMapping("change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request,
                               jakarta.servlet.http.HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        String email = jwtService.extractEmail(token);
        authService.changePassword(email,request);
    }

}
