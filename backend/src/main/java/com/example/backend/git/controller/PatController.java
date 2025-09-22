package com.example.backend.git.controller;

import com.example.backend.git.dto.PatStatusResponse;
import com.example.backend.git.dto.SetPatRequest;
import com.example.backend.git.service.PatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pat")
public class PatController {

        private final PatService patService;

        public PatController(PatService patService) {
                this.patService = patService;
        }

        @GetMapping
        public PatStatusResponse getStatus() {
                return patService.readPat().map(value -> new PatStatusResponse(true, patService.maskPat(value)))
                                .orElseGet(() -> new PatStatusResponse(false, null));
        }

        @PostMapping
        public PatStatusResponse update(@Valid @RequestBody SetPatRequest request) {
                patService.updatePat(request.pat());
                return new PatStatusResponse(true, patService.maskPat(request.pat()));
        }
}
