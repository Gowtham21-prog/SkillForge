package com.elearning.controller;

import com.elearning.dto.AdminDtos.InstructorEarnings;
import com.elearning.service.EarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/instructor")
@RequiredArgsConstructor
public class InstructorController {

    private final EarningsService earningsService;

    @GetMapping("/earnings")
    public ResponseEntity<InstructorEarnings> getEarnings(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(earningsService.getEarningsForInstructor(userDetails.getUsername()));
    }
}
