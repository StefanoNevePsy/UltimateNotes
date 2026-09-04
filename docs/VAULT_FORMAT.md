# Ultimate Notes — formato "vault" condiviso

Contratto dati fra l'app **Android** (Kotlin) e l'app **macOS** (Swift).
Entrambe leggono e scrivono la stessa cartella, che l'utente tiene
sincronizzata con Drive / Dropbox / iCloud / Syncthing. Nessun server.

## Struttura della cartella

```
<VaultRoot>/
  vault.json                     metadati del vault + cartelle
  notes/<noteId>.json            una nota per file (metadati + contenuto)
  assets/<noteId>/<fileName>     immagini, pagine PDF, allegati di quella nota
```

`<noteId>` è un UUID minuscolo. I nomi degli asset sono quelli già presenti
in `ImageElement.fileName` / `FileElement.fileName`, quindi il JSON della nota
non va riscritto quando si sincronizzano i file.

## vault.json

```json
{
  "formatVersion": 1,
  "folders": [
    { "id": "uuid", "name": "Lavoro", "color": 4283215696, "icon": "folder", "position": 0 }
  ]
}
```

`color` è un intero ARGB senza segno (0xAARRGGBB) rappresentato come numero
JSON, come su Android.

## notes/&lt;noteId&gt;.json

```json
{
  "id": "uuid",
  "title": "Titolo",
  "folderId": null,
  "pinned": false,
  "createdAt": 1730000000000,
  "updatedAt": 1730000000000,
  "deletedAt": null,
  "content": { … NoteContent … }
}
```

- I timestamp sono **millisecondi** da epoch (come `System.currentTimeMillis()`).
- `deletedAt` non nullo = tombstone: la nota è cancellata ma il file resta,
  così la cancellazione si propaga anche all'altro dispositivo. Un client può
  eliminare fisicamente i tombstone più vecchi di 30 giorni.

## Sincronizzazione

Merge **per nota**, last-write-wins su `updatedAt`:

1. Per ogni nota presente solo da un lato → copiala dall'altro.
2. Per ogni nota presente da entrambi i lati → vince `updatedAt` maggiore.
3. Un tombstone vince su una nota normale solo se il suo `deletedAt` è
   maggiore dell'`updatedAt` dell'altra copia (altrimenti la nota è stata
   modificata dopo la cancellazione e va tenuta).
4. Gli asset si copiano solo se mancanti dalla destinazione: i nomi file sono
   unici (UUID nel nome), quindi non esistono conflitti di contenuto.

La granularità è la nota intera: modificare la stessa nota su due dispositivi
mentre entrambi sono offline fa perdere la versione più vecchia. È il
compromesso accettato per non avere un server.

## NoteContent

È **esattamente** il JSON che `kotlinx.serialization` produce oggi per
`NoteContent`, così il file è leggibile dall'app Android senza conversioni.
`encodeDefaults = true`: tutti i campi sono sempre presenti in scrittura, ma
un lettore deve tollerarne l'assenza usando i default qui indicati.

```json
{
  "elements":   [ … ],
  "strokes":    [ … ],
  "connectors": [ … ],
  "frames":     [ … ],
  "tapes":      [ … ],
  "background": "DOTS"
}
```

### Colori

Ogni colore è un **Long ARGB** (0xAARRGGBB) come numero JSON, con questi
valori riservati (vedi `ThemedResolve.kt`):

| Valore | Significato |
|---|---|
| `0` | "auto": ruolo 0 del tema (per l'inchiostro = colore testo del tema) |
| `1`…`16` | slot accento del tema (il valore n = slot n-1) |
| `> 16` | colore ARGB fisso scelto dall'utente |

`TextElement.bgColor == 1` ha il significato speciale di sticky note a tema
(costante `STICKY_AUTO`).

### elements — array polimorfico

Discriminatore: campo **`type`** (default di kotlinx.serialization).

Campi comuni a tutti: `id: String`, `x: Float`, `y: Float`,
`groupId: String?` (elementi con lo stesso `groupId` si spostano insieme).

**`"text"`** — `width: Float = 600`, `scale: Float = 1`, `text: String = ""`,
`styleId: String = "body"`, `fontId: String? = null`, `color: Long? = null`,
`bgColor: Long? = null`, `fontSize: Float? = null`, `decor: String? = null`.

`text` è markdown con tag inline `{c:#RRGGBB}…{/c}`, `{f:fontId}…{/f}`,
`{s:24}…{/s}`; `{c:@n}` referenzia lo slot accento n del tema.
`decor`: `null` = nessuna skin, `"auto"` = skin del tema, altrimenti un id fra
`glass`, `parchment`, `window`, `sketch`, `terminal`.
`styleId` ∈ `title1`, `title2`, `title3`, `body`, `caption`, o `custom_<uuid>`.

**`"image"`** — `width`, `height: Float = 400`, `fileName: String`,
`isPdfPage: Bool = false`, `pdfPage: Int = 0`.

**`"notelink"`** — `width: Float = 420`, `scale: Float = 1`,
`targetNoteId: String`.

**`"weblink"`** — `width: Float = 420`, `scale: Float = 1`, `url: String`,
`title: String`.

**`"file"`** — `width: Float = 380`, `scale: Float = 1`, `fileName: String`,
`displayName: String`, `mimeType: String`, `sizeBytes: Long`.

### strokes

`id: String`, `type: "PEN"|"HIGHLIGHTER"`, `color: Long = 0`,
`width: Float = 4`, `points: [{x, y, p}]` (`p` = pressione 0…1.5),
`lineStyle: "SOLID"|"DASHED"|"DOTTED"|null`, `animated: Bool = false`.

I punti sono in coordinate mondo del canvas infinito.

### connectors

`id`, `fromId: String`, `toId: String` (id di un elemento **o** di una
cornice), `color: Long = 0`, `width: Float = 3.5`, `lineStyle: …|null`,
`animated: Bool`, `startCap`/`endCap`: `"NONE"|"ARROW"|"DOT"`,
`curveDx`/`curveDy: Float` (offset del punto di controllo bezier),
`nodes: [{x, y, p}]`.

Con `nodes` vuoto la linea è una bezier quadratica; con N nodi è una spline
Catmull-Rom che passa per `[start] + nodes + [end]`.

### frames

`id`, `x`, `y`, `width: Float = 400`, `height: Float = 300`,
`shape: "RECT"|"ROUNDED"|"ELLIPSE"|"SKETCHY"|null`, `lineStyle: …|null`,
`animated: Bool`, `color: Long = 0`, `strokeWidth: Float = 3`,
`filled: Bool`, `label: String`, `decor: String?`, `autoFit: Bool = true`,
`memberIds: [String]`.

`decor`: `null` **e** `"auto"` significano entrambi "skin del tema";
`"none"` = solo bordo; altrimenti un id di skin.

Con `autoFit`, il rettangolo disegnato non è quello memorizzato: si calcola
dal contenuto (`memberIds`, o gli elementi geometricamente dentro se la lista
è vuota) con 22 unità di padding, e `width`/`height` fanno da minimo manuale.

### tapes

`id`, `x1`, `y1`, `x2`, `y2`, `thickness: Float = 36`, `color: Long = 0`,
`pattern: "SOLID"|"STRIPES"|"DOTS"|"ZIGZAG"|"GRID"|null`,
`alpha: Float = 0.85`.

### background

`"BLANK" | "DOTS" | "GRID" | "LINES" | "PAPER" | "SCANLINES"`.

## Ordine di disegno

Entrambe le app disegnano nello stesso ordine, dal basso verso l'alto:

1. sfondo (pattern del canvas)
2. cornici (skin + bordo)
3. connettori
4. etichette delle cornici
5. elementi (testo, immagini, schede)
6. washi tape
7. **inchiostro** (sempre sopra tutto)
