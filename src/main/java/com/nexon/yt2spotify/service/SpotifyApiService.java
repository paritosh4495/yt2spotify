package com.nexon.yt2spotify.service;

import com.nexon.yt2spotify.dto.sotify.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyApiService {

    private final WebClient webClient;
    private final String SPOTIFY_API_BASE_URL = "https://api.spotify.com/v1"; // Correct base URL


    /**
     * Searches Spotify for a track based on a query string.
     * Attempts to find the best single match.
     *
     * @param query       The search query (e.g., "Video Title" or "Track Name Artist Name").
     * @param accessToken The user's Spotify OAuth access token.
     * @return An Optional containing the SpotifyTrackDto if found, otherwise empty Optional.
     */
    public Optional<SpotifyTrackDto> searchTrack(String query, String accessToken) {
        // Basic query cleaning (can be improved)
        String cleanedQuery = query.replaceAll("(?i)\\b(official music video|music video|official video|video|lyrics|lyric video)\\b", "")
                .replaceAll("[\\(\\)\\[\\]\\{\\}]", "") // Remove brackets
                .trim();
        log.info("Searching Spotify for track with cleaned query: '{}'", cleanedQuery);

        // Request only necessary fields for the track item
        final String fields = "tracks.items(id,name,uri,artists(name))";
        final int limit = 1; // Only request the top result

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SPOTIFY_API_BASE_URL + "/search")
                    .queryParam("q", cleanedQuery)
                    .queryParam("type", "track") // Search only for tracks
                    .queryParam("limit", limit)   // Limit to 1 result
                    .queryParam("fields", fields); // Request minimal fields

            String url = uriBuilder.encode().toUriString();
            log.debug("Requesting Spotify search from URL: {}", url);

            // Directly deserialize into the tracks wrapper DTO
            SpotifyTracksSearchResultDto searchResult = this.webClient.get()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(SpotifyTracksSearchResultDto.class)
                    .block();

            if (searchResult != null &&
                    searchResult.getTracks() != null &&
                    searchResult.getTracks().getItems() != null &&
                    !searchResult.getTracks().getItems().isEmpty())
            {
                // Return the first track found
                SpotifyTrackDto foundTrack = searchResult.getTracks().getItems().get(0);
                log.info("Found Spotify track for query '{}': ID={}, Name='{}'", cleanedQuery, foundTrack.getId(), foundTrack.getName());
                return Optional.of(foundTrack);
            } else {
                log.warn("No Spotify track found for query: '{}'", cleanedQuery);
                return Optional.empty();
            }

        } catch (WebClientResponseException wcre) {
            log.error("WebClient Error searching Spotify for query '{}': Status {}, Body {}", cleanedQuery, wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            // Don't throw, failing to find is acceptable
        } catch (Exception e) {
            log.error("Generic Error searching Spotify for query '{}': {}", cleanedQuery, e.getMessage(), e);
            // Don't throw
        }

        return Optional.empty(); // Return empty if error or no results
    }

    // --- Placeholders for Spotify write methods (getCurrentSpotifyUser, createPlaylist, addTracksToPlaylist) --
    //-`

    // ... existing searchTrack, getPlaylistTracks etc ...

    // --- NEW METHOD: Get Current Spotify User ID ---
    /**
     * Gets the profile of the user associated with the access token, primarily to get their ID.
     * @param accessToken The user's Spotify OAuth access token.
     * @return SpotifyUserDto containing the user's ID.
     */
    public SpotifyUserDto getCurrentSpotifyUser(String accessToken) {
        log.info("Fetching current Spotify user profile (for ID)...");
        String url = SPOTIFY_API_BASE_URL + "/me?fields=id"; // Only request the ID

        try {
            SpotifyUserDto userDto = this.webClient.get()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(SpotifyUserDto.class)
                    .block();

            if (userDto != null && userDto.getId() != null) {
                log.debug("Fetched Spotify user ID: {}", userDto.getId());
                return userDto;
            } else {
                log.error("Could not fetch current Spotify user ID. Response was null or missing ID.");
                throw new RuntimeException("Could not fetch current Spotify user ID.");
            }
        } catch (WebClientResponseException wcre) {
            log.error("WebClient Error fetching Spotify user profile: Status {}, Body {}", wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            throw new RuntimeException("API Error fetching Spotify user profile: " + wcre.getMessage(), wcre);
        } catch (Exception e) {
            log.error("Generic Error fetching Spotify user profile: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching Spotify user profile: " + e.getMessage(), e);
        }
    }

    // --- NEW METHOD: Create Spotify Playlist ---
    /**
     * Creates a new playlist for a given Spotify user.
     * @param userId The Spotify User ID of the playlist owner.
     * @param name The name for the new playlist.
     * @param description The description for the new playlist.
     * @param isPublic Should the playlist be public (true) or private (false).
     * @param accessToken The user's Spotify OAuth access token.
     * @return SpotifyPlaylistDto representing the newly created playlist.
     */
    public SpotifyPlaylistDto createPlaylist(String userId, String name, String description, boolean isPublic, String accessToken) {
        log.info("Creating Spotify playlist '{}' for user ID: {}", name, userId);
        String url = SPOTIFY_API_BASE_URL + "/users/" + userId + "/playlists";

        SpotifyCreatePlaylistRequestDto requestBody = new SpotifyCreatePlaylistRequestDto(name, description, isPublic, false); // Name, Desc, Public, Collaborative

        try {
            // Reusing SpotifyPlaylistDto as the response structure matches reasonably well
            SpotifyPlaylistDto createdPlaylist = this.webClient.post()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(SpotifyPlaylistDto.class) // Expect a Playlist object back
                    .block();

            if (createdPlaylist != null && createdPlaylist.getId() != null) {
                log.info("Successfully created Spotify playlist '{}' with ID: {}", createdPlaylist.getName(), createdPlaylist.getId());
                return createdPlaylist;
            } else {
                log.error("Failed to create Spotify playlist '{}'. Response or ID was null.", name);
                throw new RuntimeException("Failed to create Spotify playlist '" + name + "'. Response or ID was null.");
            }

        } catch (WebClientResponseException wcre) {
            log.error("WebClient Error creating Spotify playlist '{}': Status {}, Body {}", name, wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            throw new RuntimeException("API Error creating Spotify playlist '" + name + "': " + wcre.getMessage(), wcre);
        } catch (Exception e) {
            log.error("Generic Error creating Spotify playlist '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Error creating Spotify playlist '" + name + "': " + e.getMessage(), e);
        }
    }

    // --- NEW METHOD: Add Tracks to Spotify Playlist ---
    /**
     * Adds tracks to a specific Spotify playlist. Handles batching (max 100 per request).
     * @param playlistId The ID of the target Spotify playlist.
     * @param trackUris A List of Spotify Track URIs (e.g., "spotify:track:xxxx").
     * @param accessToken The user's Spotify OAuth access token.
     */
    public void addTracksToPlaylist(String playlistId, List<String> trackUris, String accessToken) {
        if (trackUris == null || trackUris.isEmpty()) {
            log.warn("No track URIs provided to add to playlist {}", playlistId);
            return;
        }
        log.info("Attempting to add {} tracks to Spotify playlist ID: {}", trackUris.size(), playlistId);
        String url = SPOTIFY_API_BASE_URL + "/playlists/" + playlistId + "/tracks";
        int batchSize = 100; // Spotify API limit

        // Process in batches of 100
        for (int i = 0; i < trackUris.size(); i += batchSize) {
            int end = Math.min(i + batchSize, trackUris.size());
            List<String> batch = trackUris.subList(i, end);
            Map<String, Object> requestBody = Collections.singletonMap("uris", batch);
            log.debug("Adding batch of {} tracks ({} - {}) to playlist {}", batch.size(), i+1, end, playlistId);

            try {
                SpotifySnapshotResponseDto response = this.webClient.post()
                        .uri(url)
                        .headers(h -> h.setBearerAuth(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(SpotifySnapshotResponseDto.class)
                        .block(); // Block for simplicity, could make reactive

                if (response != null && response.getSnapshot_id() != null) {
                    log.debug("Successfully added batch ({} tracks) to playlist {}. Snapshot ID: {}", batch.size(), playlistId, response.getSnapshot_id());
                } else {
                    log.warn("Adding batch ({} tracks) to playlist {} might have failed or returned unexpected response.", batch.size(), playlistId);
                    // Decide if we should stop or continue on partial failure
                }
                // Optional: Add delay between batches if hitting rate limits
                // if (end < trackUris.size()) { try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); } }

            } catch (WebClientResponseException wcre) {
                log.error("WebClient Error adding tracks batch to playlist {}: Status {}, Body {}", playlistId, wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
                // Decide if we should stop or continue on batch failure
                throw new RuntimeException("API Error adding tracks to playlist " + playlistId + ": " + wcre.getMessage(), wcre); // Rethrow for now
            } catch (Exception e) {
                log.error("Generic Error adding tracks batch to playlist {}: {}", playlistId, e.getMessage(), e);
                // Decide if we should stop or continue on batch failure
                throw new RuntimeException("Error adding tracks to playlist " + playlistId + ": " + e.getMessage(), e); // Rethrow for now
            }
        } // End batch loop
        log.info("Finished adding all batches (total {} URIs) to playlist {}", trackUris.size(), playlistId);
    }

}
