package com.example.mynotesapp.Models



import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.mynotesapp.Database.NoteDatabase
import com.example.mynotesapp.Database.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotesRepository
    val allNotes: LiveData<List<Note>>

    // The init block gets executed as soon as the ViewModel is created
    init {
        val dao = NoteDatabase.getDatabase(application).getNoteDao()
        repository = NotesRepository(dao)
        allNotes = repository.allNotes
    }

    /**
     * Deletes a Note.
     * We don't want to block the UI thread, so we run the delete operation on a new Coroutine.
     */
    fun deleteNote(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(note)
    }

    /**
     * Inserts a Note.
     * We don't want to block the UI thread, so we run the insert operation on a new Coroutine.
     */
    fun insertNote(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(note)
    }

    /**
     * Updates an existing Note.
     * We don't want to block the UI thread, so we run the update operation on a new Coroutine.
     */
    fun updateNote(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(note)
    }
}