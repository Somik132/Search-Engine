package searchengine.services;

import org.springframework.data.util.Pair;
import searchengine.model.SiteEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class ListOfLinks extends RecursiveTask<String> {
    private final Link link;
    private final String url;
    private final SiteEntity siteEntity;


    ListOfLinks(Link link, String url, SiteEntity siteEntity)
    {
        this.link = link;
        this.url = url;
        this.siteEntity = siteEntity;
    }
    @Override
    protected String compute() {
        List<ListOfLinks> taskList = new ArrayList<>();
        if (url.contains("http")) {
            for (Pair<Link, String> child : link.getChildren(siteEntity, url)) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ListOfLinks task = new ListOfLinks(child.getFirst(), child.getSecond(), siteEntity);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                task.fork();
                taskList.add(task);
            }
            for (ListOfLinks task : taskList) {
                task.join();
            }
        }
        return null;
    }
}
