package com.gist.mathis.controller;

import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.service.NotebookService;
import com.gist.mathis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notebooks")
public class NotebookController {

    @Autowired
    private NotebookService notebookService;

    @Autowired
    private UserService userService;

    @GetMapping("/user/{username}")
    public ResponseEntity<List<Notebook>> getNotebooksByUser(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(notebookService.findByUser(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Notebook> createNotebook(@RequestBody Notebook notebook) {
        return ResponseEntity.ok(notebookService.saveNotebook(notebook));
    }

    @DeleteMapping("/{notebookId}")
    public ResponseEntity<Void> deleteNotebook(@PathVariable Long notebookId) {
        notebookService.deleteNotebook(notebookId);
        return ResponseEntity.noContent().build();
    }
}

