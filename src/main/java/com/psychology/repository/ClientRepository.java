package com.psychology.repository;

import com.psychology.model.entity.Client;
import com.psychology.model.entity.Psychologist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByPsychologist(Psychologist psychologist);
    List<Client> findByPsychologistId(Long psychologistId);
}