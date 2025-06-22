package searchengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@NoArgsConstructor

@Entity
@Table(name = "page", indexes = {
        @Index(columnList = "path", name = "path_ind"),
        @Index(columnList = "site_id", name = "site_id_ind")
})
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, insertable = false, updatable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteEntity siteId;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @JsonIgnore
    @OneToMany(mappedBy = "pageId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IndexEntity> indexes;
}
