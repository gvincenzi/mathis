package com.gist.mathis.model.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gist.mathis.model.entity.Knowledge;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {
	Set<Knowledge> findAllByOrderByCreatedAtAsc();
}
