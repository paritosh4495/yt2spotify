package com.nexon.yt2spotify.dto.yt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubePlaylistListResponseDto {
    // We don't need kind or etag
    private String nextPageToken;
    // private String prevPageToken; // Not requesting this field
    private List<YoutubeSimplifiedPlaylistDto> items;

}
