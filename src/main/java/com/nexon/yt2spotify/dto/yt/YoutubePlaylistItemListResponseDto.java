package com.nexon.yt2spotify.dto.yt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubePlaylistItemListResponseDto {

    private String nextPageToken;
    private List<YoutubePlaylistItemDto> items;
}
