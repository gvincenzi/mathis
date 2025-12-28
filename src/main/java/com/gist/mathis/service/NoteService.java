package com.gist.mathis.service;

import com.gist.mathis.model.entity.Note;
import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.model.repository.NoteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NoteService {
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private EmbeddingService embeddingService;

    public List<Note> findByNotebook(Notebook notebook) {
        return noteRepository.findByNotebook(notebook);
    }
    
    public Optional<Note> findByNotebookAndTitle(Notebook notebook, String title) {
        return noteRepository.findByNotebookAndTitle(notebook,title);
    }

    public Note saveNote(Note note) {
        note = noteRepository.save(note);
        embeddingService.noteEmbeddings(note);
        return note;
    }

    public void deleteNote(Long noteId) {
        noteRepository.deleteById(noteId);
    }
}