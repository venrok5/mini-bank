package com.example.mini_bank.controllers;

import org.springframework.web.bind.annotation.*;

import com.example.mini_bank.dto.LoginRequestDto;
import com.example.mini_bank.dto.RegisterRequestDto;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.Roles;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.exception.AuthException;
import com.example.mini_bank.exception.ErrorResponse;
import com.example.mini_bank.security.jwt.JwtResponseDto;
import com.example.mini_bank.security.jwt.JwtUtils;
import com.example.mini_bank.security.userdetails.BankUserDetails;
import com.example.mini_bank.service.UserService;
import com.example.mini_bank.service.ValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "Authentication and registration API")
public class AuthController extends BaseController {

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ValidationService validationService;

    @Operation(summary = "User authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token returned",
                    content = @Content(schema = @Schema(implementation = JwtResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginRequestDto req) { 
        logRequest("Login attempt for email: {}", req.getEmail());
        
        try {
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
            
            BankUserDetails userDetails = (BankUserDetails) auth.getPrincipal();
            JwtResponseDto response = new JwtResponseDto(jwtUtils.generateToken(userDetails), userDetails.getId());
            
            logResponse("Login successful for userId: {}", userDetails.getId());
            
            return ResponseEntity.ok(response); 
            
        } catch (BadCredentialsException e) {
            logResponse("Login failed for email: {}", req.getEmail());
            
            throw new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
    }
    @Operation(summary = "New user registration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful",
                    content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Invalid registration data or user already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDto req) { 
        logRequest("Registration attempt for email: {}", req.getEmail());
        
        validationService.validateRegistration(req);

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Roles.USER); // ADMIN role only heandly adding in BD
        user.setStatus(UserStatus.ACTIVE);
        user.setUsername(req.getUsername());
        userService.saveUser(user);

        logResponse("User registered successfully: {}", req.getEmail());
        
        return ResponseEntity.ok("User registered successfully");
    }
}