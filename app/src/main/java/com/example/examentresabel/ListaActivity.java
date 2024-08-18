package com.example.examentresabel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ListaActivity extends AppCompatActivity {

    private DatabaseReference db;
    private ListView listViewEntrevistas;
    private List<EntrevistaModel> listaEntrevistas;
    private EntrevistaAdapter entrevistaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lista);

        adjustForSystemBars();
        initializeFirebase();
        initializeUIComponents();
        loadEntrevistas();

        listViewEntrevistas.setOnItemClickListener((adapterView, view, position, id) -> {
            EntrevistaModel selected = listaEntrevistas.get(position);
            showOptionsDialog(selected);
        });
    }

    private void adjustForSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeFirebase() {
        db = FirebaseDatabase.getInstance().getReference("entrevistas");
    }

    private void initializeUIComponents() {
        listViewEntrevistas = findViewById(R.id.listViewEntrevistas);
        listaEntrevistas = new ArrayList<>();
        entrevistaAdapter = new EntrevistaAdapter(this, listaEntrevistas);
        listViewEntrevistas.setAdapter(entrevistaAdapter);
    }

    private void loadEntrevistas() {
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaEntrevistas.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    EntrevistaModel entrevista = postSnapshot.getValue(EntrevistaModel.class);
                    if (entrevista != null) {
                        listaEntrevistas.add(entrevista);
                    }
                }
                entrevistaAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ListaActivity.this, "Lista vacia", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteEntrevista(EntrevistaModel entrevista) {
        DatabaseReference dbRef = db.child(String.valueOf(entrevista.getIdOrden()));
        dbRef.removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(ListaActivity.this, "Entrevista eliminada", Toast.LENGTH_SHORT).show();
            listaEntrevistas.remove(entrevista);
            entrevistaAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Toast.makeText(ListaActivity.this, "No se pudo eliminar la entrevista", Toast.LENGTH_SHORT).show();
        });
    }

    private void showOptionsDialog(EntrevistaModel entrevista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Acción para: " + entrevista.getDescripcion());

        String[] opciones = {"Editar", "Eliminar", "Reproducir"};
        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0:
                    editEntrevista(entrevista);
                    break;
                case 1:
                    deleteEntrevista(entrevista);
                    break;
                case 2:
                    playEntrevista(entrevista);
                    break;
            }
        });
        builder.show();
    }

    private void editEntrevista(EntrevistaModel entrevista) {
        Intent modificarIntent = new Intent(ListaActivity.this, EditarEntrevistaActivity.class);
        modificarIntent.putExtra("Id", entrevista.getIdOrden());
        startActivity(modificarIntent);
    }

    private void playEntrevista(EntrevistaModel entrevista) {
        Intent playIntent = new Intent(ListaActivity.this, PlayEntrevistaActivity.class);
        playIntent.putExtra("Entrevista", entrevista);
        startActivity(playIntent);
    }
}
