package com.lithespeed.hellojava06.repository;

import com.lithespeed.hellojava06.entity.Dialog;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DialogRepository {

    private List<Dialog> dialogs;

    public DialogRepository() {
        this.dialogs = new ArrayList<>();
        loadDialogs();
    }

    private void loadDialogs() {
        dialogs.add(new Dialog(1, "Hello", "Hello"));
        dialogs.add(new Dialog(2, "How are you?", "I'm doing well, thank you for asking!"));
        dialogs.add(new Dialog(3, "What's your name?", "I'm an AI assistant here to help you."));
        dialogs.add(new Dialog(4, "What time is it?",
                "I don't have access to real-time information, but you can check your device's clock."));
        dialogs.add(new Dialog(5, "How can I help you?",
                "You can ask me questions and I'll do my best to provide helpful answers."));
        dialogs.add(new Dialog(6, "Goodbye", "Goodbye! Have a great day!"));
        dialogs.add(new Dialog(7, "Thank you", "You're welcome! I'm glad I could help."));
    }

    public List<Dialog> findAll() {
        return new ArrayList<>(dialogs);
    }

    public Optional<Dialog> findByIdAndRequest(int id, String request) {
        return dialogs.stream()
                .filter(dialog -> dialog.getId() == id &&
                        (request == null || dialog.getRequest().toLowerCase().contains(request.toLowerCase())))
                .findFirst();
    }
}