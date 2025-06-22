package searchengine.services;

import searchengine.services.Link;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class ListOfLinks extends RecursiveTask<String> {
    private final Link link;


    ListOfLinks(Link link) {
        this.link = link;
    }
    @Override
    protected String compute() {
        String ans = link.getName();
        List<ListOfLinks> taskList = new ArrayList<>();
        if (link.getLink().contains("http")) {
            for (Link child : link.getChildren()) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ListOfLinks task = new ListOfLinks(child);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                task.fork();
                taskList.add(task);
            }

            for (ListOfLinks task : taskList) {
                ans += "\n";
                ans = ans + task.join();
            }
        }

        return ans;
    }
}
