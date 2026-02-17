package com.gist.mathis.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.MathisMessage;
import com.gist.mathis.model.repository.MathisMessageRepository;

@Service
public class MathisMessageService {
	@Autowired
	private MathisMessageRepository mathisMessageRepository;
	
	public List<MathisMessage> findAll() {
		return mathisMessageRepository.findAll();
	}

	public MathisMessage updateMathisMessage(MathisMessage mathisMessage) {
		mathisMessage.setUpdatedAt(null);
		mathisMessage = mathisMessageRepository.save(mathisMessage);
        return mathisMessage;
	}

	public void deleteById(Long id) {
		mathisMessageRepository.deleteById(id);
	}

}
