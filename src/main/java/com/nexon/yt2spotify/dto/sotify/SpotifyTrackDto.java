package com.nexon.yt2spotify.dto.sotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyTrackDto {
    private String id;
    private String name;
    private List<SpotifyArtistDto> artists; // Assuming SpotifyArtistDto (id, name) exists
    private String uri; // spotify:track:ID
    // private SpotifyAlbumDto album; // Optional: include if needed for matching
    // private int durationMs; // Optional: include if needed for matching
    // private boolean is_local; // Handled previously
}
