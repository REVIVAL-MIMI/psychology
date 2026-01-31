package com.psychology.service;

import com.psychology.controller.ProfileController;
import com.psychology.dto.PsychologistDTO;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.repository.PsychologistRepository;
import com.psychology.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final PsychologistRepository psychologistRepository;
    private final ClientRepository clientRepository;


    public Client getClientProfile(Client client) {
        return clientRepository.findById(client.getId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }
    public PsychologistDTO getPsychologistProfile(Psychologist psychologist) {
        Psychologist psych = psychologistRepository.findById(psychologist.getId())
                .orElseThrow(() -> new RuntimeException("Psychologist not found"));

        return convertToDTO(psych);
    }

    private PsychologistDTO convertToDTO(Psychologist psychologist) {
        PsychologistDTO dto = new PsychologistDTO();
        dto.setId(psychologist.getId());
        dto.setPhone(psychologist.getPhone());
        dto.setFullName(psychologist.getFullName());
        dto.setEmail(psychologist.getEmail());
        dto.setEducation(psychologist.getEducation());
        dto.setSpecialization(psychologist.getSpecialization());
        dto.setDescription(psychologist.getDescription());
        dto.setPhotoUrl(psychologist.getPhotoUrl());
        dto.setVerified(psychologist.isVerified());
        dto.setVerifiedAt(psychologist.getVerifiedAt());
        dto.setCreatedAt(psychologist.getCreatedAt());

        return dto;
    }

    @Transactional
    public Psychologist updatePsychologistProfile(Psychologist psychologist,
                                                  ProfileController.PsychologistProfileUpdateRequest request) {
        Psychologist existingPsychologist = psychologistRepository.findById(psychologist.getId())
                .orElseThrow(() -> new RuntimeException("Psychologist not found"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            existingPsychologist.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            existingPsychologist.setEmail(request.getEmail());
        }
        if (request.getEducation() != null) {
            existingPsychologist.setEducation(request.getEducation());
        }
        if (request.getSpecialization() != null) {
            existingPsychologist.setSpecialization(request.getSpecialization());
        }
        if (request.getDescription() != null) {
            existingPsychologist.setDescription(request.getDescription());
        }
        if (request.getPhotoUrl() != null) {
            existingPsychologist.setPhotoUrl(request.getPhotoUrl());
        }

        return psychologistRepository.save(existingPsychologist);
    }

    @Transactional
    public Client updateClientProfile(Client client,
                                      ProfileController.ClientProfileUpdateRequest request) {
        Client existingClient = clientRepository.findById(client.getId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            existingClient.setFullName(request.getFullName());
        }
        if (request.getAge() != null) {
            if (request.getAge() < 0 || request.getAge() > 120) {
                throw new RuntimeException("Invalid age");
            }
            existingClient.setAge(request.getAge());
        }
        if (request.getPhotoUrl() != null) {
            existingClient.setPhotoUrl(request.getPhotoUrl());
        }

        return clientRepository.save(existingClient);
    }
}