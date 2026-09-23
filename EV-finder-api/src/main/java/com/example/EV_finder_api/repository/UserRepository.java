package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(com.example.EV_finder_api.entity.Role role);
}
