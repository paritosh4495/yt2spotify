package com.nexon.yt2spotify.repository;

import com.nexon.yt2spotify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySpotifyId(String spotifyId);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
}
