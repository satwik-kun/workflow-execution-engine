package workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                .requestMatchers("/api/instances/*/approve", "/api/instances/*/reject").hasRole("MANAGER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
        @Value("${app.security.user.employee.username}") String employeeUser,
        @Value("${app.security.user.employee.password}") String employeePassword,
        @Value("${app.security.user.manager.username}") String managerUser,
        @Value("${app.security.user.manager.password}") String managerPassword,
        @Value("${app.security.user.operations.username}") String operationsUser,
        @Value("${app.security.user.operations.password}") String operationsPassword,
        PasswordEncoder passwordEncoder
    ) {
        UserDetails employee = User.withUsername(employeeUser)
            .password(passwordEncoder.encode(employeePassword))
            .roles("EMPLOYEE")
            .build();

        UserDetails manager = User.withUsername(managerUser)
            .password(passwordEncoder.encode(managerPassword))
            .roles("MANAGER", "EMPLOYEE")
            .build();

        UserDetails operations = User.withUsername(operationsUser)
            .password(passwordEncoder.encode(operationsPassword))
            .roles("OPERATIONS", "EMPLOYEE")
            .build();

        return new InMemoryUserDetailsManager(employee, manager, operations);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
