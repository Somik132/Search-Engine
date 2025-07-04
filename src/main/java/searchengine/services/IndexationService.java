package searchengine.services;


import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.http.ResponseEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

public interface IndexationService {
    ResponseEntity<Object> webPageCrawling() throws SQLException;
    ResponseEntity<Object> stopIndexing() throws IOException;
    ResponseEntity<Object> indexPage(String url) throws IOException;
}
