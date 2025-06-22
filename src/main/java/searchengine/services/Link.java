package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ConnectionData;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

@Getter
public class Link {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final String link;
    private final String tab;
    private final SiteRepository siteRepository;
    private final SiteEntity siteEntity;
    private final SitesList sitesList;
    private final IndexationService indexationService;
    private final ConnectionData connectionData;


    public Link(SitesList sitesList, IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, String link,
                String tab, SiteRepository siteRepository, SiteEntity siteEntity, ConnectionData connectionData) {
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.link = link;
        this.tab = tab;
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
        this.connectionData = connectionData;
        this.indexationService = new IndexationServiceImpl(pageRepository, siteRepository,
                lemmaRepository, indexRepository, sitesList, connectionData);
    }
    public HashSet<Link> getChildren() {
        HashSet<Link> links = new HashSet<>();
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
                indexationService.indexPage(pageEntity.getPath());
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
                        Link tempLink = new Link(sitesList, indexRepository, lemmaRepository, pageRepository, link + temp, tab + "    ", siteRepository, siteEntity, connectionData);
                        links.add(tempLink);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return links;
    }
    public String getName() {
        return tab + link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link1 = (Link) o;
        return Objects.equals(link, link1.link) && Objects.equals(tab, link1.tab);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link, tab);
    }
}
