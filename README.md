# Pamdroid

Pamdroid is a fork of PAM Android Fingerprint from @jrandiny.
It's a PAM module for authenticating from an Android fingerprint sensor.


## Android

Open `Pamdroid` as an Android Studio project, then build it for your target.
You can also compile the app directly by running `gradle` here.

Pamdroid requires Android 6.0 - API 23 - or higher.


## Linux

The Pamdroid desktop client requires Python 3.6 or higher,
with the `pyotp` and `requests` packages.

Ubuntu users can use `sudo apt install python-pyotp python-requests`.

After starting the Android app,
run `sudo init.py` to initialize your fingerprint.

Edit your PAM configuration file (`/etc/pam.d/system-auth` should work)
and add an entry like this:

`auth sufficient pam_exec.so <path to auth.py>`

Future logins should now attempt to authenticate using your device,
falling back to password login if the device isn't available.
