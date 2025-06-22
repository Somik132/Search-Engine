package searchengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor

@Entity
@Table(name = "site", indexes = {
        @Index(columnList = "url", name = "url_ind")
})
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private int id;

    @Column(name = "status", nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", nullable = false, columnDefinition = "VARCHAR(255)", unique = true)
    private String url;

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PageEntity> pages;

    @JsonIgnore
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LemmaEntity> lemmas;
}
