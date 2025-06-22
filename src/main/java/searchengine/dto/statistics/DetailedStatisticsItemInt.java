package searchengine.dto.statistics;

public interface DetailedStatisticsItemInt {
    void setName(String name);

    void setUrl(String url);

    void setPages(int pages);

    void setLemmas(int lemmas);

    void setStatus(String status);

    void setError(String error);

    void setStatusTime(long l);
}
