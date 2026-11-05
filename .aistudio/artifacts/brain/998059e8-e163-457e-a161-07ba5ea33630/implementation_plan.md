# Scan3D Studio - Scanner 3D Mobile & Esportazione OBJ

Un'applicazione Android moderna e performante in Jetpack Compose che trasforma la fotocamera dello smartphone in uno scanner 3D ad alta precisione, con guida sensoriale orbitale a 360°, ricostruzione geometrica fotogrammetrica, visualizzatore 3D interattivo completo ed esportazione diretta di modelli in formato standard Wavefront OBJ.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> In base alle tue risposte durante la fase di chiarimento, abbiamo definito le seguenti scelte progettuali chiave:

- **Approccio di Scansione**: **Fotogrammetria guidata con sensori e cattura multi-angolo**. L'utente viene assistito da un'interfaccia AR/sensoriale (giroscopio e accelerometro) con anello orbitale a 360° e indicatori di posa per scattare una sequenza ottimale di fotogrammi attorno all'oggetto.
- **Visualizzatore 3D Integrato**: **Viewer 3D interattivo completo** con rotazione su assi tridimensionali tramite tocco, pinch-to-zoom, pan, e 4 modalità di visualizzazione:
  1. *Texture & Colori* (render con interpolazione cromatica delle facce e illuminazione direzionale);
  2. *Shading Solido / Lambert* (illuminazione con calcolo delle normali di superficie);
  3. *Wireframe Poligonale* (griglia delle facce e degli spigoli);
  4. *Nuvola di Punti / Vertici* (visualizzazione della point cloud originale).
- **Esportazione OBJ**: **File OBJ standard (.obj)** ottimizzato con vertici geometrici `v`, normali `vn` e facce triangolari `f`. Massima compatibilità nativa con Blender, MeshLab, 3ds Max, Maya e software di stampa 3D (Cura, PrusaSlicer), con foglio di condivisione di sistema Android (`Intent.ACTION_SEND`) e download locale.

---

## 1. Overview & Core Concept

### Cos'è Scan3D Studio
Scan3D Studio consente a chiunque possieda uno smartphone Android di digitalizzare oggetti reali in modelli tridimensionali esportabili senza bisogno di sensori LiDAR proprietari o hardware esterno. Sfruttando la fotocamera integrata tramite CameraX e i sensori di movimento del dispositivo (SensorManager), l'app guida l'utente a circondare l'oggetto, estrae i punti di interesse salienti e genera una mesh poligonale triangolare ottimizzata.

### Destinatari & Casi d'Uso
- **Designer 3D & Modellatori**: Acquisizione rapida di referenze reali o base-mesh da rifinire in Blender/ZBrush.
- **Makers & Appassionati di Stampa 3D**: Scansione di oggetti fisici per reverse engineering o repliche in stampa 3D.
- **Studenti, Maker & Sviluppatori Indie**: Digitalizzazione di oggetti quotidiani per prototipi di giochi o realtà aumentata.

---

## 2. User Experience & Visual Design

### Flussi Utente Principali

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  Home / Scans   │ ----> │ Scanning Camera │ ----> │ Reconstruction  │ ----> │ 3D Model Viewer │
│  Library Screen │       │ & Sensor Guide  │       │ & Mesh Engine   │       │ & OBJ Export    │
└─────────────────┘       └─────────────────┘       └─────────────────┘       └─────────────────┘
```

1. **Libreria & Home (Zero-State & Scansioni Recenti)**:
   - Dashboard con card delle scansioni recenti (miniatura anteprima 3D, nome, data, numero vertici/poligoni, peso file).
   - Pulsante flottante primario ad alto contrasto **"Nuova Scansione 3D"**.
   - Filtri e barra di stato per gestire i file salvati.

2. **Cattura Guidata con Sensori (Scanner Camera)**:
   - Preview fotocamera a schermo intero con CameraX.
   - Mirino centrale con bounding box olografico e orizzonte artificiale giroscopico.
   - Anello orbitale a 360° con settori angolari che si illuminano di verde all'acquisizione di ogni prospettiva chiave (angolo orizzontale e zenitale).
   - Modalità di scatto automatica (al raggiungimento del nuovo angolo stabile) o manuale.
   - Barra di avanzamento dell'acquisizione (es. 12-24 campioni angolari consigliati).

3. **Elaborazione & Ricostruzione Mesh**:
   - Schermata di computazione con barra di progresso a 4 stadi:
     1. *Analisi visiva e corrispondenza caratteristiche*
     2. *Generazione nuvola di punti densa (Point Cloud)*
     3. *Triangolazione mesh di superficie (Delaunay/Ball-Pivoting)*
     4. *Calcolo normali ed estrazione texture/colori vertici*
   - Visualizzazione in tempo reale dei punti generati durante il calcolo.

4. **Viewer 3D Interattivo & Esportazione**:
   - Spazio 3D interattivo con controlli touch: rotazione orbitale libera con trascinamento, pinch-to-zoom a 2 dita, doppio tocco per resettare la visuale.
   - Toolbar strumenti di rendering: switch rapido tra *Solido*, *Wireframe*, *Texture/Colori*, *Punti*.
   - Scheda tecnica: conteggio vertici, facce triangolari, dimensioni stimate (X, Y, Z in cm).
   - Azione di esportazione: **"Esporta OBJ"** con menu a comparsa per:
     - Condividere su WhatsApp, Telegram, Google Drive, Email (`ACTION_SEND`).
     - Salvare direttamente nella cartella *Download* o cartella documenti dell'utente.

### Identità Visiva & Tema (Material Design 3)
- **Palette Colori**: Ispirata ai software CAD e 3D professionali contemporanei (dark mode elegante con accenti high-tech):
  - *Sfondo Principale*: Dark Charcoal `#121417`
  - *Superfici e Card*: Slate Elevation `#1C1F26` e `#242833`
  - *Accento Primario*: Ciano Elettrico / Laser Blue `#00D2FF`
  - *Accento Secondario / Guida*: Verde Neon `#10B981` (per angoli completati)
  - *Wireframe & Grid*: Grigio Tecnico Semitrasparente `#404859`
- **Tipografia**: Caratteri puliti, moderni, con numeri tabulari chiari per vertici e coordinate 3D.
- **Feedback & Micro-animazioni**:
  - Vibrazione aptica al rilevamento e scatto di un nuovo angolo.
  - Transizioni fluide animate tra camera, elaborazione e viewer 3D.

---

## 3. Key Product Decisions & Trade-Offs

- **Viewer 3D nativo in Compose (Canvas con Math 3D & Projection Engine)**:
  - *Scelta*: Implementazione di un motore di proiezione 3D (matrice Model-View-Projection, z-sorting, culling delle facce posteriori e illuminazione Lambertiana su Canvas Compose).
  - *Motivazione*: Nessuna dipendenza da motori terzi pesanti e obsoleti, avvio istantaneo a 60 FPS, piena reattività con il ciclo di vita di Compose.
- **Specifiche Formato File OBJ**:
  - *Scelta*: Formato Wavefront OBJ ASCII standard (`.obj`).
  - *Header*: Metadati del generatore (Scan3D Studio), data e numero elementi.
  - *Geometria*: Righe `v x y z r g b` (coordinate e colori vertice), `vn nx ny nz` (vettori normali normalizzati) e `f v1//vn1 v2//vn2 v3//vn3` (facce triangolari con indicizzazione 1-based).
  - *Compatibilità*: Riconosciuto da qualsiasi tool 3D al mondo senza bisogno di plugin.
- **Archiviazione Locale & Privacy**:
  - *Scelta*: I file OBJ e i dati delle scansioni vengono salvati nell'archivio locale dell'app con esportazione verso lo storage pubblico tramite SAF (Storage Access Framework) e `FileProvider` per la condivisione.
  - *Motivazione*: Massima velocità, nessun consumo di dati di rete, totale rispetto della privacy dell'utente.

---

## 4. Technical Architecture & Data Strategy

### Diagramma di Architettura & Componenti

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             MainActivity & UI                              │
│                                                                             │
│  ┌───────────────────────┐   ┌───────────────────────┐   ┌──────────────┐  │
│  │   ScanLibraryScreen   │   │     ScannerScreen     │   │ ModelViewer  │  │
│  │   (Elenco Scansioni)  │   │ (CameraX + OrientSens)│   │ 3D Screen    │  │
│  └───────────┬───────────┘   └───────────┬───────────┘   └──────┬───────┘  │
└──────────────┼───────────────────────────┼──────────────────────┼───────────┘
               │                           │                      │
               ▼                           ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ScannerViewModel                                  │
│  - Gestione stato scansione (Idle, Capturing, Processing, Viewing)          │
│  - Stato visualizzatore 3D (rotazione, zoom, modalità rendering)            │
│  - Trigger scatto ed esportazione file OBJ                                 │
└──────────────┬───────────────────────────┬──────────────────────┬───────────┘
               │                           │                      │
               ▼                           ▼                      ▼
┌─────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────┐
│     SensorTracker       │ │     Mesh3DEngine          │ │    ObjExporter    │
│  - Accelerometro        │ │  - Triangolazione mesh    │ │  - Generatore     │
│  - Giroscopio           │ │  - Calcolo vertici/facce  │ │    formato OBJ    │
│  - Orientamento orbitale│ │  - Normali & illuminazione│ │  - FileProvider   │
└─────────────────────────┘ └───────────────────────────┘ └───────────────────┘
               │                           │                      │
               └───────────────────────────┴──────────────────────┘
                                           │
                                           ▼
                            ┌─────────────────────────────┐
                            │      Local File Storage     │
                            │  - Directory Scansioni      │
                            │  - File .obj e miniature    │
                            └─────────────────────────────┘
```

### Struttura Dati Principale

```kotlin
data class Vertex3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val color: Int = Color.White.toArgb()
)

data class Face3D(
    val v1Index: Int,
    val v2Index: Int,
    val v3Index: Int,
    val normal: Vector3D
)

data class Model3D(
    val id: String,
    val name: String,
    val timestamp: Long,
    val vertices: List<Vertex3D>,
    val faces: List<Face3D>,
    val objFilePath: String,
    val boundingBox: BoundingBox3D
)
```

---

## Prossimi Passi
Una volta approvato questo piano di implementazione, procederemo con:
1. Configurazione permessi fotocamera in `AndroidManifest.xml` e abilitazione dipendenze CameraX in `build.gradle.kts`.
2. Sviluppo del modulo di tracciamento orientamento sensoriale e cattura orbitale.
3. Creazione del motore di calcolo geometrico e triangolazione mesh 3D.
4. Realizzazione del visualizzatore 3D interattivo con shader, wireframe, zoom e rotazione.
5. Implementazione del generatore/esportatore di file `.obj` compatibile con condivisione nativa Android.
6. Test e verifica completa della compilazione.
