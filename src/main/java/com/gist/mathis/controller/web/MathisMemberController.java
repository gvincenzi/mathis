package com.gist.mathis.controller.web;

import com.gist.mathis.model.entity.MathisUser;
import com.gist.mathis.model.entity.memebrship.MathisMember;
import com.gist.mathis.model.entity.memebrship.MathisPositionHeldEnum;
import com.gist.mathis.service.membership.MathisMemberService;
import com.gist.mathis.service.security.MathisUserDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Controller
@RequestMapping("/web/members")
public class MathisMemberController {

    @Autowired
    private MathisMemberService mathisMemberService;

    @Autowired
    private MathisUserDetailsService mathisUserDetailsService;

    @GetMapping
    public String listMembers(Model model) {
        List<MathisMember> members = mathisMemberService.findAll();
        List<MathisUser> users = mathisUserDetailsService.findAll();
        model.addAttribute("members", members);
        model.addAttribute("users", users);
        return "member";
    }

    @GetMapping("/new")
    public String newMemberForm(Model model) {
        List<MathisUser> users = mathisUserDetailsService.findAll();
        model.addAttribute("users", users);
        return "member_create";
    }

    @PostMapping("/create")
    public String createMember(
            @RequestParam String mathisUserId,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam String placeOfBirth,
            @RequestParam String taxCode,
            @RequestParam String residenceAddress,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate enrollmentDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate,
            @RequestParam(required = false) MathisPositionHeldEnum positionsHeld
    ) {
    	MathisUser user = null;
        if (mathisUserId != null && !mathisUserId.isBlank()) {
            user = mathisUserDetailsService.findById(Long.parseLong(mathisUserId)).orElse(null);
        }
        MathisMember member = new MathisMember();
        member.setMathisUser(user);
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setDateOfBirth(dateOfBirth);
        member.setPlaceOfBirth(placeOfBirth);
        member.setTaxCode(taxCode);
        member.setResidenceAddress(residenceAddress);
        member.setEnrollmentDate(enrollmentDate);
        member.setTerminationDate(terminationDate);
        member.setPositionsHeld(positionsHeld);

        mathisMemberService.save(member);
        return "redirect:/web/members";
    }

    @PostMapping("/update/{id}")
    public String updateMember(
            @PathVariable Long id,
            @RequestParam Long mathisUserId,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam String placeOfBirth,
            @RequestParam String taxCode,
            @RequestParam String residenceAddress,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate enrollmentDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate,
            @RequestParam(required = false) MathisPositionHeldEnum positionsHeld
    ) {
        Optional<MathisMember> memberOpt = mathisMemberService.findById(id);
        Optional<MathisUser> userOpt = mathisUserDetailsService.findById(mathisUserId);
        if (memberOpt.isEmpty() || userOpt.isEmpty()) {
            return "redirect:/web/members?error=not_found";
        }

        MathisMember member = memberOpt.get();
        member.setMathisUser(userOpt.get());
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setDateOfBirth(dateOfBirth);
        member.setPlaceOfBirth(placeOfBirth);
        member.setTaxCode(taxCode);
        member.setResidenceAddress(residenceAddress);
        member.setEnrollmentDate(enrollmentDate);
        member.setTerminationDate(terminationDate);
        member.setPositionsHeld(positionsHeld);

        mathisMemberService.save(member);
        return "redirect:/web/members";
    }

    // DELETE
    @GetMapping("/delete/{id}")
    public String deleteMember(@PathVariable Long id) {
    	mathisMemberService.deleteById(id);
        return "redirect:/web/members";
    }
}
