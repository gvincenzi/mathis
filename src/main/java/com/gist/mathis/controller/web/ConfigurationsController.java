package com.gist.mathis.controller.web;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gist.mathis.service.MathisJobService;
import com.gist.mathis.service.entity.MathisJob;
import com.gist.mathis.service.entity.MathisJobTypeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/web/configurations")
public class ConfigurationsController {

    @Autowired
    private MathisJobService mathisJobService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String configurations(Model model) {
        Map<String, MathisJob> allJobs = mathisJobService.getRegisteredJobs();

        Map<String, MathisJob> ingesters = allJobs.values().stream()
                .filter(job -> job.getType() == MathisJobTypeEnum.INGESTER)
                .collect(Collectors.toMap(MathisJob::getId, job -> job));
        Map<String, MathisJob> processors = allJobs.values().stream()
                .filter(job -> job.getType() == MathisJobTypeEnum.PROCESSOR)
                .collect(Collectors.toMap(MathisJob::getId, job -> job));

        model.addAttribute("ingesters", ingesters);
        model.addAttribute("processors", processors);

        return "configurations";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/job/{jobId}/toggle")
    public String toggleJob(@PathVariable String jobId, RedirectAttributes redirectAttributes) {
        MathisJob job = mathisJobService.getRegisteredJobs().get(jobId);
        if (job == null) {
            redirectAttributes.addFlashAttribute("error", "Job non found: " + jobId);
            return "redirect:/web/configurations";
        }
        if (job.getEnabled()) {
            mathisJobService.disable(jobId);
        } else {
            mathisJobService.enable(jobId);
        }
        return "redirect:/web/configurations";
    }
}
