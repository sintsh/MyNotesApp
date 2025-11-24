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

## Code Walkthrough
### Data Model (`app/src/main/java/com/example/mynotesapp/Models/Note.kt`)
- Annotated with `@Entity(tableName = "notes_table")`, so Room auto-creates the table.
- Nullable `id` lets Room autogenerate primary keys during insertion.
- Implements `Serializable`, which makes passing a whole `Note` between Activities easy.

```kotlin
@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "date") val date: String?
) : Serializable
```

### Data Access (`Database/NoteDao.kt`, `NoteDatabase.kt`, `NotesRepository.kt`)
- `NoteDao` declares suspend CRUD methods; Room generates the implementation.
- `NoteDatabase` wraps the Room builder in a singleton so only one DB instance exists.
- `NotesRepository` is intentionally thin—it simply forwards calls to the DAO but gives us a centralized place to swap data sources later (e.g., add caching or network).

```kotlin
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("SELECT * FROM notes_table ORDER BY id ASC")
    fun getAllNotes(): LiveData<List<Note>>
}
```

### ViewModel (`Models/NoteViewModel.kt`)
- Holds `val allNotes: LiveData<List<Note>>` coming from the repository.
- Exposes `insertNote`, `deleteNote`, and `updateNote`, each launching coroutines on `Dispatchers.IO`.
- Keeps Activities unaware of the database implementation—UI simply observes changes and calls these helpers.

```kotlin
fun insertNote(note: Note) = viewModelScope.launch(Dispatchers.IO) {
    repository.insert(note)
}
```

### UI Layer (`MainActivity.kt`, `AddNote.kt`, `Adapter/NotesAdapter.kt`)
- `MainActivity` sets up `RecyclerView`, observes `viewModel.allNotes`, and updates the adapter whenever the list changes.
- Uses `registerForActivityResult` twice: once for creating notes and once for editing existing notes. Returned `Note` objects are passed straight back to the ViewModel.
- `AddNote` handles both create and edit flows. If it receives `current_note`, it prepopulates inputs and updates that record; otherwise it creates a new `Note`.
- `NotesAdapter` binds note data to `list_item.xml`, raises click + long-press events, and supports a simple `filterList(query)` search.

This section should give you a mental map for navigating the code: start at the Activity to see user events, follow calls into the ViewModel, then step into the repository/DAO as needed.

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