package com.nexon.yt2spotify.dto.yt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeMinimalPlaylistSnippetDto {
    // Only map the fields we explicitly request via the 'fields' parameter
    private String title;
}
