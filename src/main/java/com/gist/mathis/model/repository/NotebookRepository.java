package com.gist.mathis.model.repository;

import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, Long> {
    List<Notebook> findByUser(User user);
}
