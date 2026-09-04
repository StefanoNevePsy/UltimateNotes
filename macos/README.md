# Ultimate Notes per macOS

App nativa SwiftUI che legge e scrive le stesse note dell'app Android,
attraverso una cartella condivisa. Nessun server, nessun account.

## Come funziona la sincronizzazione

Le due app non si parlano tra loro: parlano entrambe con una **cartella**
che tu tieni sincronizzata con lo strumento che preferisci (iCloud Drive,
Google Drive, Dropbox, Syncthing…). Il formato dei file è descritto in
[`../docs/VAULT_FORMAT.md`](../docs/VAULT_FORMAT.md).

```
LaTuaCartella/
  vault.json          cartelle
  notes/<id>.json     una nota per file
  assets/<id>/…       immagini e allegati di quella nota
```

Il merge è **per nota intera**, vince la modifica più recente
(`updatedAt`). Conseguenza da tenere a mente: se modifichi la *stessa*
nota su tablet e Mac mentre entrambi sono offline, quando si
riallineano sopravvive solo la versione salvata più tardi. Note diverse
modificate in parallelo non danno alcun problema.

Le cancellazioni viaggiano come *tombstone*: il file della nota resta ma
con `deletedAt` valorizzato, così l'altro dispositivo sa che è stata
eliminata invece di rimandarla indietro.

## Primo avvio

1. **Mac** — apri l'app, `File ▸ Apri cartella vault…` e scegli la
   cartella dentro il tuo servizio di sync.
2. **Android** — Impostazioni ▸ *Sincronizzazione con il Mac* ▸ *Scegli
   cartella*, e seleziona **la stessa cartella**.

Da lì in poi l'app Android si allinea all'apertura dell'elenco note (o
col pulsante *Sincronizza ora*), il Mac osserva la cartella e ricarica da
solo quando arrivano file nuovi.

> Su Android la cartella dev'essere raggiungibile via SAF. Funziona con i
> provider che espongono cartelle locali sincronizzate (Dropbox,
> Nextcloud, Syncthing). Google Drive, che non tiene una copia locale, è
> meno affidabile: se lo usi, verifica che il picker ti faccia scegliere
> la cartella e che la sincronizzazione riporti file scritti.

## Compilare

Serve un Mac con Xcode (o i Command Line Tools) — SwiftUI esiste solo su
piattaforme Apple.

```bash
cd macos
swift build            # compila
swift test             # test del modello dati
./scripts/make-app.sh  # produce dist/Ultimate Notes.app
```

`make-app.sh` costruisce in release, impacchetta il binario in un bundle
`.app` con il suo `Info.plist` e lo firma ad-hoc, quel tanto che basta per
avviarlo in locale senza un account sviluppatore.

Se preferisci non compilare nulla: ogni push fa girare il workflow
[`build-macos`](../.github/workflows/macos.yml) su un runner macOS, che
allega un `.dmg` agli artifact della run.

### Nota sulla firma

Il bundle è firmato ad-hoc, non notarizzato. Al primo avvio macOS può
bloccarlo: tasto destro sull'app ▸ *Apri*, oppure Impostazioni di Sistema
▸ *Privacy e sicurezza* ▸ *Apri comunque*. Per evitarlo servirebbe un
Apple Developer ID (a pagamento) e la notarizzazione nel workflow.

## Struttura

```
macos/
  Package.swift                     pacchetto SwiftPM (eseguibile + test)
  Resources/Info.plist              metadati del bundle .app
  scripts/make-app.sh               build + impacchettamento
  Sources/UltimateNotesMac/
    Model/NoteModel.swift           modello note, 1:1 con il Kotlin
    Model/VaultStore.swift          lettura/scrittura/osservazione del vault
    Model/Theme.swift               temi portati dall'app Android
    UI/…                            interfaccia SwiftUI
  Tests/UltimateNotesMacTests/      test del formato dati
```

## Differenze rispetto all'app Android

L'app per Mac nasce dopo e non ha ancora tutto. Al momento restano solo
su Android: S Pen e pressione del tratto, i font `.ttf` importati
dall'utente (su Mac ogni tema usa la faccia di sistema più vicina) e il
backup `.zip` — che qui conta meno, visto che la cartella condivisa *è*
già una copia leggibile di tutte le note.
