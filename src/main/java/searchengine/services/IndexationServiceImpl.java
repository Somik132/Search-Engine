package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionData;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.ResponseFalse;
import searchengine.dto.response.ResponseTrue;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexationServiceImpl implements IndexationService {
    public volatile static boolean indexingIsAllowed = true;
    public volatile static boolean isIndexing = false;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final ConnectionData connectionData;
    private final LemmaFinder lemmaFinder;

    @Override
    public ResponseEntity<Object> webPageCrawling() {
        if (isIndexing) {
            ResponseFalse responseFalse = new ResponseFalse();
            responseFalse.setError("Индексация уже запущена");
            return ResponseEntity.status(403).body(responseFalse);
        } else {
            ResponseTrue responseTrue = new ResponseTrue();
            ForkJoinPool.commonPool().execute(() -> {
                try {
                    startWebPageCrawling();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return ResponseEntity.ok(responseTrue);
        }


    }

    private void startWebPageCrawling() throws IOException {
        isIndexing = true;
        indexingIsAllowed = true;
        for (Site site : sitesList.getSites()) {
            String url = site.getUrl();

            if (Objects.nonNull(siteRepository.findByUrl(url))) {
                siteRepository.delete(siteRepository.findByUrl(url));
            }

            SiteEntity siteEntity = createSiteEntity(Status.INDEXING, "",
                    site.getUrl(), site.getName());
            siteRepository.save(siteEntity);
            Link link = new Link(indexRepository, lemmaRepository, pageRepository,
                    siteRepository, sitesList, connectionData, lemmaFinder);

            siteEntity = siteRepository.findByUrl(url);
            try {
                ForkJoinPool.commonPool().invoke(new ListOfLinks(link, url, siteEntity));
                siteEntity.setStatusTime(LocalDateTime.now());
                siteEntity.setStatus(Status.INDEXED);
            } catch (RuntimeException e) {
                siteEntity.setStatusTime(LocalDateTime.now());
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError("Runtime exception");
                e.printStackTrace();
            }
            siteRepository.save(siteEntity);
        }
        isIndexing = false;
    }

    private SiteEntity createSiteEntity(Status status, String lastError,
                                        String url, String name) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError(lastError);
        siteEntity.setUrl(url);
        siteEntity.setName(name);
        siteEntity.setPages(new HashSet<>());
        siteEntity.setLemmas(new HashSet<>());
        return siteEntity;
    }

    @Override
    public ResponseEntity<Object> stopIndexing() throws IOException {
        if (!isIndexing) {
            ResponseFalse responseFalse = new ResponseFalse();
            responseFalse.setError("Индексация не запущена");
            return ResponseEntity.status(403).body(responseFalse);
        }
        isIndexing = false;
        indexingIsAllowed = false;
        ResponseTrue responseTrue = new ResponseTrue();
        List<SiteEntity> siteEntities = siteRepository.findAll();
        for (SiteEntity siteEntity : siteEntities) {
            if (siteEntity.getStatus() == Status.INDEXING) {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteEntity.setLastError("Индексация завершена досрочно");
                siteRepository.save(siteEntity);
            }
        }
        return ResponseEntity.ok(responseTrue);
    }

    @Override
    public ResponseEntity<Object> indexPage(String url) throws IOException {
        Link link = new Link(indexRepository, lemmaRepository, pageRepository,
                siteRepository, sitesList, connectionData, lemmaFinder);
        return link.indexPage(url, null, null);
    }

}
