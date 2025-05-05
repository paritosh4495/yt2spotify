package com.nexon.yt2spotify.controller;

import com.nexon.yt2spotify.dto.yt.YoutubePlaylistItemDto;
import com.nexon.yt2spotify.dto.yt.YoutubeSimplifiedPlaylistDto;
import com.nexon.yt2spotify.service.AuthorizationHelper;
import com.nexon.yt2spotify.service.YoutubeApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeController {
    private final YoutubeApiService youtubeApiService;
    private final AuthorizationHelper authorizationHelper; // Use the helper

    @GetMapping("/playlists")
    public ResponseEntity<?> getCurrentUserPlaylists(Authentication authentication) {
        log.info("Request received for /api/youtube/playlists by user {}", (authentication != null ? authentication.getName() : "UNKNOWN"));
        try {
            // 1. Get Google Token using Helper
            String accessToken = authorizationHelper.getAccessToken(authentication, "google");

            // 2. Call Service with Token
            List<YoutubeSimplifiedPlaylistDto> playlists = youtubeApiService.getCurrentUserPlaylists(accessToken);
            return ResponseEntity.ok(playlists);

        } catch (Exception e) {
            log.error("Error getting YouTube playlists for user {}: {}", (authentication != null ? authentication.getName() : "UNKNOWN"), e.getMessage(), e);
            // Specific check for authorization errors from helper
            if (e instanceof RuntimeException && e.getMessage().contains("authorization failed") || e instanceof org.springframework.security.oauth2.client.ClientAuthorizationRequiredException) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization failed: " + e.getMessage());
            }
            return ResponseEntity.internalServerError().body("Error fetching YouTube playlists: " + e.getMessage());
        }
    }

    @GetMapping("/playlists/{playlistId}/items")
    public ResponseEntity<?> getPlaylistItems(
            Authentication authentication,
                @PathVariable String playlistId){
        log.info("Request received for /api/youtube/playlists/{}/items by user {}", playlistId, (authentication != null ? authentication.getName() : "UNKNOWN"));
        try {
            if (playlistId == null || playlistId.isBlank()) {
                return ResponseEntity.badRequest().body("Playlist ID cannot be blank.");
            }
            // 1. Get Google Token using Helper
            String accessToken = authorizationHelper.getAccessToken(authentication, "google");

            // 2. Call Service with Token
            List<YoutubePlaylistItemDto> items = youtubeApiService.getPlaylistItems(playlistId, accessToken);
            return ResponseEntity.ok(items);

        } catch (Exception e) {
            log.error("Error getting YouTube playlist items for user {} and playlist {}: {}",
                    (authentication != null ? authentication.getName() : "UNKNOWN"), playlistId, e.getMessage(), e);
            if (e instanceof RuntimeException && e.getMessage().contains("authorization failed") || e instanceof org.springframework.security.oauth2.client.ClientAuthorizationRequiredException) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization failed: " + e.getMessage());
            }
            return ResponseEntity.internalServerError().body("Error fetching YouTube playlist items: " + e.getMessage());
        }
    }
}
