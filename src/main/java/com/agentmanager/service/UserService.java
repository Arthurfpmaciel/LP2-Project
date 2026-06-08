package com.agentmanager.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import com.agentmanager.dto.UserRequest;
import com.agentmanager.dto.UserResponse;
import com.agentmanager.exception.BusinessException;
import com.agentmanager.exception.ResourceNotFoundException;
import com.agentmanager.model.User;
import com.agentmanager.model.PlanType;
import com.agentmanager.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ja existe um usuario com este email.");
        }

        User user = new User();
        applyRequest(user, request);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = getEntity(id);
        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Ja existe um usuario com este email.");
                });

        applyRequest(user, request);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = getEntity(id);
        userRepository.delete(user);
    }

    public User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + id));
    }

    public List<UserResponse> listByPlanType(PlanType planType, int limit){
        if (limit <= 0 || limit>100){
            throw new BusinessException("O limite deve ser > 0 e <= 100");
        }
        return userRepository.findByPlanType(planType, PageRequest.of(0,limit))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private void applyRequest(User user, UserRequest request) {
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPlanType(request.planType());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPlanType(),
                user.getCreatedAt()
        );
    }
}
