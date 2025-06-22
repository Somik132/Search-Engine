package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor

@Entity
@Table(name = "lemma")
public class LemmaEntity implements Comparable<LemmaEntity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, insertable = false, updatable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteEntity siteId;

    @Column(name = "lemma", nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @Override
    public int compareTo(LemmaEntity other) {
        return this.frequency - other.frequency;
    }
}
