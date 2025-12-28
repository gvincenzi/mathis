package com.gist.mathis.model.repository;

import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, Long> {
    List<Notebook> findByUser(User user);
    Optional<Notebook> findByUserAndTitle(User user, String title);
}
