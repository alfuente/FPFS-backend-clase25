package com.shopeasy.config;

import com.shopeasy.model.Role;
import com.shopeasy.model.User;
import com.shopeasy.repository.RoleRepository;
import com.shopeasy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Crea datos iniciales al arrancar la aplicacion:
 * - Roles ROLE_ADMIN y ROLE_USER
 * - Usuario administrador por defecto (admin@shopeasy.com / admin123)
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Crear roles si no existen
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

        // Crear usuario administrador por defecto si no existe
        if (!userRepository.existsByEmail("admin@shopeasy.com")) {
            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@shopeasy.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            System.out.println(">>> Admin creado: admin@shopeasy.com / admin123");
        }
    }
}
