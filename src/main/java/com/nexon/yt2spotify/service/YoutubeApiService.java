package com.nexon.yt2spotify.service;

import com.nexon.yt2spotify.dto.yt.YoutubePlaylistItemDto;
import com.nexon.yt2spotify.dto.yt.YoutubePlaylistItemListResponseDto;
import com.nexon.yt2spotify.dto.yt.YoutubePlaylistListResponseDto;
import com.nexon.yt2spotify.dto.yt.YoutubeSimplifiedPlaylistDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeApiService {

    private final WebClient webClient;
    private final String YOUTUBE_API_BASE_URL =  "https://www.googleapis.com/youtube/v3";


    /**
     * Fetches YouTube playlists owned by the user associated with the accessToken.
     * Requests only minimal fields (id, title, itemCount) using the 'fields' parameter.
     * Handles pagination.
     *
     * @param accessToken The user's Google OAuth access token.
     * @return A list of simplified playlist DTOs.
     */

    public List<YoutubeSimplifiedPlaylistDto> getCurrentUserPlaylists(String accessToken) {

        log.info("Fetching current user Youtube playlists (minimal fields) ....");
        List<YoutubeSimplifiedPlaylistDto> allPlaylists = new ArrayList<>();
        String nextPageToken = null;
        final String fieldsToRequest = "nextPageToken,items(id,snippet(title),contentDetails(itemCount))";
        try {
            do {
                // Build URI with parameters including fields and pagination token
                UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE_URL + "/playlists")
                        .queryParam("part", "snippet,contentDetails") // Need these parts to contain the fields
                        .queryParam("mine", "true")
                        .queryParam("maxResults", 50)
                        .queryParam("fields", fieldsToRequest); // Add the fields parameter!

                if (nextPageToken != null) {
                    uriBuilder.queryParam("pageToken", nextPageToken);
                }
                String currentUrl = uriBuilder.encode().toUriString();

                log.debug("Requesting YouTube playlists from URL: {}", currentUrl);

                YoutubePlaylistListResponseDto page = this.webClient.get()
                        .uri(currentUrl)
                        .headers(h -> h.setBearerAuth(accessToken)) // Set header manually
                        .retrieve()
                        .bodyToMono(YoutubePlaylistListResponseDto.class) // Use the specific Response DTO
                        .block(); // Synchronous call

                if (page != null && page.getItems() != null) {
                    allPlaylists.addAll(page.getItems());
                    nextPageToken = page.getNextPageToken();
                    log.debug("Fetched {} playlists this page, next page token: {}", page.getItems().size(), nextPageToken);
                } else {
                    log.warn("Received null page or null items from YouTube playlists endpoint.");
                    nextPageToken = null; // Stop looping if response is weird
                }

            } while (nextPageToken != null); // Continue if there's a next page token

        } catch (WebClientResponseException wcre) {
            log.error("WebClient Error fetching YouTube playlists: Status {}, Body {}", wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            // You might want custom exceptions or different handling here
            throw new RuntimeException("API Error fetching YouTube playlists: " + wcre.getMessage(), wcre);
        } catch (Exception e) {
            log.error("Generic Error fetching YouTube playlists: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching YouTube playlists: " + e.getMessage(), e);
        }

        log.info("Successfully fetched {} YouTube playlists in total.", allPlaylists.size());
        return allPlaylists;
    }

    /**
     * Fetches the video items from a specific YouTube playlist.
     * Requests minimal fields (title, videoId, channelTitle) using the 'fields' parameter.
     * Handles pagination.
     *
     * @param youtubePlaylistId The ID of the YouTube playlist.
     * @param accessToken       The user's Google OAuth access token.
     * @return A list of playlist item DTOs.
     */

    public List<YoutubePlaylistItemDto> getPlaylistItems(String youtubePlaylistId, String accessToken) {
        log.info("Fetching items for YouTube playlist ID: {} (minimal fields)...", youtubePlaylistId);
        List<YoutubePlaylistItemDto> allItems = new ArrayList<>();
        String nextPageToken = null;
        // Specify only the fields needed for searching Spotify
        final String fieldsToRequest = "nextPageToken,items(id,snippet(title,resourceId(videoId),videoOwnerChannelTitle))";

        try {
            do {
                UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE_URL + "/playlistItems")
                        .queryParam("part", "snippet") // Need snippet to contain the fields we want
                        .queryParam("playlistId", youtubePlaylistId)
                        .queryParam("maxResults", 50)
                        .queryParam("fields", fieldsToRequest); // Add the fields parameter

                if (nextPageToken != null) {
                    uriBuilder.queryParam("pageToken", nextPageToken);
                }
                String currentUrl = uriBuilder.encode().toUriString();

                log.debug("Requesting YouTube playlist items from URL: {}", currentUrl);

                YoutubePlaylistItemListResponseDto page = this.webClient.get()
                        .uri(currentUrl)
                        .headers(h -> h.setBearerAuth(accessToken)) // Set header manually
                        .retrieve()
                        .bodyToMono(YoutubePlaylistItemListResponseDto.class) // Use the specific List Response DTO
                        .block(); // Synchronous call

                if (page != null && page.getItems() != null) {
                    // Filter out items that might not have a video ID (rare, but possible)
                    List<YoutubePlaylistItemDto> validItems = page.getItems().stream()
                            .filter(item -> item.getSnippet() != null &&
                                    item.getSnippet().getResourceId() != null &&
                                    item.getSnippet().getResourceId().getVideoId() != null)
                            .collect(Collectors.toList());

                    allItems.addAll(validItems);
                    nextPageToken = page.getNextPageToken();
                    log.debug("Fetched {} valid items this page, next page token: {}", validItems.size(), nextPageToken);
                } else {
                    log.warn("Received null page or null items from YouTube playlistItems endpoint.");
                    nextPageToken = null; // Stop looping
                }

            } while (nextPageToken != null); // Continue if there's a next page token

        } catch (WebClientResponseException wcre) {
            log.error("WebClient Error fetching items for YouTube playlist {}: Status {}, Body {}", youtubePlaylistId, wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            throw new RuntimeException("API Error fetching items for YouTube playlist " + youtubePlaylistId + ": " + wcre.getMessage(), wcre);
        } catch (Exception e) {
            log.error("Generic Error fetching items for YouTube playlist {}: {}", youtubePlaylistId, e.getMessage(), e);
            throw new RuntimeException("Error fetching items for YouTube playlist " + youtubePlaylistId + ": " + e.getMessage(), e);
        }

        log.info("Successfully fetched {} valid items for YouTube playlist ID: {}.", allItems.size(), youtubePlaylistId);
        return allItems;

    }

    /**
     * Fetches details for a specific YouTube playlist by its ID.
     * Requests minimal fields (id, title, description).
     *
     * @param youtubePlaylistId The ID of the YouTube playlist.
     * @param accessToken       The user's Google OAuth access token.
     * @return A DTO containing the playlist details, or null if not found/error.
     */
    public YoutubeSimplifiedPlaylistDto getPlaylistDetails(String youtubePlaylistId, String accessToken) {
        // Using YoutubeSimplifiedPlaylistDto for simplicity, assuming title is enough.
        // If description is needed, adjust DTO or create a new one.
        log.info("Fetching details for YouTube playlist ID: {}", youtubePlaylistId);
        // Request specific fields for the single playlist
        final String fieldsToRequest = "items(id,snippet(title,description))"; // Added description

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE_URL + "/playlists")
                    .queryParam("part", "snippet") // Snippet contains title and description
                    .queryParam("id", youtubePlaylistId) // Filter by specific ID
                    .queryParam("maxResults", 1) // Expect only one result
                    .queryParam("fields", fieldsToRequest);

            String url = uriBuilder.encode().toUriString();
            log.debug("Requesting YouTube playlist details from URL: {}", url);

            // The response is still a list, even when querying by ID
            YoutubePlaylistListResponseDto response = this.webClient.get()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(YoutubePlaylistListResponseDto.class)
                    .block();

            if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                YoutubeSimplifiedPlaylistDto playlistDetails = response.getItems().get(0);
                log.info("Successfully fetched details for YouTube playlist: {}", playlistDetails.getSnippet().getTitle());
                return playlistDetails;
            } else {
                log.warn("Could not find details or received empty items for YouTube playlist ID: {}", youtubePlaylistId);
                return null; // Indicate playlist not found or empty response
            }

        } catch (WebClientResponseException wcre) {
            log.error("WebClient Error fetching details for YouTube playlist {}: Status {}, Body {}", youtubePlaylistId, wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            // Return null or throw specific exception (e.g., PlaylistNotFound)
            return null;
        } catch (Exception e) {
            log.error("Generic Error fetching details for YouTube playlist {}: {}", youtubePlaylistId, e.getMessage(), e);
            return null; // Indicate error
        }
    }

}
