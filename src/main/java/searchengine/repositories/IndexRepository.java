package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Query(value = "select * from `index` where page_id = :pageId", nativeQuery = true)
    List<IndexEntity> findAllByPageId(Integer pageId);

    @Query(value = "select * from `index` where page_id = :pageId and lemma_id = :lemmaId", nativeQuery = true)
    List<IndexEntity> findAllByPageIdAndLemmaId(Integer pageId, Integer lemmaId);
}
