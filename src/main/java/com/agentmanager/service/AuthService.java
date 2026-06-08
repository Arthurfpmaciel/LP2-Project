package com.agentmanager.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.agentmanager.dto.LoginRequest;
import com.agentmanager.dto.LoginResponse;
import com.agentmanager.exception.BusinessException;
import com.agentmanager.model.User;
import com.agentmanager.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public LoginResponse login(LoginRequest request){
        User user = userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BusinessException("email ou senha inválidos."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("email ou senha inválidos");
        }    
        return new LoginResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPlanType().name()
        );
    }
}
