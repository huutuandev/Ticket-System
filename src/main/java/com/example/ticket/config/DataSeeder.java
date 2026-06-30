package com.example.ticket.config;

import com.example.ticket.entity.Role;
import com.example.ticket.entity.User;
import com.example.ticket.repository.RoleRepository;
import com.example.ticket.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner init(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepo.count() == 0) {
                User u = new User();
//                u.setUsername("test");
                u.setEmail("test@gmail.com");
                u.setFullName("Test User");
                u.setPassword(passwordEncoder.encode("123"));
                
                Set<Role> roles = new HashSet<>();
                roleRepo.findByName("USER").ifPresent(roles::add);
                u.setRoles(roles);

                userRepo.save(u);
            }
        };
    }
}
