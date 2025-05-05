package com.nexon.yt2spotify.dto.yt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeMinimalPlaylistItemResourceIdDto {
    private String videoId;
}

