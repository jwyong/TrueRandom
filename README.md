TrueRandom - because Spotify shuffle sux.

- Spotify shuffle creates a "randomised" list when we tap on the play button with random selected
- this enables previous / next buttons to work correctly
- problem is, this list is probably limited to maybe a few hundred at max
- the behaviour noticed is: after a long play session (car day trip, playback several hours), the same songs will be played again, with the same sequence (presumably the list has exhausted, and playback starts from beginning of list again)
- another noticed behaviour is, even if we "reset" the session by tapping play again / killing app, a set of songs will always get "preference" and played more "frequentyl". this might just be purely psychological.
- either way, TrueRandom attempts to playback with "True" shuffling - basically playing ALL the tracks with lowest count and working upward
- so in a way, this app is not TRUE random at all. it's more of a "no-repeat shuffle" mode

- current implementation uses Spotify Remote App API to auth the user, then retrieves all tracks in the user's "Liked Songs" to be stored in local db
- local tracks seem to NOT be supported by this API (tracks that are NOT on Spotify, but rather added by user locally)
- when user taps play, the app gets a list of tracks with the LOWEST play count from local db, picks a random track and plays it
- end of playback for said track is detected by callback provided by API, with which play count of the track will be incremented to db
- then the process is repeated by getting a NEW list of tracks with lowest play count from db to randomise (which will now EXCLUDE the song just played)

NOTE:
- Spotify recently added a new shuffling mode based on "freshness", which will most likely satisfy most users who noticed the repetitiveness of the previous shuflle mode, but NOT if you prefer no-repeat playback style.
- This is an ongoing project, with fixes and new features to be added continuously until the iteration is of satisfactory UX to my use-case.
