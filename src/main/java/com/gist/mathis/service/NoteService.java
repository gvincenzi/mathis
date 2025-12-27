package com.gist.mathis.service;

import com.gist.mathis.model.entity.Note;
import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.model.repository.NoteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NoteService {

    @Autowired
    private NoteRepository noteRepository;

    public List<Note> findByNotebook(Notebook notebook) {
        return noteRepository.findByNotebook(notebook);
    }

    public Note saveNote(Note note) {
        return noteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        noteRepository.deleteById(noteId);
    }
}