package searchengine.services;

import lombok.RequiredArgsConstructor;
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

    @Override
    public ResponseEntity<Object> webPageCrawling(){
        if (!siteRepository.findAllContainingTheStatus("INDEXING").isEmpty()) {
            ResponseFalse responseFalse = new ResponseFalse();
            responseFalse.setError("Индексация уже запущена");
            return ResponseEntity.status(403).body(responseFalse);
        } else {
            ResponseTrue responseTrue = new ResponseTrue();
            ForkJoinPool.commonPool().execute(this::startWebPageCrawling);
            return ResponseEntity.ok(responseTrue);
        }


    }

    private void startWebPageCrawling() {
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
            Link link = new Link(sitesList, indexRepository, lemmaRepository,
                    pageRepository, siteEntity.getUrl(),
                    "", siteRepository, siteEntity, connectionData);

            siteEntity = siteRepository.findByUrl(url);
            try {
                ForkJoinPool.commonPool().invoke(new ListOfLinks(link));
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
        SiteEntity siteEntity = null;
        PageEntity pageEntity = null;
        ResponseTrue responseTrue = new ResponseTrue();
        List<PageEntity> pageEntities = pageRepository.findAllContains(url);
        if (pageEntities.isEmpty()) {
            boolean notFound = true;
            for (Site site : sitesList.getSites()) {
                System.out.println(url + " = " + site.getName().toLowerCase(Locale.ROOT));
                if (url.contains(site.getName().toLowerCase(Locale.ROOT))) {
                    siteEntity = siteRepository.findByUrl(site.getUrl());

                    notFound = false;
                    Connection.Response response = Jsoup.connect(url)
                            .timeout(5000)
                            .ignoreHttpErrors(true)
                            .execute();
                    String statusCode = Integer.toString(response.statusCode());
                    if (statusCode.charAt(0) != '2') {
                        ResponseFalse responseFalse = new ResponseFalse();
                        responseFalse.setError("Код ошибки: " + statusCode);
                        return ResponseEntity.status(Integer.parseInt(statusCode)).body(responseFalse);
                    }
                    pageEntity = new PageEntity();
                    pageEntity.setSiteId(siteEntity);
                    pageEntity.setPath(url);
                    pageEntity.setCode(response.statusCode());
                    pageEntity.setContent(response.body());
                    pageRepository.save(pageEntity);
                    break;
                }
            }
            if (notFound) {
                ResponseFalse responseFalse = new ResponseFalse();
                responseFalse.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
                return ResponseEntity.status(400).body(responseFalse);
            }
        } else {
            pageEntity = pageEntities.get(0);
            deleteIndexPage(pageEntity);
        }
        LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
        Map<String, Integer> lemmas = lemmaFinder.сountAllLemmas(lemmaFinder.deleteHTMLTag(pageEntity.getContent()));
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            List<LemmaEntity> lemmaEntities = lemmaRepository.findAllContains(entry.getKey());
            LemmaEntity lemmaEntity = null;
            if (lemmaEntities.isEmpty()) {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSiteId(pageEntity.getSiteId());
                lemmaEntity.setFrequency(1);
                lemmaEntity.setLemma(entry.getKey());
            } else {
                lemmaEntity = lemmaEntities.get(0);
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            }
            lemmaRepository.save(lemmaEntity);
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setPageId(pageEntity);
            indexEntity.setLemmaId(lemmaEntity);
            indexEntity.setRank((float) entry.getValue());
            indexRepository.save(indexEntity);
        }
        return ResponseEntity.ok(responseTrue);
    }

    private void deleteIndexPage(PageEntity pageEntity) {
        for (IndexEntity indexEntity : indexRepository.findAllByPageId(pageEntity.getId())) {
            indexRepository.delete(indexEntity);
            LemmaEntity lemmaEntity = indexEntity.getLemmaId();
            if (lemmaEntity.getFrequency() == 1) {
                lemmaRepository.delete(lemmaEntity);
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() - 1);
                lemmaRepository.save(lemmaEntity);
            }
        }
    }
}
