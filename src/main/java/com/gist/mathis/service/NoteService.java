package com.gist.mathis.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.Note;
import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.model.repository.NoteRepository;

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
        embeddingService.createNoteEmbeddings(note);
        return note;
    }
    
    public Note saveNote(Note note, Resource resource) {
        note = noteRepository.save(note);
        embeddingService.createDocumentNoteEmbeddings(note, resource);
        return note;
    }

    public void deleteNote(Long noteId) {
    	embeddingService.removeNoteEmbeddings(noteId);
        noteRepository.deleteById(noteId);
    }
}