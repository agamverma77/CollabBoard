package com.example.collabboard.service;

import com.example.collabboard.model.User;
import com.example.collabboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpiryStorage = new ConcurrentHashMap<>();

    
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void registerUser(String username, String email, String password) throws Exception {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new Exception("Username already exists.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new Exception("Email address is already registered.");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }


    public Optional<User> loginUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * @param email 
     * @throws Exception 
     */
    public void generateAndSendOtp(String email) throws Exception {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("No user found with this email address."));

        String otp = new Random().ints(1, 100000, 999999)
                .findFirst()
                .getAsInt() + "";

        otpStorage.put(email, otp);
        otpExpiryStorage.put(email, LocalDateTime.now().plusMinutes(10)); 

        emailService.sendOtpEmail(email, otp);
    }

    /**

     
     * @param email       
     * @param otp         
     * @param newPassword 
     * @throws Exception 
     */
    public void resetPassword(String email, String otp, String newPassword) throws Exception {
        if (!verifyOtp(email, otp)) {
            throw new Exception("Invalid or expired OTP. Please try again.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("An unexpected error occurred. User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpStorage.remove(email);
        otpExpiryStorage.remove(email);
    }

    
    private boolean verifyOtp(String email, String otp) {
        
        boolean isOtpValid = otp != null && otp.equals(otpStorage.get(email));
        if (!isOtpValid) {
            return false;
        }

       
        LocalDateTime expiryTime = otpExpiryStorage.get(email);
        return expiryTime != null && expiryTime.isAfter(LocalDateTime.now());
    }
}

