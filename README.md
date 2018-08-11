# NotesCrypt #
NotesCrypt is a simple secure application that uses Android Key Store API and Fingerprint API to keep your notes private and safe.

## Requirements ##
* Android 23+ devices
* Device must be secure by pin/pattern/password

## Features ##
* Use finerprint to allow access to notes (Could fallback to password)
* Secure notes with AES 256 bit encryption key
* Stores data in sqlite database

## Screenshots ##
[Screenshots](https://github.com/sidstk/NotesCrypt/screens.md)

## Getting Started ##
This sample uses the Gradle build system. To build this project, use the "gradlew build" command or use "Import Project" in Android Studio.

## Technologies Used ##
* Android SDK 27
* Android Build Tools v27.1.1
* AndroidKeyStore API
* Android fingerprint API
* Password based encryption key derivation
* SQLite database
* Leak Cananry (For detecting memory leaks)

## Resources ##
### KeyStore API ###
* https://developer.android.com/training/articles/keystore.html
* https://developer.android.com/training/articles/keystore#java
* https://medium.com/@josiassena/using-the-android-keystore-system-to-store-sensitive-information-3a56175a454b

### Fingerprint API ###
* https://github.com/googlesamples/android-FingerprintDialog
* https://medium.com/@manuelvicnt/android-fingerprint-authentication-f8c7c76c50f8

### Cryptography ###
* https://doridori.github.io/android-security-the-forgetful-keystore/
* https://developer.android.com/reference/javax/crypto/spec/PBEKeySpec
* https://nelenkov.blogspot.com/2012/04/using-password-based-encryption-on.html
* https://www.owasp.org/index.php/Hashing_Java
* https://developer.android.com/reference/javax/crypto/Cipher
* https://proandroiddev.com/secure-data-in-android-encrypting-large-data-dda256a55b36

### Libraries ###
* https://github.com/square/leakcanary
* https://github.com/afollestad/material-dialogs

## Support ##
If you've found an error in this sample, please file an issue: https://github.com/sidstk/NotesCrypt
