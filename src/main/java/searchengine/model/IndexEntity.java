package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(name = "`index`", indexes = {
        @Index(columnList = "page_id", name = "page_id_ind"),
        @Index(columnList = "lemma_id", name = "lemma_id_ind")
})
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`id`", nullable = false, insertable = false, updatable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "page_id")
    private PageEntity pageId;

    @ManyToOne
    @JoinColumn(name = "lemma_id")
    private LemmaEntity lemmaId;

    @Column(name = "`rank`", nullable = false)
    private float rank;
}
