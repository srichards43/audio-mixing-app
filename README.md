# AudioMixer
An android app that allows users to play music and ambient sounds simultaneously for sleep, relaxation or focusing.

## Overview

While trying to listen to white noise alongside music, I found that Android's audio system only allows one app to take control of audio playback, meaning one sound overrides and interrupts the other. I couldn't find any apps on android with a simple solution, so I created this app to solve this.
It gives users control of two audio sources together: their music library and looping ambient sounds.

## Features

- Play music and ambient sounds simultaneously
- Ambient sound library (rain, white noise, log fire, etc.)
- Queue system to manage song playback
- Independent volume control for each audio source
- Sleep timers to automatically fade out into silence.

## Screenshots

<img width="400" height="885" alt="image" src="https://github.com/user-attachments/assets/52d66d77-e355-45e8-ab76-f149d1706a67" />

<img width="400" height="895" alt="image" src="https://github.com/user-attachments/assets/bb2e80ac-12bb-4854-b662-6c44385af64d" />

## Built With

- Java
- Android Studio
- ExoPlayer - audio playback
- [CircularSeekBar](https://github.com/tankery/CircularSeekBar) - volume seekbar UI
