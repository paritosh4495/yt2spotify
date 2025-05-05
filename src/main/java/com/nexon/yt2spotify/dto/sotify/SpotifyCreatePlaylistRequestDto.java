package com.nexon.yt2spotify.dto.sotify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotifyCreatePlaylistRequestDto {
    private String name;
    private String description;
    private boolean public_ = false; // Default to private playlist (use underscore as 'public' is keyword)
    private boolean collaborative = false;
}
