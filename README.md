# Ultimate Notes

App Android nativa (Kotlin + Jetpack Compose) pensata come hub personale per
le note: scrittura a mano con S Pen, testo ricco in Markdown, immagini, PDF
annotabili, canvas infinito e collegamenti visivi stile Kinopio.

## Download

Ogni push su `main` o sui branch `claude/**` produce automaticamente una
**release su GitHub** con l'APK firmato (firma di test stabile: le build
successive si installano come aggiornamento). Vai su *Releases* e scarica
l'ultimo `UltimateNotes-buildN.apk`.

## Funzionalità

| Area | Dettagli |
| --- | --- |
| **Cartelle** | Cartelle illimitate, personalizzabili con 12 colori e 17 icone. Tieni premuta una cartella per modificarla. |
| **Canvas infinito** | Pan e zoom illimitati (pizzica con due dita o trascina con un dito quando "Disegna solo con la S Pen" è attivo). Sfondi: vuoto, punti, griglia, righe. |
| **Penna & S Pen** | Penna con spessore sensibile alla pressione, evidenziatore semitrasparente, gomma a tratto. Il fondello-gomma della S Pen cancella automaticamente. |
| **Ruota rapida** | Premi il **pulsante della S Pen** (sia in hover sia a contatto) per aprire la ruota radiale stile Samsung Notes con strumenti, palette colori e slider spessore, centrata sotto la punta. Si apre anche dal cerchio colore nella barra strumenti. |
| **Testo libero** | Blocchi di testo posizionabili ovunque sul canvas, trascinabili e ridimensionabili. |
| **Markdown live** | `# ## ###` titoli, `**grassetto**`, `*corsivo*`, `~~barrato~~`, `` `codice` ``, elenchi `-`, checklist `- [ ]`, citazioni `>` — con styling in tempo reale mentre scrivi e barra di formattazione completa. |
| **Stili di testo** | Titolo 1/2/3, Corpo, Didascalia personalizzabili (dimensione e peso) dalle impostazioni; le modifiche si propagano a tutte le note. |
| **Font personalizzati** | Importa qualsiasi `.ttf`/`.otf` dalle impostazioni e applicalo per singolo blocco di testo. |
| **Immagini & sticker** | Inserisci immagini dalla galleria o **direttamente dalla Samsung Keyboard** (sticker, AI drawing assistant…): mentre scrivi in un blocco di testo, gli sticker della tastiera vengono ricevuti via `commitContent` e aggiunti come immagini sul canvas. |
| **PDF annotabili** | Importa un PDF: ogni pagina viene renderizzata sul canvas e puoi scriverci sopra con qualsiasi strumento. |
| **Collegamenti (Kinopio-style)** | Strumento "Collega": tocca due elementi per unirli con una curva di Bézier modificabile trascinando il punto centrale. Stili linea solid/tratteggiata/punteggiata, animazione "marching dashes", punte freccia/pallino per lato, colori e spessore. I collegamenti seguono gli elementi quando li sposti. |
| **Temi** | 7 temi (Latte, Seppia, Nordic, Foresta, Notte, Dracula, OLED): ogni tema cambia colori **e** personalità grafica — raggio degli angoli, trasparenza "glass" delle barre flottanti e, nei temi analogici, bordi disegnati a mano stile excalidraw. |
| **Design** | UI glass-like con barre flottanti, tipografia mista serif (Lora) + sans arrotondato (Nunito), icone Lucide, animazioni spring su strumenti, card, FAB, transizioni di navigazione e selezioni. |
| **Ricerca** | Ricerca full-text su titoli e contenuto. |
| **Pin & organizzazione** | Fissa le note in alto, spostale tra cartelle dal menu contestuale. |
| **Backup** | Esporta/importa tutto (note, cartelle, immagini, font) come singolo `.zip` tramite il selettore di sistema — puoi salvarlo direttamente su **Google Drive**. |

## Architettura

```
app/src/main/java/com/stefanoneve/ultimatenotes/
├── data/
│   ├── model/      NoteContent: strokes, elementi (testo/immagine/PDF), stili
│   ├── db/         Room: cartelle e note (contenuto serializzato JSON)
│   ├── repo/       NotesRepository, SettingsStore
│   ├── fonts/      FontManager (font di sistema + .ttf/.otf importati)
│   └── backup/     BackupManager (zip via SAF)
├── ui/
│   ├── home/       Home: cartelle, griglia note, ricerca, impostazioni
│   ├── editor/     Canvas infinito, gesti, ruota radiale, markdown, barre
│   └── theme/      Material 3 + dynamic color
└── util/           SPenEvents (pulsante S Pen a livello Activity)
```

Scelte principali:
- **Contenuto nota** = JSON (`kotlinx.serialization`) dentro Room: tratti di
  inchiostro con punti+pressione e elementi liberi con coordinate mondo.
- **Coordinate mondo**: il canvas è infinito; `CanvasState` gestisce
  offset+scala e gli elementi sono composables trasformati via `graphicsLayer`.
- **S Pen**: rilevamento stylus/gomma via `PointerType`, pressione da
  `PointerInputChange.pressure`, pulsante via `MotionEvent.BUTTON_STYLUS_PRIMARY`
  intercettato in `MainActivity` (funziona anche in hover).
- **Sticker tastiera**: `Modifier.contentReceiver` (Compose Foundation) sui
  campi di testo riceve contenuti immagine da tastiera/clipboard/drag&drop.

## Build

Richiede JDK 17+ e Android SDK 35.

```bash
./gradlew :app:assembleDebug
# APK in app/build/outputs/apk/debug/app-debug.apk
```

## Roadmap / idee future

- Selezione lazo e trasformazione dei tratti
- Riconoscimento forme e testo manoscritto
- Esportazione nota in PDF/PNG
- Sincronizzazione automatica con Google Drive (OAuth)
- Tag e collegamenti tra note
