![LibreSpeed-Android Logo](https://github.com/adolfintel/speedtest-android/blob/master/.github/Readme-Logo.png?raw=true)
 
# LibreSpeed Android QUIC
The Speedtest Android template allows you to configure and distribute an Android app that performs a speedtest using your existing [LibreSpeed](https://github.com/librespeed/speedtest) server(s).

The template is easy to configure, customize and distribute.

## Disclaimer

This repository is the result of a university project where the implementation of QUIC in the librespeed repo was the goal. Due to time constraints there are no guarantees quality- or accuracy wise. Hopefully one of you will take up the torch and continue the work!

## Test it out!

[<img src="https://upload.wikimedia.org/wikipedia/commons/a/a0/APK_format_icon.png" alt="Download APK" height="80">](https://github.com/Nevby/speedTest/blob/master/speedTest-prototype.apk)

Download the [demo APK](https://github.com/Nevby/speedTest/blob/master/speedTest-prototype.apk) to try it out!

## Compatibility
Android 4.0.3 and up (SDK 15), all architectures.

## Features
* Download
* Upload
* Ping
* Jitter
* IP Address, ISP, distance from server (optional)
* Telemetry (optional)
* Results sharing (optional)
* Multiple Points of Test (optional)

## New Features
* QUIC Support in Client
* New containerized backend with QUIC support 
* Parallel connections selection for TCP
* Functionality to add congestion control selection in the client

![Screenshot](https://github.com/Nevby/speedTest/blob/master/github_prototype.png?raw=true)

## Server requirements
One or more servers with [LibreSpeed](https://github.com/librespeed/speedtest) installed for TCP testing. For QUIC testing follow the setup instructions in the server side

## Setup Instructions Server

### Requirements for hosting the backend server
* Docker engine and docker compose installed on machine.
* Valid CA signed certificate in under to upgrade to HTTP/3, preferably certificate from Lets’s Encrypt as it provides a free valid CA signed certificate.
* Run backend server on Linux

To enable QUIC support on the NGNIX server, you’ll need to add the certificate and the private key as a volume when mounting the container. The NGNIX configuration file specifies the ssl_certificate as the path /opt/NGNIX/certs/live/”youdomain”/fullchain.pem as well as ssl_certificate:key at the path /opt/NGNIX/certs/live/”youdomain”/privkey.pem.  This is most easily done by using certbot. See the example docker-compose.yml files in the folder XXXX to see the example configuration for the php-fpm, NGNIX-QUIC and NGNIX-TCP containers. 

The run the command sudo docker-compose up -d, -d stands for detached mode which allows the containers to run in the background. Keep in mind that you’ll probably need to open the firewall for UDP traffic at port 443.

## Setup Instructions Client

This part is easy! Just clone the repository and run it in Android studio!

## Donate
[![Donate with Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/fdossena/donate)  
[Donate with PayPal](https://www.paypal.me/sineisochronic)  

## License
Copyright (C) 2020 Federico Dossena

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/lgpl>.
