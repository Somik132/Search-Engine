package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query(value = "Select * from site where status = :status", nativeQuery = true)
    List<SiteEntity> findAllContainingTheStatus(String status);

    SiteEntity findByUrl(String siteUrl);
}
