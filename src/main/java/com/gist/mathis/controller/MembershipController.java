package com.gist.mathis.controller;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gist.mathis.service.membership.MathisMemberService;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;

@Slf4j
@RestController
@RequestMapping("/api/membership")
public class MembershipController {
	@Autowired
	private MathisMemberService mathisMemberService;
	
	@GetMapping("/card/{memberId}")
    public ResponseEntity<Resource> getMemberCard(@PathVariable("memberId") Long memberId) {
    	ByteArrayResource resource;
		try {
			resource = new ByteArrayResource(mathisMemberService.getMemberCard(memberId));
	        String fileName = "card#" + memberId + ".pdf";
	        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").contentLength(resource.contentLength()).contentType(MediaType.APPLICATION_JSON).body(resource);
		} catch (NoSuchElementException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		} catch (JRException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
