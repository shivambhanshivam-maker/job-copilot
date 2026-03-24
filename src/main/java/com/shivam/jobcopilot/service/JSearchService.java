package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.entity.ExperienceLevel;
import com.shivam.jobcopilot.entity.JobListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class JSearchService {

    private static final Logger log = LoggerFactory.getLogger(JSearchService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jsearch.api.key}")
    private String apiKey;

    public JSearchService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://jsearch.p.rapidapi.com")
                .build();
    }

    /**
     * Fetches jobs for a single (roleCategory, location) combination.
     * Returns up to 10 results (one page).
     */
    public List<JobListing> search(String roleCategory, String cityName, String countryCode, ExperienceLevel experienceLevel) {
        String query = roleCategory + " jobs in " + cityName;

        log.info("Fetching jobs — query: '{}', country: {}, experience: {}", query, countryCode, experienceLevel);

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("query", query)
                            .queryParam("page", "1")
                            .queryParam("num_pages", "1")
                            .queryParam("country", countryCode)
                            .queryParam("date_posted", "today")
                            .queryParam("job_requirements", experienceLevel.toApiValue())
                            .build())
                    .header("x-rapidapi-key", apiKey)
                    .header("x-rapidapi-host", "jsearch.p.rapidapi.com")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(response, roleCategory);

        } catch (Exception e) {
            log.error("JSearch call failed — query: '{}', country: {}", query, countryCode, e);
            return List.of();
        }
    }

    private List<String> parseStringList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private List<JobListing> parseResponse(String response, String roleCategory) {
        List<JobListing> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (!data.isArray()) {
                log.warn("JSearch response has no data array: {}", response);
                return results;
            }

            int limit = Math.min(5, data.size());
            for (int i = 0; i < limit; i++) {
                JsonNode node = data.get(i);
                JobListing listing = new JobListing();
                listing.setExternalId(node.path("job_id").asText(null));
                listing.setEmployerName(node.path("employer_name").asText(null));
                listing.setJobEmploymentType(node.path("job_employment_type").asText(null));
                listing.setJobTitle(node.path("job_title").asText(null));
                listing.setJobApplyLink(node.path("job_apply_link").asText(null));
                listing.setJobPostedAt(node.path("job_posted_at_datetime_utc").asText(null));
                listing.setJobLocation(node.path("job_location").asText(null));
                listing.setJobCountry(node.path("job_country").asText(null));
                listing.setRoleCategory(roleCategory);

                listing.setJobDescription(node.path("job_description").asText(null));

                JsonNode highlights = node.path("job_highlights");
                listing.setQualifications(parseStringList(highlights.path("Qualifications")));
                listing.setResponsibilities(parseStringList(highlights.path("Responsibilities")));

                results.add(listing);
            } // end for
        } catch (Exception e) {
            log.error("Failed to parse JSearch response", e);
        }
        return results;
    }
}