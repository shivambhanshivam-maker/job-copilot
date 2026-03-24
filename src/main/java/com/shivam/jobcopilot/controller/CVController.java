package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.CvNameResponse;
import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.service.CVService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cvs")
public class CVController {

    private final CVService cvService;

    public CVController(CVService cvService) {
        this.cvService = cvService;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public CV create(@RequestParam("file") MultipartFile file,
                     @RequestParam(value = "name", required = false) String name,
                     Authentication auth) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document)
                    .replaceAll("[ \\t]+", " ")            // collapse runs of spaces/tabs to single space
                    .replaceAll(" *(\\r?\\n) *", "$1")     // strip leading/trailing spaces on each line
                    .replaceAll("\\n([a-z(])", " $1")      // join lines where continuation is obvious (lowercase or open-paren)
                    .replaceAll("\\n{3,}", "\n\n")         // collapse 3+ blank lines to 2
                    .strip();

            CV cv = new CV();
            cv.setName(name != null ? name : file.getOriginalFilename());
            cv.setContentText(text);
            return cvService.save(cv, currentUserId(auth));
        }
    }

    @GetMapping
    public List<CV> listAll(Authentication auth) {
        return cvService.listAll(currentUserId(auth));
    }

    @GetMapping("/names")
    public List<CvNameResponse> listNames(Authentication auth) {
        return cvService.listAll(currentUserId(auth)).stream()
                .map(cv -> new CvNameResponse(cv.getId(), cv.getName(), cv.isDefaultCv()))
                .toList();
    }

    @GetMapping(value = "/{id}/text", produces = "text/plain")
    public String getText(@PathVariable UUID id) {
        return cvService.getById(id).getContentText();
    }

    @PostMapping("/{id}/default")
    public CV setDefault(@PathVariable UUID id, Authentication auth) {
        return cvService.setDefault(id, currentUserId(auth));
    }
}
