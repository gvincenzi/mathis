package com.gist.mathis.model.repository;

import com.gist.mathis.model.entity.AuthorityEnum;
import com.gist.mathis.model.entity.MathisUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MathisUserRepository extends JpaRepository<MathisUser, Long> {
    Optional<MathisUser> findByUsername(String username);
    List<MathisUser> findByAuth(AuthorityEnum auth);
}
