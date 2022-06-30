package com.example.notepadapp;

import androidx.cardview.widget.CardView;

import com.example.notepadapp.Models.Notes;

public interface NotesClickListener {
    void onClick(Notes notes);
    void onLongClick(Notes notes, CardView cardView);
}
