package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.hibernate.usertype.LoggableUserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Main {
    public static void main(String[] args) throws IOException {
        List<String> words = new ArrayList<>();
        words.add("pro");
        words.set(0, "<b>" + words.get(0) + "</b>");
        System.out.println(words.get(0));
    }
}
