package com.lithespeed.hellojava06.service;

import com.lithespeed.hellojava06.entity.Dialog;
import com.lithespeed.hellojava06.entity.DialogResponseDTO;
import com.lithespeed.hellojava06.repository.DialogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DialogService {

    private final DialogRepository dialogRepository;

    @Autowired
    public DialogService(DialogRepository dialogRepository) {
        this.dialogRepository = dialogRepository;
    }

    public List<Dialog> getAllDialogs() {
        return dialogRepository.findAll();
    }

    public DialogResponseDTO getDialogByIdAndRequest(int id, String request) throws Exception {
        Optional<Dialog> optionalDialog = dialogRepository.findByIdAndRequest(id, request);
        if (optionalDialog.isPresent()) {
            Dialog dialog = optionalDialog.get();
            return new DialogResponseDTO(dialog.getId(), dialog.getResponse());
        } else {
            throw new Exception("Dialog not found");
        }
    }
}