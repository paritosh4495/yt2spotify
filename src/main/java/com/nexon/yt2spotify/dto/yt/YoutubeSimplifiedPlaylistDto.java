package com.nexon.yt2spotify.dto.yt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeSimplifiedPlaylistDto {
    private String id;
    private YoutubeMinimalPlaylistSnippetDto snippet;
    private YoutubeMinimalPlaylistContentDetailsDto contentDetails;
}
