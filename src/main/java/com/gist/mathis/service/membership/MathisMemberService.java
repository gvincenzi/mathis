package com.gist.mathis.service.membership;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.membership.MathisMember;
import com.gist.mathis.model.repository.membership.MathisMemberRepository;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

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
	
	public byte[] getMemberCard(Long memberId) throws NoSuchElementException, JRException {
		MathisMember member = mathisMemberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException(String.format("User with id %d does not exists",memberId)));
		
	    InputStream reportStream = getClass().getResourceAsStream("/templates/tessera.jrxml");
	    JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

	    Map<String, Object> params = new HashMap<>();
	    params.put("firstName", member.getFirstName());
	    params.put("lastName", member.getLastName());
	    params.put("taxCode", member.getTaxCode());
	    params.put("role", member.getPositionsHeld().toString());
	    params.put("enrollmentDate", member.getEnrollmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
	    params.put("placeOfBirth", member.getPlaceOfBirth());
	    params.put("dateOfBirth", member.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
	    params.put("residenceAddress", member.getResidenceAddress());

	    JRDataSource dataSource = new JREmptyDataSource();

	    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    JasperExportManager.exportReportToPdfStream(jasperPrint, baos);

	    return baos.toByteArray();
	}
}
