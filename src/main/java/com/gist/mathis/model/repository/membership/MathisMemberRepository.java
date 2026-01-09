package com.gist.mathis.model.repository.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gist.mathis.model.entity.membership.MathisMember;

public interface MathisMemberRepository extends JpaRepository<MathisMember, Long> {

}
