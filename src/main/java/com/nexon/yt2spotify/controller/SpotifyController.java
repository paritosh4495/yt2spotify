package com.nexon.yt2spotify.controller;

import com.nexon.yt2spotify.dto.sotify.SpotifyTrackDto;
import com.nexon.yt2spotify.service.AuthorizationHelper;
import com.nexon.yt2spotify.service.SpotifyApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.ILoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyApiService spotifyApiService;
    private final AuthorizationHelper authorizationHelper; // Inject helper

    @GetMapping("/search")
    public ResponseEntity<?> searchSpotifyTrack(


            @RequestParam String query,
            Authentication authentication) {

        log.info("Request received for /api/spotify/search?query={} by user {}", query, (authentication != null ? authentication.getName() : "UNKNOWN"));
        try {
            if (query == null || query.isBlank()) {
                return ResponseEntity.badRequest().body("Query parameter cannot be blank.");
            }
            // 1. Get Spotify Token using Helper
            String accessToken = authorizationHelper.getAccessToken(authentication, "spotify");
            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Spotify authorization failed or token missing.");
            }

            // 2. Call Service with Token
            Optional<SpotifyTrackDto> trackOptional = spotifyApiService.searchTrack(query, accessToken);

            // 3. Return result or 404
            return trackOptional.map(ResponseEntity::ok) // If present, wrap in 200 OK
                    .orElseGet(() -> ResponseEntity.notFound().build()); // If empty, return 404

        } catch (Exception e) {
            log.error("Error searching Spotify for user {}: {}", (authentication != null ? authentication.getName() : "UNKNOWN"), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error during search: " + e.getMessage());
        }
    }
}
