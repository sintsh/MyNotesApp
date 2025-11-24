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
### `Models/Note.kt`
- Room `@Entity` describing a note record; nullable primary key supports auto-generation.
- Implements `Serializable` for easy passing through intents.

```kotlin
@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "date") val date: String?
) : Serializable
```

### Data Access Layer
#### `Database/NoteDao.kt`
- Declares suspend CRUD operations. Room generates query code at compile time.
- `getAllNotes()` returns `LiveData<List<Note>>`, which plugs straight into the ViewModel.

```kotlin
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Delete suspend fun delete(note: Note)
    @Update suspend fun update(note: Note)

    @Query("SELECT * FROM notes_table ORDER BY id ASC")
    fun getAllNotes(): LiveData<List<Note>>
}
```

#### `Database/NoteDatabase.kt`
- Annotated with `@Database(entities = [Note::class], version = 1)`.
- Exposes `abstract fun getNoteDao(): NoteDao`.
- Uses a `companion object` + `@Volatile` singleton so the Room instance is created once app-wide.

```kotlin
companion object {
    @Volatile private var INSTANCE: NoteDatabase? = null

    fun getDatabase(context: Context): NoteDatabase =
        INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                NoteDatabase::class.java,
                DATABASE_NAME
            ).build().also { INSTANCE = it }
        }
}
```

#### `Database/NotesRepository.kt`
- Provides a clean API for the rest of the app.
- Lazily exposes `val allNotes = noteDao.getAllNotes()`.
- Wraps suspend DAO calls so future data sources (network, cache) can be inserted without touching UI code.

### `Models/NoteViewModel.kt`
- Extends `AndroidViewModel` to access application context for DB creation.
- Holds `val allNotes: LiveData<List<Note>>` and exposes coroutine-backed helpers.
- Ensures database work stays on the IO dispatcher.

```kotlin
fun insertNote(note: Note) = viewModelScope.launch(Dispatchers.IO) {
    repository.insert(note)
}
```

### UI Layer
#### `MainActivity.kt`
- Wires up `RecyclerView` with a `StaggeredGridLayoutManager` and `NotesAdapter`.
- Observes `viewModel.allNotes` to refresh the adapter automatically.
- Uses two `registerForActivityResult` launchers: one for creating notes, one for editing existing entries.
- Handles search (delegates to adapter filter) and displays a contextual `PopupMenu` for deletions.

```kotlin
viewModel.allNotes.observe(this) { list ->
    list?.let { adapter.updateList(it) }
}
```

#### `AddNote.kt`
- Reused for both “add” and “edit” flows. If a `current_note` extra exists, fields are prefilled.
- Builds a `Note` object, stamps it with a formatted date, and returns it via `setResult`.
- Basic validation ensures empty notes aren’t saved; `Toast` provides feedback.

```kotlin
if (isUpdate) {
    note = old_note.copy(title = title, note = noteDesc, date = formatter.format(Date()))
} else {
    note = Note(null, title, noteDesc, formatter.format(Date()))
}
```

#### `Adapter/NotesAdapter.kt`
- Holds a mutable list of notes plus a backup copy used for search filtering.
- Emits callbacks through `NotesClickListener` for short tap and long press.
- `onBindViewHolder` binds title, snippet, and date; `filterList(query)` performs case-insensitive matching across title/note body.

```kotlin
fun filterList(query: String) {
    val filtered = oldNotes.filter {
        it.title?.contains(query, ignoreCase = true) == true ||
        it.note?.contains(query, ignoreCase = true) == true
    }
    notes.clear()
    notes.addAll(filtered)
    notifyDataSetChanged()
}
```

#### `Utilties/constants.kt`
- Stores `const val DATABASE_NAME = "notes_database"`, keeping the string in one place for reuse by `NoteDatabase`.

This walkthrough gives you a file-by-file roadmap: start from UI events in `MainActivity`/`AddNote`, follow the ViewModel into the repository, and drill into Room components as needed.

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