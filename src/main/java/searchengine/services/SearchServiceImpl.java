package searchengine.services;

import jakarta.persistence.Index;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.search.ResponsePage;
import searchengine.dto.search.SearchResponseFalse;
import searchengine.dto.search.SearchResponseTrue;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public ResponseEntity<Object> search(String query, String site, int offset, int limit) throws IOException {
        LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
        Map<String, Integer> mapLemmas = lemmaFinder.сountAllLemmas(query);
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        SearchResponseFalse responseFalse = new SearchResponseFalse();

        if (query.isEmpty()) {
            responseFalse.setError("Задан пустой поисковый запрос");
            return ResponseEntity.status(400).body(responseFalse);
        }

        int cntPages = 0;
        List<PageEntity> pageEntities = new ArrayList<>();
        if (site.equals("all")) {
            for (SiteEntity siteEntity : siteRepository.findAll()) {
                if (!siteEntity.getStatus().equals(Status.INDEXED)) {
                    responseFalse.setError("Есть не проиндексированные сайты");
                    return ResponseEntity.status(403).body(responseFalse);
                }
                cntPages += siteEntity.getPages().size();
                pageEntities.addAll(siteEntity.getPages());
            }
        } else {
            SiteEntity siteEntity = siteRepository.findByUrl(site);
            if (!siteEntity.getStatus().equals(Status.INDEXED)) {
                responseFalse.setError("Сайт, по которому ищем, не проиндексирован");
                return ResponseEntity.status(403).body(responseFalse);
            }
            cntPages = siteEntity.getPages().size();
            pageEntities.addAll(siteEntity.getPages());
        }
        return startSearch(mapLemmas, cntPages, lemmaEntities, pageEntities, offset, limit);
    }

    private ResponseEntity<Object> startSearch(Map<String, Integer> mapLemmas, int cntPages, List<LemmaEntity> lemmaEntities,
                                               List<PageEntity> pageEntities, int offset, int limit) throws IOException {
        for (Map.Entry<String, Integer> lemma : mapLemmas.entrySet()) {
            List<LemmaEntity> tempLemmaEntities = lemmaRepository.findAllContains(lemma.getKey());
            for (LemmaEntity tempLemmaEntity : tempLemmaEntities) {
                if ((float) tempLemmaEntity.getFrequency() / cntPages <= 0.85) {
                    lemmaEntities.add(tempLemmaEntity);
                }
            }
            if (tempLemmaEntities.isEmpty()) {
                lemmaEntities = new ArrayList<>();
                break;
            }
        }
        Collections.sort(lemmaEntities);
        for (LemmaEntity lemmaEntity : lemmaEntities) {
            pageEntities = pageEntities.stream()
                    .filter(page -> !indexRepository.findAllByPageIdAndLemmaId(page.getId(),
                            lemmaEntity.getId()).isEmpty())
                    .collect(Collectors.toList());
        }
        SearchResponseTrue responseTrue = new SearchResponseTrue();
        if (pageEntities.isEmpty() || lemmaEntities.isEmpty()) {
            responseTrue.setCount(0);
            return ResponseEntity.ok(responseTrue);
        }
        float maxRank = 0;
        List<ResponsePage> responsePages = new ArrayList<>();
        for (PageEntity pageEntity : pageEntities) {
            ResponsePage responsePage = new ResponsePage();
            responsePage.setSiteName(pageEntity.getSiteId().getName());
            responsePage.setSite(pageEntity.getSiteId().getUrl());
            String content = pageEntity.getContent();
            responsePage.setTitle(content.substring(
                    content.indexOf("<title>") + 7,
                    content.indexOf("</title>")));
            responsePage.setUri(pageEntity.getPath()
                    .substring(pageEntity.getSiteId().getUrl().length()));
            responsePage.setSnippet(getSnippet(pageEntity, lemmaEntities).toString());
            responsePage.setRelevance(0);
            for (LemmaEntity lemmaEntity : lemmaEntities) {
                responsePage.setRelevance(responsePage.getRelevance() + indexRepository
                        .findAllByPageIdAndLemmaId(pageEntity.getId(), lemmaEntity.getId()
                        ).get(0).getRank());
            }
            maxRank = Float.max(maxRank, responsePage.getRelevance());
            responsePages.add(responsePage);
        }
        for (ResponsePage responsePage :  responsePages) {
            responsePage.setRelevance(responsePage.getRelevance() / maxRank);
        }
        Collections.sort(responsePages);
        responseTrue.setCount(responsePages.size());
        List<ResponsePage> answerResponsePages = new ArrayList<>();
        int countPages = 0;
        for (ResponsePage responsePage : responsePages) {
            if (offset > 0) {
                --offset;
                continue;
            }
            if (countPages == limit) break;
            answerResponsePages.add(responsePage);
            ++countPages;
        }
        responseTrue.setData(answerResponsePages);
        return ResponseEntity.ok(responseTrue);
    }

    private StringBuilder getSnippet(PageEntity pageEntity, List<LemmaEntity> lemmaEntities) throws IOException {
        int quantityWordsInSnippet = 35;
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        List<String> wordsF = new ArrayList<>(Arrays.stream(pageEntity.getContent()
                .replaceAll("([^а-яА-Я0-9\\s\\p{Punct}])", "")
                .trim()
                .split("\\s+")).toList());
        List<String> lemmaWords = new ArrayList<>();
        List<String> words = new ArrayList<>();
        for (String word : wordsF) {
            if (!word.replaceAll("([^а-яА-Я])", "").isEmpty()
                    && word.replaceAll("([^\\p{Punct}])", "").length() <= 3) {
                words.add(word);
            }
        }
        for (String word : words) {
            word = word.toLowerCase(Locale.ROOT).replaceAll("([^а-яА-Я])", "");
            if (word.isEmpty()) {
                lemmaWords.add("");
                continue;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            lemmaWords.add(normalForms.get(0));
        }
        List<Integer> hasTheWordFound = new ArrayList<>();
        for (int i = 0; i < lemmaWords.size(); ++i) {
            int hasFound = 0;
            for (LemmaEntity lemmaEntity : lemmaEntities) {
                if (lemmaWords.get(i).equals(lemmaEntity.getLemma())) {
                    hasFound = 1;
                    String word = words.get(i);
                    String normWord = word.replaceAll("([^а-яА-Я])", "");
                    StringBuilder newWord = new StringBuilder();
                    String lemma = lemmaWords.get(i);
                    int startIndex = word.toLowerCase(Locale.ROOT).indexOf(lemma.charAt(0));
                    if (startIndex == -1 || lemma.length() > word.length()) continue;
                    int endIndex = startIndex + normWord.length() - 1;
                    newWord.append(word, 0, startIndex).append("<b>")
                            .append(word.substring(startIndex, endIndex + 1)).append("</b>")
                            .append(word.substring(endIndex + 1));
                    words.set(i, newWord.toString());
                    break;
                }
            }
            hasTheWordFound.add(hasFound);
        }
        int beginIndex = 0;
        int endIndex = 0;
        int theMaxAmountOnTheSegment = 0;
        while (endIndex < hasTheWordFound.size() && endIndex < quantityWordsInSnippet) {
            theMaxAmountOnTheSegment += hasTheWordFound.get(endIndex);
            ++endIndex;
        }
        int tempAmount = theMaxAmountOnTheSegment;
        for (int i = endIndex; i < hasTheWordFound.size(); ++i) {
            tempAmount = tempAmount + hasTheWordFound.get(i)
                    - hasTheWordFound.get(i - quantityWordsInSnippet);
            if (theMaxAmountOnTheSegment < tempAmount) {
                theMaxAmountOnTheSegment = tempAmount;
                beginIndex = i - quantityWordsInSnippet + 1;
            }
        }
        endIndex = Integer.min(hasTheWordFound.size() - 1,
                beginIndex + quantityWordsInSnippet - 1);
        StringBuilder answer = new StringBuilder();
        for (int i = beginIndex; i <= endIndex; ++i) {
            answer.append(words.get(i));
            if (i != endIndex) answer.append(" ");
        }
        return answer;
    }
}
