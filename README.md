# Sensor Streamer WearOS
A minimal, hackable sensor streamer for Android WearOS. Originally created for [_SAMoSA_ (IMWUT/Ubicomp 2022)](https://vimalmollyn.com/research/samosa/).

<p align="center">
  <img src="media/icon.png" width="30%"/>
</p>

## What's it good for?
Sensor Streamer streams IMU (accelerometer, gyroscope, magnetometer, rotation vector, etc) and Audio data from an Android smartwatch to a local UDP server. Tested with WearOS v2.23 and Android Studio Arctic Fox | 2020.3.1 Beta 3, on a Fossil Gen 5 smartwatch and an M1 Macbook Air running MacOS 11.2.

## How to use?
Compile and install the app on your android watch. Enter your UDP server's IP Address. Data is streamed on `port 5005`. 

<p float="left" align="center">
  <img title="background img credit: verge.com" src="./media/app_drawer.png" width="30%"/>
  <img title="background img credit: popsci.com" src="./media/app_preview.png" width="30%"/>
</p>
