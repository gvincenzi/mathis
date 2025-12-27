package com.gist.mathis.controller;

import com.gist.mathis.model.entity.Note;
import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.service.NoteService;
import com.gist.mathis.service.NotebookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Autowired
    private NotebookService notebookService;

    @GetMapping("/notebook/{notebookId}")
    public ResponseEntity<List<Note>> getNotesByNotebook(@PathVariable Long notebookId) {
        Optional<Notebook> notebook = notebookService.findById(notebookId);
        return notebook.map(value -> ResponseEntity.ok(noteService.findByNotebook(value)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Note> createNote(@RequestBody Note note) {
        return ResponseEntity.ok(noteService.saveNote(note));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }
}
