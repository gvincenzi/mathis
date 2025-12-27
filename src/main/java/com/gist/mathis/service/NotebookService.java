package com.gist.mathis.service;

import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.model.entity.User;
import com.gist.mathis.model.repository.NotebookRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotebookService {

    @Autowired
    private NotebookRepository notebookRepository;

    public List<Notebook> findByUser(User user) {
        return notebookRepository.findByUser(user);
    }

    public Notebook saveNotebook(Notebook notebook) {
        return notebookRepository.save(notebook);
    }

    public void deleteNotebook(Long notebookId) {
        notebookRepository.deleteById(notebookId);
    }
}
