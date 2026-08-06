package com.investwise.user.config;

import com.investwise.user.model.Enums;
import com.investwise.user.model.Role;
import com.investwise.user.model.User;
import com.investwise.user.repository.jpa.RoleRepository;
import com.investwise.user.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds roles and the demo accounts on first start.
 * Idempotent, so it is safe against a database already populated by seed.sql.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    /** Everything a seeded account needs, so the loop below stays readable. */
    private record Demo(String first, String last, String email, String password, String phone,
                        LocalDate dob, Enums.Gender gender, String pan, String income,
                        String occupation, String city, String state, String pincode,
                        Enums.Tier tier, boolean admin) { }

    @Override
    @Transactional
    public void run(String... args) {
        List.of("ROLE_USER", "ROLE_ADMIN").forEach(name ->
                roles.findByName(name).orElseGet(() -> roles.save(new Role(name))));

        List<Demo> demos = List.of(
                new Demo("Aarav", "Mehta", "admin@investwise.in", "Admin@123", "9876500001",
                        LocalDate.of(1988, 4, 12), Enums.Gender.MALE, "ABCPE1234F", "2500000",
                        "Platform Administrator", "Pune", "Maharashtra", "411001",
                        Enums.Tier.PREMIUM, true),
                new Demo("Rahul", "Sharma", "rahul.sharma@example.com", "User@123", "9876500002",
                        LocalDate.of(1995, 9, 23), Enums.Gender.MALE, "BCDPA2345G", "900000",
                        "Software Engineer", "Bengaluru", "Karnataka", "560001",
                        Enums.Tier.FREE, false),
                new Demo("Priya", "Nair", "priya.nair@example.com", "User@123", "9876500003",
                        LocalDate.of(1992, 1, 8), Enums.Gender.FEMALE, "CDEPB3456H", "1800000",
                        "Product Manager", "Mumbai", "Maharashtra", "400001",
                        Enums.Tier.PREMIUM, false));

        demos.stream()
             .filter(demo -> !users.existsByEmailIgnoreCase(demo.email()))
             .forEach(demo -> {
                 Set<Role> assigned = new LinkedHashSet<>();
                 assigned.add(roles.findByName("ROLE_USER").orElseThrow());
                 if (demo.admin()) {
                     assigned.add(roles.findByName("ROLE_ADMIN").orElseThrow());
                 }
                 users.save(User.builder()
                         .firstName(demo.first()).lastName(demo.last()).email(demo.email())
                         .password(passwordEncoder.encode(demo.password()))
                         .phone(demo.phone()).dateOfBirth(demo.dob()).gender(demo.gender())
                         .panNumber(demo.pan()).annualIncome(new BigDecimal(demo.income()))
                         .occupation(demo.occupation()).city(demo.city()).state(demo.state())
                         .pincode(demo.pincode())
                         .emailVerified(true).status(Enums.Status.ACTIVE).tier(demo.tier())
                         .roles(assigned).build());
                 log.info("Seeded {} ({})", demo.email(), demo.password());
             });
    }
}
