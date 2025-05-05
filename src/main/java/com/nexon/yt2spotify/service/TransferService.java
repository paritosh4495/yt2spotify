package com.nexon.yt2spotify.service;

import com.nexon.yt2spotify.dto.sotify.SpotifyPlaylistDto;
import com.nexon.yt2spotify.dto.sotify.SpotifyTrackDto;
import com.nexon.yt2spotify.dto.sotify.SpotifyUserDto;
import com.nexon.yt2spotify.dto.yt.YoutubePlaylistItemDto;
import com.nexon.yt2spotify.dto.yt.YoutubeSimplifiedPlaylistDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    private final SpotifyApiService spotifyApiService;
    private final YoutubeApiService youtubeApiService;
    private static final int SPOTIFY_BATCH_SIZE = 100; // Spotify limit for adding tracks

    @Async("taskExecutor") // Specify the bean name of your configured TaskExecutor
    public void transferYoutubePlaylistAsync(String youtubePlaylistId, String spotifyToken, String googleToken) {

        log.info("Starting ASYNC transfer for YouTube Playlist ID: {}", youtubePlaylistId);
        long startTime = System.currentTimeMillis();

        try {
            // 1. Get YouTube Playlist Details (Name for Spotify)
            log.debug("Fetching YouTube playlist details...");
            YoutubeSimplifiedPlaylistDto ytPlaylist = youtubeApiService.getPlaylistDetails(youtubePlaylistId, googleToken);
            if (ytPlaylist == null || ytPlaylist.getSnippet() == null || ytPlaylist.getSnippet().getTitle() == null) {
                log.error("Cannot proceed: Failed to get valid details for YouTube playlist {}", youtubePlaylistId);
                return;
            }
            String newSpotifyPlaylistName = ytPlaylist.getSnippet().getTitle();
            // Potentially use YT description: String newSpotifyPlaylistDesc = ytPlaylist.getSnippet().getDescription();
            String newSpotifyPlaylistDesc = "Transferred from YouTube Playlist: " + newSpotifyPlaylistName; // Default description
            log.info("Source YouTube Playlist Name: '{}'", newSpotifyPlaylistName);

            // 2. Get Spotify User ID (needed to create playlist)
            log.debug("Fetching Spotify user ID...");
            SpotifyUserDto spotifyUser = spotifyApiService.getCurrentSpotifyUser(spotifyToken);
            if (spotifyUser == null || spotifyUser.getId() == null) {
                log.error("Cannot proceed: Failed to get Spotify User ID.");
                return;
            }
            String spotifyUserId = spotifyUser.getId();
            log.info("Target Spotify User ID: {}", spotifyUserId);

            // 3. Create new Spotify Playlist
            log.debug("Creating new Spotify playlist '{}'...", newSpotifyPlaylistName);
            SpotifyPlaylistDto newSpotifyPlaylist = spotifyApiService.createPlaylist(
                    spotifyUserId, newSpotifyPlaylistName, newSpotifyPlaylistDesc, false, spotifyToken // false = private
            );
            if (newSpotifyPlaylist == null || newSpotifyPlaylist.getId() == null) {
                log.error("Cannot proceed: Failed to create Spotify playlist '{}'", newSpotifyPlaylistName);
                return;
            }
            String newSpotifyPlaylistId = newSpotifyPlaylist.getId();
            log.info("Created Spotify playlist '{}' with ID: {}", newSpotifyPlaylistName, newSpotifyPlaylistId);

            // 4. Get YouTube Playlist Items
            log.debug("Fetching YouTube playlist items for ID: {}", youtubePlaylistId);
            List<YoutubePlaylistItemDto> ytItems = youtubeApiService.getPlaylistItems(youtubePlaylistId, googleToken);
            int totalYtItems = ytItems.size();
            log.info("Found {} items in YouTube playlist {}", totalYtItems, youtubePlaylistId);

            if (totalYtItems == 0) {
                log.info("Source YouTube playlist is empty. Transfer complete.");
                return;
            }

            // 5. Process Items: Search Spotify & Collect URIs
            log.info("Starting Spotify search for {} YouTube items...", totalYtItems);
            List<String> spotifyTrackUris = new ArrayList<>();
            int notFoundCount = 0;

            for (int i = 0; i < totalYtItems; i++) {
                YoutubePlaylistItemDto item = ytItems.get(i);
                String ytTitle = item.getSnippet() != null ? item.getSnippet().getTitle() : "Unknown Title";
                String ytChannel = item.getSnippet() != null ? item.getSnippet().getVideoOwnerChannelTitle() : "";

                log.info("[Item {}/{}] Processing YT Video: '{}' by '{}'", (i + 1), totalYtItems, ytTitle, ytChannel);

                // Basic Search Query Construction (can be improved)
                // Often YouTube titles include "Artist - Title" or just "Title"
                // Using title + channel might help sometimes
                String query = ytTitle + " " + ytChannel; // Combine title and channel

                // Search Spotify
                Optional<SpotifyTrackDto> searchResult = spotifyApiService.searchTrack(query, spotifyToken);

                if (searchResult.isPresent()) {
                    SpotifyTrackDto track = searchResult.get();
                    if (track.getUri() != null && !track.getUri().isBlank()) {
                        log.debug("   -> Found Spotify Track: '{}' ({})", track.getName(), track.getUri());
                        spotifyTrackUris.add(track.getUri());
                    } else {
                        log.warn("   -> Found Spotify track '{}' but it has no URI. Skipping.", track.getName());
                        notFoundCount++;
                    }
                } else {
                    log.warn("   -> No Spotify track found for query: '{}'. Skipping.", query);
                    notFoundCount++;
                }
                // Optional Delay to prevent hitting rate limits aggressively
                // try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } // End YT Item loop

            log.info("Spotify search complete. Found {} potential tracks to add.", spotifyTrackUris.size());

            // 6. Add Found Tracks to Spotify Playlist in Batches
            if (!spotifyTrackUris.isEmpty()) {
                log.info("Adding {} tracks to Spotify playlist '{}' in batches of {}...",
                        spotifyTrackUris.size(), newSpotifyPlaylistId, SPOTIFY_BATCH_SIZE);

                int addedCount = 0;
                for (int i = 0; i < spotifyTrackUris.size(); i += SPOTIFY_BATCH_SIZE) {
                    int end = Math.min(i + SPOTIFY_BATCH_SIZE, spotifyTrackUris.size());
                    List<String> batch = spotifyTrackUris.subList(i, end);
                    try {
                        spotifyApiService.addTracksToPlaylist(newSpotifyPlaylistId, batch, spotifyToken);
                        addedCount += batch.size();
                        log.debug("   -> Added batch {} - {} successfully.", i + 1, end);
                        // Optional Delay
                        // if (end < spotifyTrackUris.size()) { try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
                    } catch (Exception e) {
                        log.error("   -> Failed to add batch {} - {}: {}", i + 1, end, e.getMessage());
                        // Decide whether to stop or continue - continuing for now
                    }
                }
                log.info("Finished adding tracks to Spotify. Successfully added: {}", addedCount);
            } else {
                log.info("No Spotify tracks found to add to the new playlist.");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Transfer completed for YouTube Playlist ID: {}. Duration: {} ms. Found on Spotify: {}, Not Found/Skipped: {}",
                    youtubePlaylistId, duration, spotifyTrackUris.size(), notFoundCount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Transfer failed catastrophically for YouTube Playlist ID {}: {} (Duration: {} ms)",
                    youtubePlaylistId, e.getMessage(), duration, e); // Log exception details
        }
    }
}
