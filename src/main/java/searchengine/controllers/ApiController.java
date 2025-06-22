package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexationService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexationService indexationService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexationService indexationService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexationService = indexationService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() throws SQLException {
        return indexationService.webPageCrawling();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() throws IOException {
        return indexationService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam String url) throws IOException {
        return indexationService.indexPage(url);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam String query, @RequestParam(defaultValue = "all") String site,
                                         @RequestParam(defaultValue = "0") String offset,
                                         @RequestParam(defaultValue = "20") String limit) throws IOException {
        return searchService.search(query, site,
                Integer.valueOf(offset), Integer.valueOf(limit));
    }
}
