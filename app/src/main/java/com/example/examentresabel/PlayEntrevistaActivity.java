package com.example.examentresabel;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;

public class PlayEntrevistaActivity extends AppCompatActivity {
    private ImageView ivEntrevista;
    private Button btnReproducir;
    private MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play_entrevista);

        ivEntrevista = findViewById(R.id.ivEntrevista);
        btnReproducir = findViewById(R.id.btnReproducir);

        EntrevistaModel entrevista = (EntrevistaModel) getIntent().getSerializableExtra("Entrevista");

        if (entrevista != null) {
            cargarImagen(entrevista.getImagenUrl());
            conf_reproducir(entrevista.getAudioUrl());
        }

        btnReproducir.setOnClickListener(v -> {
            reproducirAudio();
        });
    }

    private void cargarImagen(String imagenUrl) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(imagenUrl);

        Glide.with(this)
                .load(storageReference)
                .into(ivEntrevista);
    }

    private void conf_reproducir(String audioUrl) {
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(mp -> btnReproducir.setEnabled(true));
            mediaPlayer.setOnCompletionListener(mp -> {
                btnReproducir.setText("Reproducir Audio");
                mediaPlayer.seekTo(0);
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar el audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void reproducirAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            btnReproducir.setText("Pausar Audio");
        } else if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnReproducir.setText("Reproducir Audio");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}