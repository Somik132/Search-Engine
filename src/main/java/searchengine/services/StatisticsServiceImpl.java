package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        if (siteRepository.findAllContainingTheStatus("INDEXING").isEmpty()) {
            total.setIndexing(false);
        } else {
            total.setIndexing(true);
        }

        List<DetailedStatisticsItemInt> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            boolean error = true;
            Site site = sitesList.get(i);
            SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
            if (siteEntity == null) {
                siteEntity = new SiteEntity();
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteEntity.setName(site.getName());
                siteEntity.setUrl(site.getUrl());
                siteEntity.setLastError("Индексации еще не было");
                siteEntity.setLemmas(new HashSet<>());
                siteEntity.setPages(new HashSet<>());
                siteRepository.save(siteEntity);
            }
            DetailedStatisticsItemInt item = null;
            if (siteEntity.getLastError().isEmpty()) {
                error = false;
                item = new DetailedStatisticsNotError();
            } else {
                item = new DetailedStatisticsItem();
            }
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = siteEntity.getPages().size();
            int lemmas = siteEntity.getLemmas().size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity.getStatus().toString());
            if (error) {
                item.setError(siteEntity.getLastError());
            }
            item.setStatusTime(siteEntity.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
