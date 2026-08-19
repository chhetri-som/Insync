# Insync — Java / Spring Boot Backend Specification

---

## 1. Technology Decisions

| Concern | Choice | Why |
|---|---|---|
| Language | Java 21 | LTS, virtual threads (Project Loom), modern records |
| Framework | Spring Boot 3.3 | Auto-configuration, mature ecosystem |
| Build Tool | Maven | Explicit, easy to read for learners |
| Database | PostgreSQL 16 | JSONB for flexible layout state, full ACID |
| ORM | Spring Data JPA + Hibernate | Reduces boilerplate, still lets you write SQL |
| Migrations | Flyway | Version-controlled schema changes |
| Image Processing | OpenCV (Java bindings) + Thumbnailator | CV algorithms + easy thumbnail generation |
| Async Queue | Spring's `@Async` + `ThreadPoolTaskExecutor` | No external broker needed to start; swap to RabbitMQ later |
| Real-time Push | Spring WebSocket (STOMP) | SSE is simpler but STOMP gives two-way comms for free |
| File Storage | Local disk (dev) → abstract behind a `StorageService` | Easy to swap to S3 later without touching business logic |
| Auth | Spring Security + JWT (stateless) | Stateless fits a React SPA |
| Testing | JUnit 5 + Mockito + Testcontainers | Unit + integration tests from day one |
| API Docs | SpringDoc OpenAPI (Swagger UI) | Auto-generated, always in sync |

---

## 2. Domain Model

### 2.1 Entity Relationship Overview

```
User ──< Album ──< Photo
                    │
                    └── PhotoMetadata (1-to-1)

Album ──< AlbumLayout (1-to-many versions, one is ACTIVE)
AlbumLayout.pages → JSONB document (PageLayout[] → SlotLayout[])
```

`LayoutPage` and `LayoutSlot` are **not database tables**. They are plain Java
records (`PageLayout`, `SlotLayout`) that live inside the `album_layouts.pages`
JSONB column. The layout is always loaded and saved as a whole document — never
queried by individual page or slot — so relational tables would add joins with
no benefit.

---

### 2.2 Entity Definitions

#### `User`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `email` | VARCHAR(255) | Unique, not null |
| `password_hash` | VARCHAR(255) | BCrypt |
| `display_name` | VARCHAR(100) | |
| `created_at` | TIMESTAMP | |

---

#### `Album`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `owner_id` | UUID | FK → User |
| `title` | VARCHAR(255) | |
| `style` | ENUM | `CHRONOLOGICAL`, `COLOR_PALETTE`, `PORTRAIT_GALLERY`, `HIGH_CONTRAST` |
| `status` | ENUM | `DRAFT`, `PROCESSING`, `READY`, `ERROR` |
| `created_at` | TIMESTAMP | |
| `updated_at` | TIMESTAMP | |

---

#### `Photo`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `album_id` | UUID | FK → Album |
| `original_storage_key` | VARCHAR(500) | Path/key to raw file |
| `processed_storage_key` | VARCHAR(500) | Path/key to auto-polished file |
| `thumbnail_storage_key` | VARCHAR(500) | |
| `processing_status` | ENUM | `PENDING`, `PROCESSING`, `DONE`, `FAILED` |
| `upload_order` | INT | Preserves upload sequence as fallback sort |
| `created_at` | TIMESTAMP | |

---

#### `PhotoMetadata`
Extracted EXIF + CV analysis results. Typed columns only — no raw EXIF blob.
Every column here is something an algorithm actually reads.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `photo_id` | UUID | FK → Photo (unique) |
| `taken_at` | TIMESTAMP | From EXIF; nullable |
| `latitude` | DECIMAL(10,7) | Nullable |
| `longitude` | DECIMAL(10,7) | Nullable |
| `dominant_colors` | JSONB | `[{"hex":"#3a1f0d","weight":0.42}, ...]` |
| `brightness_score` | FLOAT | 0.0–1.0, histogram mean |
| `contrast_score` | FLOAT | 0.0–1.0, pixel std deviation |
| `complexity_score` | FLOAT | 0.0–1.0, Canny edge density |
| `has_faces` | BOOLEAN | From face detector |
| `face_count` | INT | |

> `exif_raw` is intentionally omitted. We extract what the algorithms need into
> typed columns. The full raw EXIF dump is logged during extraction — not stored.

> `dominant_colors` is mapped in Java as `List<DominantColor>` (not `List<Map<String, Object>>`).
> `DominantColor` is a plain record `(String hex, double weight)` in `domain/model/`.
> Hibernate serializes it to the same JSONB shape; the record gives type-safe access in the strategies.

---

#### `AlbumLayout`
One complete, persisted layout snapshot.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `album_id` | UUID | FK → Album |
| `layout_type` | ENUM | `ALGORITHM` or `USER_OVERRIDE` |
| `is_active` | BOOLEAN | Only one active per type per album |
| `cover_photo_id` | UUID | FK → Photo |
| `pages` | JSONB | Full page/slot tree (see structure below) |
| `created_at` | TIMESTAMP | |

**`pages` JSONB structure:**
```json
[
  {
    "pageNumber": 1,
    "slots": [
      {
        "photoId":    "uuid-string",
        "position":   0,
        "cropX":      0.0,
        "cropY":      0.0,
        "cropWidth":  1.0,
        "cropHeight": 1.0,
        "isFullPage": false
      }
    ]
  }
]
```

---

### 2.3 In-Memory Value Objects (not persisted)

These are Java records used within the service layer. They have no `@Entity`
annotation and never touch the database directly.

| Class | Purpose |
|---|---|
| `PageLayout` | One page in a layout; contains `List<SlotLayout>` |
| `SlotLayout` | One image cell; contains photoId, position, crop params |
| `LayoutPreview` | Ephemeral result of a strategy run before the user confirms a style |

`LayoutPreview` is what the Style Wizard returns for all four style options.
Only after the user picks a style does the backend convert it into a saved
`AlbumLayout` row.

---

## 3. API Design (REST)

Base path: `/api/v1`

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Create account |
| POST | `/auth/login` | Returns JWT |
| POST | `/auth/refresh` | Refresh JWT |

---

### Albums
| Method | Path | Description |
|---|---|---|
| GET | `/albums` | List current user's albums |
| POST | `/albums` | Create album (title + style) |
| GET | `/albums/{id}` | Get album details + status |
| PATCH | `/albums/{id}` | Update title, style |
| DELETE | `/albums/{id}` | Delete album + all photos |

---

### Photos (within an album)
| Method | Path | Description |
|---|---|---|
| POST | `/albums/{id}/photos` | Upload 1–50 images (multipart) |
| GET | `/albums/{id}/photos` | List photos + metadata |
| DELETE | `/albums/{id}/photos/{photoId}` | Remove single photo |

---

### Layouts
| Method | Path | Description |
|---|---|---|
| GET | `/albums/{id}/layout/previews` | Run all 4 strategies in memory, return previews (not saved) |
| POST | `/albums/{id}/layout/confirm` | Confirm a style; saves that strategy's output as the active AlbumLayout |
| GET | `/albums/{id}/layout` | Get active layout (algo + user overrides merged) |
| POST | `/albums/{id}/layout/regenerate` | Force re-run algorithm |
| PUT | `/albums/{id}/layout/override` | Submit user's manual arrangement |
| DELETE | `/albums/{id}/layout/override` | Revert to algorithm layout |

---

### WebSocket Topics (STOMP)
| Destination | Direction | Payload |
|---|---|---|
| `/topic/albums/{id}/processing` | Server → Client | `{ photoId, status, progress }` |
| `/topic/albums/{id}/ready` | Server → Client | `{ layoutId }` — fires when all photos done |

---

## 4. Package Structure

```
com.insync
├── config/
│   ├── SecurityConfig.java
│   ├── AsyncConfig.java
│   ├── WebSocketConfig.java
│   └── StorageConfig.java
│
├── domain/
│   ├── model/
│   │   ├── User.java
│   │   ├── Album.java
│   │   ├── Photo.java
│   │   ├── PhotoMetadata.java
│   │   ├── AlbumLayout.java
│   │   ├── DominantColor.java     ← record, JSONB value type for PhotoMetadata
│   │   ├── PageLayout.java        ← record, not @Entity
│   │   ├── SlotLayout.java        ← record, not @Entity
│   │   └── LayoutPreview.java     ← record, never persisted
│   └── enums/
│       ├── AlbumStyle.java
│       ├── AlbumStatus.java
│       ├── PhotoProcessingStatus.java
│       └── LayoutType.java
│
├── repository/
│   ├── UserRepository.java
│   ├── AlbumRepository.java
│   ├── PhotoRepository.java
│   ├── PhotoMetadataRepository.java
│   └── AlbumLayoutRepository.java
│
├── service/
│   ├── AlbumService.java
│   ├── PhotoService.java
│   ├── LayoutService.java
│   ├── AuthService.java
│   │
│   ├── processing/
│   │   ├── PhotoProcessingService.java
│   │   ├── MetadataExtractor.java
│   │   ├── ImagePolishingService.java
│   │   └── ThumbnailService.java
│   │
│   ├── layout/
│   │   ├── BookLayoutStrategy.java       ← interface; returns LayoutPreview
│   │   ├── ChronologicalStrategy.java
│   │   ├── ColorPaletteStrategy.java
│   │   ├── PortraitGalleryStrategy.java
│   │   ├── HighContrastStrategy.java
│   │   └── LayoutStrategyFactory.java
│   │
│   └── storage/
│       ├── StorageService.java
│       └── LocalStorageService.java
│
├── web/
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── AlbumController.java
│   │   ├── PhotoController.java
│   │   └── LayoutController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RegisterRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   └── RefreshRequest.java
│   │   └── response/
│   │       └── AuthResponse.java
│   └── mapper/
│
├── security/
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   └── UserDetailsServiceImpl.java
│
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ErrorResponse.java
    ├── EmailAlreadyExistsException.java
    ├── ResourceNotFoundException.java
    └── ProcessingException.java
```

---

## 5. The Strategy Pattern (Deep Dive)

```
BookLayoutStrategy (interface)
        │
        ├── ChronologicalStrategy      → sorts by PhotoMetadata.takenAt
        ├── ColorPaletteStrategy       → K-means clusters by dominantColors
        ├── PortraitGalleryStrategy    → groups by hasFaces / faceCount
        └── HighContrastStrategy       → sorts by brightnessScore + complexityScore

LayoutStrategyFactory
    .getStrategy(AlbumStyle) → BookLayoutStrategy
```

Each strategy receives `List<Photo>` (with metadata eagerly loaded) and returns
a `LayoutPreview` — an in-memory arrangement that is **not saved to the DB**.

`LayoutService` is responsible for:
- Running all 4 strategies → returning 4 `LayoutPreview` objects for the Style Wizard
- Converting a confirmed `LayoutPreview` into a saved `AlbumLayout` row
- Merging the ALGORITHM and USER_OVERRIDE layouts when serving GET /layout

---

## 6. Async Processing Pipeline

```
PhotoController.upload()
    └──> PhotoService.saveRawFiles()        [saves to disk, creates Photo rows as PENDING]
    └──> PhotoProcessingService.processAsync(albumId, photoIds)   [@Async]
              │
              for each photo:
              ├── MetadataExtractor.extract()       → creates PhotoMetadata row
              ├── ImagePolishingService.polish()    → writes processed file
              ├── ThumbnailService.generate()       → writes thumbnail
              ├── Photo.status = DONE
              └── WebSocket: push progress to client
              │
              after all photos done:
              ├── LayoutService.generateLayout(album)  → runs Strategy → saves AlbumLayout
              ├── Album.status = READY
              └── WebSocket: push "album ready" event
```

---

## 7. State Merging Logic

Two `AlbumLayout` rows can exist per album at most:
- `layout_type = ALGORITHM` — always present once processing completes
- `layout_type = USER_OVERRIDE` — present only after the user edits

**GET `/albums/{id}/layout`** merges them server-side in `AlbumLayout.mergeWith()`:
1. Start with ALGORITHM layout's `pages` as base
2. For each page in USER_OVERRIDE, replace the corresponding base page
3. Return the merged page list to the frontend

**DELETE `/albums/{id}/layout/override`** marks the USER_OVERRIDE row `is_active = false`.
The ALGORITHM layout is always preserved underneath — reverting is free.

---

## 8. Security Model

- All endpoints except `/auth/**` require a valid JWT in `Authorization: Bearer <token>`
- JWT payload: `{ sub: userId, email, iat, exp }`
- Users can only access their own albums — ownership enforced at the repository query level
- File uploads validated: allowed MIME types `image/jpeg`, `image/png`, `image/webp`; max 20MB per file

---

## 9. Build Order (Recommended Learning Path)

1. **Project scaffold** — Spring Initializr, `pom.xml` dependencies, folder structure 
2. **Database + Migrations** — Flyway scripts for all tables, connect to PostgreSQL 
3. **Domain entities + Repositories** — JPA entities, value objects, basic CRUD repos 
4. **Auth** — Register/login, JWT generation + filter 
5. **Album + Photo CRUD** — Create album, upload files, list photos
6. **Metadata Extraction** — EXIF parsing with Apache Commons Imaging
7. **Image Polishing** — OpenCV histogram eq + unsharp mask
8. **Async Pipeline** — Wire steps 6+7 into `@Async` with WebSocket progress
9. **Strategy Pattern** — Implement all 4 layout strategies + factory
10. **Layout API** — Generate, serve, override, revert
11. **Cover Selection** — Pick the highest contrast image automatically
12. **Tests** — Unit tests for strategies, integration tests for upload flow

---

## Update

Honestly, I was building this web app using mostly Claude and at some point it got really overwhelming so I stopped working on this project. 
The whole point of this project was to learn the SpringBoot framework, Claude built this app pretty efficiently but I wasn't really able to learn much.
Hence, I have shifted to other beginner friendly builds and have stopped this project indefinitely, IDK when I might start this again, but I don't wanna delete this repo either.
