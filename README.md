## Required libraries
gcc-multilib
bluetooth-dev
libbluetooth3

## Bluez setup
Bluez 5 has broken sdp tool `https://raspberrypi.stackexchange.com/questions/41776/failed-to-connect-to-sdp-server-on-ffffff000000-no-such-file-or-directory`
It must be run in compat mode for it to work.
Sdp file permissions might be needed to work on non-sudo users