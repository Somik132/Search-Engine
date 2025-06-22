package searchengine.dto.statistics;

import lombok.Data;

@Data
public class DetailedStatisticsNotError implements DetailedStatisticsItemInt {
    private String url;
    private String name;
    private String status;
    private long statusTime;
    private int pages;
    private int lemmas;

    @Override
    public void setError(String error) {
    }
}
