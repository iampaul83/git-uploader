package com.example.backend.git.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AzureRepoUrlParser {

        private AzureRepoUrlParser() {
        }

        public static Optional<Result> parse(String url) {
                if (url == null || url.isBlank()) {
                        return Optional.empty();
                }
                try {
                        URI uri = URI.create(url.trim());
                        List<String> segments = Arrays.stream(uri.getPath().split("/"))
                                        .filter(segment -> !segment.isBlank())
                                        .map(segment -> URLDecoder.decode(segment, StandardCharsets.UTF_8))
                                        .toList();
                        int gitIndex = segments.indexOf("_git");
                        if (gitIndex < 1 || gitIndex + 1 >= segments.size()) {
                                return Optional.empty();
                        }
                        String project = segments.get(gitIndex - 1);
                        String repository = segments.get(gitIndex + 1);
                        Optional<String> branch = parseBranch(uri.getRawQuery());
                        return Optional.of(new Result(url.trim(), project, repository, branch.orElse(null)));
                } catch (IllegalArgumentException exception) {
                        return Optional.empty();
                }
        }

        private static Optional<String> parseBranch(String query) {
                if (query == null || query.isBlank()) {
                        return Optional.empty();
                }
                for (String parameter : query.split("&")) {
                        int separator = parameter.indexOf('=');
                        if (separator < 0) {
                                continue;
                        }
                        String key = parameter.substring(0, separator);
                        String value = parameter.substring(separator + 1);
                        if (key.equalsIgnoreCase("version") && value.startsWith("GB")) {
                                return Optional.of(decodeBranch(value.substring(2)));
                        }
                        if (key.equalsIgnoreCase("branch")) {
                                return Optional.of(decodeBranch(value));
                        }
                }
                return Optional.empty();
        }

        private static String decodeBranch(String raw) {
                return URLDecoder.decode(raw, StandardCharsets.UTF_8)
                                .replaceFirst("^refs/heads/", "");
        }

        public static String createFolderName(String project, String repository, String branch) {
                return String.join("_", sanitize(project), sanitize(repository), sanitize(branch));
        }

        private static String sanitize(String value) {
                String cleaned = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
                if (cleaned.isBlank()) {
                        return "repo";
                }
                return cleaned;
        }

        public record Result(String url, String project, String repository, String branch) {
        }
}
