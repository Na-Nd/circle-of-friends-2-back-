package ru.nand.registryservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "owner_username")
    private String owner;

    @Column(name = "post_text")
    private String postText;

    @Column(name = "date_of_publication")
    private LocalDateTime dateOfPublication;

    @Column(name = "date_of_update")
    private LocalDateTime dateOfUpdate;

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    private Set<String> tags;
}