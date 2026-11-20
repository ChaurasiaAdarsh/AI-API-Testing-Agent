package com.apitest.agent.controller;

import com.apitest.agent.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/demo")
@Tag(name = "Demo API", description = "Demo API with intentional defects for testing demonstration")
@Slf4j
public class DemoController {

    private final AppProperties appProperties;

    // In-memory store
    private static final Map<Long, DemoUser> USERS = new ConcurrentHashMap<>();
    private static final AtomicLong ID_COUNTER = new AtomicLong(1);

    public DemoController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostMapping("/users")
    @Operation(summary = "Create a user (Demo - has intentional defect: returns 200 instead of 201 when defects enabled)")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody CreateDemoUserRequest request) {
        DemoUser user = new DemoUser();
        user.setId(ID_COUNTER.getAndIncrement());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setRole(request.getRole());
        user.setBio(request.getBio());
        user.setCreatedAt(LocalDateTime.now());
        USERS.put(user.getId(), user);

        Map<String, Object> response = userToMap(user, false);

        // INTENTIONAL DEFECT: Return 200 instead of 201 when defects enabled
        if (appProperties.getDemo().isDefectsEnabled()) {
            log.debug("Demo defect: returning 200 instead of 201 for user creation");
            return ResponseEntity.ok(response);  // Contract declares 201 Created
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String role) {

        List<DemoUser> users = new ArrayList<>(USERS.values());
        if (role != null) {
            users = users.stream().filter(u -> role.equalsIgnoreCase(u.getRole())).toList();
        }

        int total = users.size();
        List<DemoUser> paged = users.stream().skip((long) page * size).limit(size).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("users", paged.stream().map(u -> userToMap(u, false)).toList());
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID (correct behavior)")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {
        DemoUser user = USERS.get(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorMap(404, "User not found", "User with id " + id + " not found"));
        }
        return ResponseEntity.ok(userToMap(user, false));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user (Demo - has intentional defect: missing updatedAt in response)")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDemoUserRequest request) {
        DemoUser user = USERS.get(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorMap(404, "User not found", "User with id " + id + " not found"));
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getRole() != null) user.setRole(request.getRole());
        user.setUpdatedAt(LocalDateTime.now());
        USERS.put(id, user);

        // INTENTIONAL DEFECT: Missing updatedAt field when defects enabled
        return ResponseEntity.ok(userToMap(user, appProperties.getDemo().isDefectsEnabled()));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user (correct behavior: returns 204)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!USERS.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        USERS.remove(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> userToMap(DemoUser user, boolean omitUpdatedAt) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("age", user.getAge());
        map.put("role", user.getRole());
        map.put("bio", user.getBio());
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        if (!omitUpdatedAt) {
            map.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        }
        return map;
    }

    private Map<String, Object> errorMap(int status, String error, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", LocalDateTime.now().toString());
        map.put("status", status);
        map.put("error", error);
        map.put("message", message);
        return map;
    }

    @Data
    public static class CreateDemoUserRequest {
        @NotBlank private String name;
        @NotBlank private String email;
        @NotNull @Min(18) @Max(120) private Integer age;
        @NotBlank private String role;
        private String bio;
    }

    @Data
    public static class UpdateDemoUserRequest {
        private String name;
        private String bio;
        private String role;
    }

    @Data
    public static class DemoUser {
        private Long id;
        private String name;
        private String email;
        private Integer age;
        private String role;
        private String bio;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
