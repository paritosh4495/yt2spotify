package com.nexon.yt2spotify.dto.sotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyPagingObject<T>{

    private List<T> items;
    private int limit;
    private String next;
    private int offset;
    private String previous;
    private int total;
}
