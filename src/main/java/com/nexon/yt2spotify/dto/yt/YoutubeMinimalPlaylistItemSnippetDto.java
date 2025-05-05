package com.nexon.yt2spotify.dto.yt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeMinimalPlaylistItemSnippetDto {

    private String title;
    private String videoOwnerChannelTitle;
    private YoutubeMinimalPlaylistItemResourceIdDto resourceId;
}
