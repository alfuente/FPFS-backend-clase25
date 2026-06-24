package com.shopeasy.service.impl;

import com.shopeasy.dto.request.LoginRequest;
import com.shopeasy.dto.request.RegisterRequest;
import com.shopeasy.dto.response.AuthResponse;
import com.shopeasy.exception.ResourceNotFoundException;
import com.shopeasy.model.Role;
import com.shopeasy.model.User;
import com.shopeasy.repository.RoleRepository;
import com.shopeasy.repository.UserRepository;
import com.shopeasy.security.JwtUtil;
import com.shopeasy.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getName(), "ROLE_USER");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(user.getEmail());

        String primaryRole = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("ROLE_USER");

        return new AuthResponse(token, user.getEmail(), user.getName(), primaryRole);
    }
}
