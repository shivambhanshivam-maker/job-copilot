package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.CvNameResponse;
import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.service.CVService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
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
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        CV cv = new CV();
        cv.setName(name != null ? name : file.getOriginalFilename());

        if (filename.endsWith(".docx")) {
            try (InputStream is = file.getInputStream()) {
                XWPFDocument document = new XWPFDocument(is);
                String markdown = extractMarkdown(document);
                cv.setContentMarkdown(markdown);
                cv.setContentText(stripMarkdown(markdown));
            }
        } else {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document)
                        .replaceAll("[ \\t]+", " ")
                        .replaceAll(" *(\\r?\\n) *", "$1")
                        .replaceAll("\\n([a-z(])", " $1")
                        .replaceAll("\\n{3,}", "\n\n")
                        .strip();
                cv.setContentText(text);
            }
        }

        return cvService.save(cv, currentUserId(auth));
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

    @GetMapping(value = "/{id}/markdown", produces = "text/plain")
    public ResponseEntity<String> getMarkdown(@PathVariable UUID id) {
        String markdown = cvService.getById(id).getContentMarkdown();
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(markdown);
    }

    @PutMapping("/{id}/content")
    public CV updateContent(@PathVariable UUID id,
                            @RequestBody Map<String, String> body,
                            Authentication auth) {
        return cvService.updateContent(id, body.get("contentMarkdown"));
    }

    @PostMapping("/{id}/default")
    public CV setDefault(@PathVariable UUID id, Authentication auth) {
        return cvService.setDefault(id, currentUserId(auth));
    }

    private String extractMarkdown(XWPFDocument document) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph para : document.getParagraphs()) {
            String style = para.getStyle();
            String text = extractRunText(para);
            if (text.isBlank()) {
                sb.append("\n");
                continue;
            }
            if (style != null) {
                if (style.startsWith("Heading1") || style.equals("Title")) {
                    sb.append("# ").append(text);
                } else if (style.startsWith("Heading2")) {
                    sb.append("## ").append(text);
                } else if (style.startsWith("Heading3")) {
                    sb.append("### ").append(text);
                } else if (style.startsWith("ListParagraph") || style.startsWith("ListBullet")) {
                    if (para.getNumID() != null) {
                        // Real list item — has an actual bullet/number
                        String numFmt = getNumberingFormat(para);
                        if ("decimal".equals(numFmt)) {
                            sb.append("1. ").append(text);
                        } else {
                            sb.append("- ").append(text);
                        }
                    } else {
                        // Continuation paragraph — same style but no numbering, treat as plain
                        sb.append(text);
                    }
                } else {
                    sb.append(text);
                }
            } else {
                sb.append(text);
            }
            sb.append("\n");
        }
        return sb.toString().replaceAll("\n{3,}", "\n\n").strip();
    }

    private String extractRunText(XWPFParagraph para) {
        StringBuilder result = new StringBuilder();
        String currentFormat = null;
        StringBuilder currentText = new StringBuilder();

        for (XWPFRun run : para.getRuns()) {
            String text = run.getText(0);
            if (text == null) text = "";
            text = text.replace("\n", " ").replace("\r", ""); // strip soft breaks
            boolean bold = run.isBold();
            boolean italic = run.isItalic();
            String format = (bold && italic) ? "boldItalic" : bold ? "bold" : italic ? "italic" : "plain";

            if (format.equals(currentFormat)) {
                currentText.append(text);
            } else {
                if (currentFormat != null) {
                    result.append(applyFormat(currentText.toString(), currentFormat));
                }
                currentFormat = format;
                currentText = new StringBuilder(text);
            }
        }
        if (currentFormat != null) {
            result.append(applyFormat(currentText.toString(), currentFormat));
        }
        return result.toString();
    }

    private String applyFormat(String text, String format) {
        if ("plain".equals(format) || text.strip().isEmpty()) return text;
        String leading = text.substring(0, text.length() - text.stripLeading().length());
        String trailing = text.substring(text.stripTrailing().length());
        String inner = text.strip();
        String marker = switch (format) {
            case "boldItalic" -> "***";
            case "bold" -> "**";
            case "italic" -> "*";
            default -> "";
        };
        return leading + marker + inner + marker + trailing;
    }

    private String getNumberingFormat(XWPFParagraph para) {
        try {
            return para.getNumIlvl() != null
                    ? para.getDocument().getNumbering()
                    .getAbstractNum(para.getDocument().getNumbering()
                            .getNum(para.getNumID()).getCTNum().getAbstractNumId().getVal())
                    .getCTAbstractNum().getLvlArray(para.getNumIlvl().intValue())
                    .getNumFmt().getVal().toString()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String stripMarkdown(String markdown) {
        if (markdown == null) return null;
        return markdown
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("\\*\\*\\*(.+?)\\*\\*\\*", "$1")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("(?m)^[-*+]\\s+", "")
                .replaceAll("(?m)^\\d+\\.\\s+", "")
                .trim();
    }
}