package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.*;
import com.example.EV_finder_api.entity.User;
import com.example.EV_finder_api.exception.UnauthorizedException;
import com.example.EV_finder_api.repository.UserRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/** Profile self-service — works for every role (USER / OPERATOR / ADMIN). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          CurrentUserProvider currentUserProvider,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public UserMeResponse me() {
        return UserMeResponse.from(currentUserProvider.getCurrentUser());
    }

    @PutMapping("/me")
    public UserMeResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUserProvider.getCurrentUser();
        user.setName(request.name());
        user.setPhone(request.phone());
        return UserMeResponse.from(userRepository.save(user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }
}
