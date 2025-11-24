# MyNotesApp

MyNotesApp is a lightweight Android notes manager written in Kotlin. It focuses on quick capture, edit, and deletion of short notes while keeping the codebase clean and approachable for MVVM + Room learning.

## What the App Does
- Creates notes with a title, body, and timestamp (`AddNote` screen).
- Lists notes in a `RecyclerView` (`MainActivity` + `NotesAdapter`).
- Persists data locally via Room (`Note`, `NoteDao`, `NoteDatabase`).
- Keeps UI updates reactive with `LiveData` observed from `NoteViewModel`.
- Performs database work off the main thread using Kotlin coroutines.

## Why MVVM?
The project is intentionally structured around the Model-View-ViewModel pattern to amplify separation of concerns:

- **Model layer**
  - `Note`: `@Entity` describing a row in `notes_table`.
  - `NoteDao`: suspend DAO with `insert`, `delete`, `update`, `getAllNotes`.
  - `NoteDatabase`: Room singleton builder exposed through `getNoteDao()`.
  - `NotesRepository`: simple abstraction that hides the DAO from higher layers.

- **View layer**
  - `MainActivity` observes `LiveData<List<Note>>` and binds it to `NotesAdapter`.
  - `AddNote` collects user input, sends it back via intents or direct ViewModel calls.
  - Layouts in `res/layout` use ViewBinding for null-safe view access.

- **ViewModel layer**
  - `NoteViewModel` owns `allNotes` LiveData and exposes helper methods (`insertNote`, `deleteNote`, `updateNote`). Each helper launches a coroutine on `Dispatchers.IO`, then delegates to `NotesRepository`.

### MVVM Flow in This Codebase
1. User taps add/edit/delete in an Activity.
2. Activity calls the relevant `NoteViewModel` function.
3. ViewModel launches a coroutine, calls into `NotesRepository`, which calls the appropriate DAO suspend function.
4. Room updates the SQLite table and emits new data via `LiveData`.
5. Activity observes the `LiveData`, submits the new list to `NotesAdapter`, and the UI refreshes automatically.

This approach keeps Activities lean, minimizes mutable shared state, and makes it straightforward to plug in tests or swap UI pieces later.

## Tech Stack
- Kotlin, Coroutines, ViewBinding
- AndroidX AppCompat, RecyclerView, Material Components
- Lifecycle (LiveData, ViewModel)
- Room persistence library

## Project Structure
```
app/
 └── src/main/java/com/example/mynotesapp
     ├── Adapter/NotesAdapter.kt
     ├── Database/{NoteDao, NoteDatabase, NotesRepository}.kt
     ├── Models/{Note, NoteViewModel}.kt
     ├── MainActivity.kt
     └── AddNote.kt
```

## Getting Started
1. Open the project in Android Studio (Giraffe or newer).
2. Sync Gradle; versions are declared in `gradle/libs.versions.toml`.
3. Plug in a device/emulator running API 24+.
4. Run `app`. Interact with the UI to add, edit, or delete notes.

## Extending the App
- Add search or filtering by title/date.
- Attach reminders/alarms by integrating WorkManager.
- Sync to cloud storage (Firebase, Supabase, etc.) by swapping the repository implementation while keeping the MVVM contract intact.