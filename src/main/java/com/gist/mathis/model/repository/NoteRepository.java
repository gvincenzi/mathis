package com.gist.mathis.model.repository;

import com.gist.mathis.model.entity.Note;
import com.gist.mathis.model.entity.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByNotebook(Notebook notebook);
	Optional<Note> findByNotebookAndTitle(Notebook notebook, String title);
}
