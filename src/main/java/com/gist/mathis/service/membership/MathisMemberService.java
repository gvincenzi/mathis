package com.gist.mathis.service.membership;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.memebrship.MathisMember;
import com.gist.mathis.model.repository.membership.MathisMemberRepository;

@Service
public class MathisMemberService {
	@Autowired
	private MathisMemberRepository mathisMemberRepository;

	public List<MathisMember> findAll() {
		return mathisMemberRepository.findAll();
	}

	public void save(MathisMember member) {
		mathisMemberRepository.save(member);
	}

	public Optional<MathisMember> findById(Long id) {
		return mathisMemberRepository.findById(id);
	}

	public void deleteById(Long id) {
		mathisMemberRepository.deleteById(id);
	}
}
