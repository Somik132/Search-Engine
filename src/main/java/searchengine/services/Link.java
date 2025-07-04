package searchengine.services;

import com.mysql.cj.conf.ConnectionUrlParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
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
@Getter
public class Link {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final ConnectionData connectionData;
    private final LemmaFinder lemmaFinder;

    @Autowired
    public Link(IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository, SitesList sitesList, ConnectionData connectionData, LemmaFinder lemmaFinder) {
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.connectionData = connectionData;
        this.lemmaFinder = lemmaFinder;
    }

    public HashSet<Pair<Link, String>> getChildren(SiteEntity siteEntity, String link) {
        HashSet<Pair<Link, String>> links = new HashSet<>();
        if (!IndexationServiceImpl.indexingIsAllowed) {
            return links;
        }
        if (!pageRepository.findAllContains(link).isEmpty()) {
            return links;
        }
        try {
            Connection.Response response = Jsoup.connect(link)
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .execute();
            if (response.statusCode() == 200) {
                PageEntity pageEntity = new PageEntity();
                pageEntity.setSiteId(siteEntity);
                pageEntity.setPath(link);
                pageEntity.setCode(response.statusCode());
                pageEntity.setContent(response.body());
                pageRepository.save(pageEntity);
                indexPage(pageEntity.getPath(), siteEntity, pageEntity);
                Document document = Jsoup.connect(link)
                        .userAgent(connectionData.getUserAgent())
                        .referrer(connectionData.getReferrer())
                        .get();
                Elements elements = document.select("a");
                for (Element e : elements) {
                    String temp = e.attr("href");
                    if (temp.isEmpty() || temp.contains("#") || temp.charAt(0) != '/') continue;

                    if (temp.length() > 1) {
                        if (link.charAt(link.length() - 1) == '/') {
                            temp = temp.substring(1);
                        }
                        Link tempLink = new Link(indexRepository, lemmaRepository, pageRepository,
                                siteRepository, sitesList, connectionData, lemmaFinder);
                        links.add(Pair.of(tempLink, link + temp));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return links;
    }

    public ResponseEntity<Object> indexPage(String url, SiteEntity siteEntity,
                                            PageEntity pageEntity) throws IOException {
        ResponseTrue responseTrue = new ResponseTrue();
        List<PageEntity> pageEntities = pageRepository.findAllContains(url);
        if (pageEntities.isEmpty() && pageEntity == null) {
            boolean notFound = true;
            for (Site site : sitesList.getSites()) {
                if (url.contains(site.getName().toLowerCase(Locale.ROOT))) {
                    siteEntity = siteRepository.findByUrl(site.getUrl());
                    if (siteEntity == null) {
                        siteEntity = createSiteEntity(Status.FAILED, "", site.getUrl(), site.getName());
                        siteRepository.save(siteEntity);
                    }

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
        Map<String, Integer> lemmas = lemmaFinder.сountAllLemmas(lemmaFinder.deleteHTMLTag(pageEntity.getContent()));
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            List<LemmaEntity> lemmaEntities = lemmaRepository.findAllContains(entry.getKey());
            LemmaEntity lemmaEntity;
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
}
