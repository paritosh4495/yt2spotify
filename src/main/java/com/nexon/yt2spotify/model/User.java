package com.nexon.yt2spotify.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String spotifyId;

    @Column(unique = true)
    private String googleId;

    private String email;

    @Column(nullable = false)
    private String displayName;

    public User(String providerId, String provider, String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
        if("spotify".equalsIgnoreCase(provider)) {
            spotifyId = providerId;
        }
        else if("google".equalsIgnoreCase(provider)) {
            googleId = providerId;
        }
    }




}
