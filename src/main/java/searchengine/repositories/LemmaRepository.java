package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Query(value = "SELECT * from lemma where lemma = :lemma", nativeQuery = true)
    List<LemmaEntity> findAllContains(String lemma);
}
