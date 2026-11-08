# Draghetto Run

Un gioco arcade per Android: tocca lo schermo per far volare il draghetto ed evitare gli ostacoli. Dopo una partita, tocca di nuovo per riprovare; il record viene salvato sul dispositivo.

## Compilare e installare

Servono JDK 17 e Android SDK 35. Dalla cartella del progetto:

```powershell
.\gradlew.bat :draghetto:assembleDebug
```

L'APK si trova in `draghetto\build\outputs\apk\debug\draghetto-debug.apk`.

GitHub Actions compila l'APK a ogni push e lo pubblica come artefatto della build. Un tag che inizia con `v` (per esempio `v1.0.0`) crea una GitHub Release con l'APK.
