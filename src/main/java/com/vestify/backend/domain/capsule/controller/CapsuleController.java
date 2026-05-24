package com.vestify.backend.domain.capsule.controller;

import com.vestify.backend.domain.capsule.dto.CapsuleRequest;
import com.vestify.backend.domain.capsule.dto.CapsuleResponse;
import com.vestify.backend.domain.capsule.service.CapsuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleController {

    private final CapsuleService capsuleService;

    public CapsuleController(CapsuleService capsuleService) {
        this.capsuleService = capsuleService;
    }

    @PostMapping("/generate")
    public ResponseEntity<CapsuleResponse> generateCapsule(@RequestBody CapsuleRequest request) {
        CapsuleResponse response = capsuleService.generateSmartCapsule(request);
        return ResponseEntity.ok(response);
    }
}