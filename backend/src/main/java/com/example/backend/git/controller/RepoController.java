package com.example.backend.git.controller;

import com.example.backend.git.dto.CommitRequest;
import com.example.backend.git.dto.CommitResponse;
import com.example.backend.git.dto.CreateRepoRequest;
import com.example.backend.git.dto.RepoSummaryResponse;
import com.example.backend.git.service.RepoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

        private final RepoService repoService;

        public RepoController(RepoService repoService) {
                this.repoService = repoService;
        }

        @GetMapping
        public List<RepoSummaryResponse> list() {
                return repoService.listRepositories();
        }

        @PostMapping
        public RepoSummaryResponse addRepository(@Valid @RequestBody CreateRepoRequest request) {
                return repoService.addRepository(request);
        }

        @PostMapping("/{repoId}/commit-and-push")
        public CommitResponse commitAndPush(@PathVariable String repoId, @Valid @RequestBody CommitRequest request) {
                return repoService.commitAndPush(repoId, request);
        }
}
