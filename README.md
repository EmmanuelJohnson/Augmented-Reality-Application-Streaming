# Augmented-Reality-Application-Streaming

Project to offload augmented reality processing to a remote machine, from an Android device. Please read [Report.pdf](https://github.com/EmmanuelJohnson/Augmented-Reality-Application-Streaming/blob/master/Report.pdf) for details.

## Setup

Before statring, setup the following modules. These are used to inject a custom video feed into an Android Virtual Device's Camera.

### Video Loopback device

To create a virtual video device, load the [v4l2loopback](https://github.com/umlaeute/v4l2loopback) module. Please read the readme file on that repo for more details.

### FFMPEG

To inject an input file or stream to the virtual device created install [FFMPEG](https://github.com/FFmpeg/FFmpeg), and run the following commands.

For a video file,
```
ffmpeg -re -i <sample_footage>.mp4 -map 0:v -f v4l2 </dev/video0>
```

For a live stream,
```
ffmpeg -f video4linux2 -input_format mjpeg -i rtsp://<stream_url> -f v4l2 </dev/video0>
```

## Contents

Description of contents of folders in this repository.

### Final Application and Server

Folder for final Android application (to live stream both Camera and Sensor values) and Java server (to accept and feed sensor values to an AVD over TCP). 

### Python Live Sensor Simulator

Folder for Android application to live stream Sensor values and Python server (to accept and feed sensor values to an AVD through web sockets).

### VideoSensorsRecorder

Android application to simultaneously record Camera feed to an .mp4 file and Sensor values to a .csv file.

### Sensor Scripts

Folder for scripts to start emulator and inject sensor values from existing .csv file to AVD over telnet.

### spydroid-ipcamera

Android application to live stream Camera feed over HTTP or RTSP.
