package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Role;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.entity.UserStatus;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getById(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı"));
    }

    @Transactional
    public void updateProfile(Integer id, String name, String surname, String companyName, String phone) {
        User u = getById(id);
        u.setName(name);
        u.setSurname(surname);
        u.setCompanyName(companyName);
        u.setPhone(phone);
        userRepository.save(u);
    }

    @Transactional
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        User u = getById(id);
        if (u.getPasswordHash() != null && !passwordEncoder.matches(currentPassword, u.getPasswordHash())) {
            throw new ConflictException("Mevcut şifre hatalı");
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    // ---- admin ----

    public List<User> listUsers(String role) {
        if (role == null || role.isBlank()) return userRepository.findAll();
        return userRepository.findByRole(Role.valueOf(role), PageRequest.of(0, 500)).getContent();
    }

    public List<User> listCustomers() {
        return userRepository.findByRole(Role.CUSTOMER, PageRequest.of(0, 500)).getContent();
    }

    public long countCustomers() {
        return userRepository.countByRole(Role.CUSTOMER);
    }

    @Transactional
    public User createUser(String email, String username, String password, String role, String name, String surname) {
        if (userRepository.existsByEmail(email)) throw new ConflictException("Bu e-posta zaten kayıtlı");
        if (username != null && !username.isBlank() && userRepository.existsByUsername(username)) {
            throw new ConflictException("Bu kullanıcı adı zaten kayıtlı");
        }
        User u = new User();
        u.setEmail(email);
        u.setUsername(username != null && !username.isBlank() ? username : null);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(Role.valueOf(role));
        u.setStatus(UserStatus.ACTIVE);
        u.setName(name);
        u.setSurname(surname);
        return userRepository.save(u);
    }

    @Transactional
    public void updateRole(Integer id, String role) {
        User u = getById(id);
        u.setRole(Role.valueOf(role));
        userRepository.save(u);
    }

    @Transactional
    public void updateStatus(Integer id, String status) {
        User u = getById(id);
        u.setStatus(UserStatus.valueOf(status));
        userRepository.save(u);
    }

    @Transactional
    public void deleteUser(Integer id) {
        userRepository.delete(getById(id));
    }
}
