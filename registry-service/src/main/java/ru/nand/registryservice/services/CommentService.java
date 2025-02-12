package ru.nand.registryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.Comment;
import ru.nand.registryservice.repositories.CommentRepository;

@Slf4j
@Service
public class CommentService {
    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public void save(Comment comment){
        commentRepository.save(comment);
    }
}
