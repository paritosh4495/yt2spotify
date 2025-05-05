
# YouTube to Spotify Playlist Transfer (Backend) 🎬➡️🎧

A Spring Boot backend application designed to transfer user playlists from YouTube Music to Spotify. This application handles authentication with both services using OAuth 2.0 and provides API endpoints to manage the playlist transfer process.

## Overview ✨

This project allows users to authenticate their Google (YouTube) and Spotify accounts and then initiate a transfer of a selected YouTube playlist. The application fetches video details from YouTube, matches them to corresponding tracks on Spotify, creates a new playlist on Spotify with the same name, and adds the found tracks. The core transfer process is handled asynchronously.

**Core Technologies:**

* Java 21
* Spring Boot (v3.3.x)
* Spring Security (OAuth 2.0 Client, Session Management)
* Spring Data JPA / Hibernate
* Spring Web / WebClient
* PostgreSQL
* Lombok
* YouTube Data API v3
* Spotify Web API

## Features 🚀

* **OAuth 2.0 Authentication:** Secure login via both Google (YouTube) and Spotify.
* **Account Linking:** Automatically links accounts by matching email addresses. Separate accounts are created if emails don't match.
* **Token Management:** Persistently stores OAuth access and refresh tokens in the database using Spring Security's default schema. Tokens are refreshed automatically via `OAuth2AuthorizedClientManager`.
* **List YouTube Playlists:** Fetches all user playlists from the authenticated YouTube account.
* **Get YouTube Playlist Videos:** Retrieves video details (title, duration, channel) from a specific playlist, filtering out non-music videos where possible.
* **Search Spotify Tracks:** Matches YouTube videos to Spotify tracks using title and artist info.
* **Create Spotify Playlist:** Creates a private playlist on the authenticated user’s Spotify account.
* **Add Tracks to Spotify Playlist:** Adds matched tracks to the new Spotify playlist.
* **Asynchronous Transfer:** Runs the full transfer flow in a background thread using `@Async` for fast responses.
* **Session-based API Access:** Works with tools like Postman using session cookies (`JSESSIONID`) after browser login.

## Technologies Used 🛠️

Same stack as the original with appropriate API focus changes:

* **Framework:** Spring Boot 3.3.x
* **Language:** Java 21
* **Security:** Spring Security 6.x (OAuth 2.0)
* **Data:** Spring Data JPA, Hibernate
* **HTTP Client:** WebClient
* **Database:** PostgreSQL
* **Build Tool:** Maven
* **Utility:** Lombok
* **External APIs:**

  * YouTube Data API v3
  * Spotify Web API
* **Async:** Spring `@Async` with custom executor setup

## Prerequisites 📋

* Java 21
* Maven 3.8+
* PostgreSQL instance running
* A [Google Cloud Project](https://console.cloud.google.com/) with YouTube Data API v3 enabled
* A [Spotify Developer Account](https://developer.spotify.com/dashboard/) with app credentials

## Setup & Configuration ⚙️

1. **Clone the Repository:**

   ```bash
   git clone <your-repo-url>
   cd <your-project-folder>
   ```

2. **Database Setup:**

   Create a database (e.g., `yt2spotify_db`) and configure your credentials in `application.yml`. Tables will be auto-generated on startup (`ddl-auto: update`).

3. **API Credentials:**

   * **Google (YouTube):**

     * Enable YouTube Data API v3.
     * Set redirect URI: `http://localhost:8080/login/oauth2/code/google`
     * Required Scopes: `openid`, `profile`, `email`, `https://www.googleapis.com/auth/youtube.readonly`

   * **Spotify:**

     * Redirect URI: `http://localhost:8080/login/oauth2/code/spotify`
     * Required Scopes: `playlist-modify-private`, `playlist-modify-public`, `user-read-email`

4. **`application.yml`:**

   Configure like so:

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/yt2spotify_db
       username: ${DB_USERNAME:your_db_user}
       password: ${DB_PASSWORD:your_db_password}

     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true

     security:
       oauth2:
         client:
           registration:
             google:
               client-id: ${GOOGLE_CLIENT_ID}
               client-secret: ${GOOGLE_CLIENT_SECRET}
               scope:
                 - openid
                 - profile
                 - email
                 - https://www.googleapis.com/auth/youtube.readonly
               redirect-uri: http://localhost:8080/login/oauth2/code/google

             spotify:
               client-id: ${SPOTIFY_CLIENT_ID}
               client-secret: ${SPOTIFY_CLIENT_SECRET}
               scope:
                 - playlist-modify-private
                 - playlist-modify-public
                 - user-read-email
               redirect-uri: http://localhost:8080/login/oauth2/code/spotify

           provider:
             google:
               user-info-uri: https://openidconnect.googleapis.com/v1/userinfo

   logging:
     level:
       root: INFO
       com.yourapp: DEBUG
   ```

## Running the Application 🚀

```bash
mvn spring-boot:run
```

Access the app at `http://localhost:8080`

## Usage / API Endpoints 🧭

### 1. Authentication

* Login to Google:
  `http://localhost:8080/oauth2/authorization/google`

* Login to Spotify:
  `http://localhost:8080/oauth2/authorization/spotify`

**Tip:** Use the same browser session for both to store tokens together.

### 2. Use API Tools

After authenticating via browser:

* Open dev tools → cookies → copy `JSESSIONID`
* Add to Postman or `curl` requests as:
  `Cookie: JSESSIONID=<value>`

### 3. Key Endpoints

* **Get YouTube Playlists:**
  `GET /api/youtube/playlists`

* **Get YouTube Playlist Videos:**
  `GET /api/youtube/playlists/{playlistId}/videos`

* **Search Spotify Track:**
  `GET /api/spotify/search?query=Song+Name+Artist`

* **Create Spotify Playlist:**
  `POST /api/spotify/playlists`
  Body: `title`, `description`, `public=true/false`

* **Transfer YouTube → Spotify Playlist:**
  `POST /api/transfers/youtube/{playlistId}`
  Returns: `202 Accepted` — process runs in background

## Project Structure 📁

```bash
src/
├── config/            # OAuth2, Web, Security configs
├── controller/        # REST API controllers
├── service/           # Playlist processing logic
├── dto/               # YouTube and Spotify DTOs
├── mapper/            # Mapping between DTOs and models
├── repository/        # Spring Data Repositories
└── model/             # JPA Entities
```

## Known Limitations & Future Work 🔧

* No frontend – only API support via tools like Postman
* Track matching could be smarter (e.g., fuzzy search, duration tolerance)
* No progress status API (just logs for now)
* Rate limiting not fully handled – caution with large playlists
* Limited error reporting (mostly backend logs)
* One-way transfer only: no Spotify → YouTube in this repo

## License 📄

MIT License
