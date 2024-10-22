# Configuring your android emulator to work with local issuer/verifier
## Table of contents
* [Overview](#overview)
* [Install SDK](#pre-requisites)
* [Replace device Host file](#replace-device-host-file)
  
## Overview
This guide aims to assist developers to install and configure an android emulator that will communicate with localy running issuers/verifiers.

## Pre-requisites
It is assumed that you have locally running issuer and/or verifier. You will also need to have a local host-file with a line mapping a hostname to your local ip.  
Example:  
```
	127.0.0.1		demo-wallet-issuer
```
You will also need to install Android Stuido (preferably the latest version)

## Install Device (AVD)
You will need an appropriate Device (AVD), that is one that doesn't contain `Google API's`, this is important, since Devices's with `Google API's` cannot be run in writable mode.
You can open the Device manager from your `Tools` menu in Android Studio.  
In your device manager, click the `+` to create new device. You will now get a list of hardware to select from. Pick one where the `Play Store` column is empty, and install this.

## Replace device Host File
### Run you device in writable mode
1. Open a terminal, and navigate to your Android SDK directory. (The location can be found by opening `SDK Manager` from the `Tools` menu in Android Studio)  
1. Open the emulator subdirectory
1. List your current installed emulators by running `.\emulator.exe -list-avds` (on windows)
1. Run the emulator you just installed in writable mode, by using a command like `.\emulator.exe -writable-system -avd Pixel_6_Pro_API_35` 
### Replace the device Host file
1. Open a new terminal, and navigate to the `platform-tools`subdirectory in you Android SDK directory - you should find an application in here named `adb`
1. Get root access by running `.\adb.exe root`
1. Remount filesystem with root access by running `.\adb.exe remount`
1. Get your device default host-file content by running `.\adb.exe shell cat /etc/hosts`
1. Create a new hosts file with the default content and add your own line. Note that your local computer is accessible at ip 10.0.2.2 inside the emulator.  
    Add a line looking something like this: 
    ```
    10.0.2.2        demo-wallet-issuer
    ```
1. Push your new hosts file to the device, replacing in two locations, by running  
    - `.\adb.exe push c:\temp\hosts /etc/hosts`
    - `.\adb.exe push c:\temp\hosts /system/etc/hosts`
1. Restart you device by running `.\adb.exe reboot`
1. After reboot, verify hosts file content by again running `.\adb.exe shell cat /etc/hosts`
1. You can now shutdown your emulator - In windows you can open the first termminal and click `Ctrl-C`


Your new hosts file should now be active any time you run this emulator, as long as you do not completely reset it. You would then have to repeat the process.