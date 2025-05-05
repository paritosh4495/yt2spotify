package com.nexon.yt2spotify.dto.sotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyArtistDto {

    private String id;
    private String name;
}
