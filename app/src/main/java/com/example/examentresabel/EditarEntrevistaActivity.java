package com.example.examentresabel;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditarEntrevistaActivity extends AppCompatActivity {

    private EditText etDescripcion, etPeriodista, etFecha;
    private ImageView ivImagen;
    private TextView tvAudioSeleccionado;
    private Uri imagenUri, audioUri;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Button btnSeleccionar, btnsubirAudio, btngrabarAudio, btnguardar;
    private String entrevistaId;
    private String fechaOriginal;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_AUDIO_REQUEST = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_AUDIO_RECORD = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_entrevista);

        initializeUI();
        initializeFirebase();

        entrevistaId = getIntent().getStringExtra("EntrevistaId");
        if (isValidEntrevistaId(entrevistaId)) {
            cargarEntrevista(entrevistaId);
        } else {
            showToast("ID de entrevista no válido");
            finish();
        }

        setupListeners();
    }

    private void initializeUI() {
        etDescripcion = findViewById(R.id.etDescripcion);
        etPeriodista = findViewById(R.id.etPeriodista);
        etFecha = findViewById(R.id.etFecha);
        ivImagen = findViewById(R.id.ivImagen);
        btnSeleccionar = findViewById(R.id.btnSeleccionar);
        btnsubirAudio = findViewById(R.id.btnsubirAudio);
        btngrabarAudio = findViewById(R.id.btngrabarAudio);
        btnguardar = findViewById(R.id.btnguardar);
        etFecha.setEnabled(false);
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference("entrevistas");
        storageReference = FirebaseStorage.getInstance().getReference("entrevistas");
    }

    private boolean isValidEntrevistaId(String id) {
        return id != null && !id.isEmpty();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupListeners() {
        btnSeleccionar.setOnClickListener(v -> mostrarDialogoSeleccionImagen());
        btnsubirAudio.setOnClickListener(v -> seleccionarAudio());
        btngrabarAudio.setOnClickListener(v -> checkPermissionAndExecute(Manifest.permission.RECORD_AUDIO, REQUEST_AUDIO_RECORD, this::iniciarGrabacionAudio));
        btnguardar.setOnClickListener(v -> guardarEntrevista());
    }

    private void cargarEntrevista(String id) {
        databaseReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    EntrevistaModel entrevistaModel = snapshot.getValue(EntrevistaModel.class);
                    if (entrevistaModel != null) {
                        populateFields(entrevistaModel);
                    }
                } else {
                    showToast("Entrevista no encontrada");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Error al cargar los datos");
            }
        });
    }

    private void populateFields(EntrevistaModel entrevista) {
        etDescripcion.setText(entrevista.getDescripcion());
        etPeriodista.setText(entrevista.getPeriodista());
        etFecha.setText(entrevista.getFecha());
        fechaOriginal = entrevista.getFecha();

        Glide.with(this)
                .load(entrevista.getImagenUrl())
                .into(ivImagen);

        imagenUri = Uri.parse(entrevista.getImagenUrl());
        audioUri = Uri.parse(entrevista.getAudioUrl());
    }

    private void mostrarDialogoSeleccionImagen() {
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Imagen")
                .setItems(new CharSequence[]{"Desde galería", "Abrir Cámara"}, (dialog, which) -> {
                    if (which == 0) {
                        seleccionarImagen();
                    } else {
                        checkPermissionAndExecute(Manifest.permission.CAMERA, REQUEST_IMAGE_CAPTURE, this::capturarImagen);
                    }
                })
                .show();
    }

    private void checkPermissionAndExecute(String permission, int requestCode, Runnable onPermissionGranted) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted.run();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    private void capturarImagen() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void iniciarGrabacionAudio() {
        startActivityForResult(new Intent(this, RecordEntrevistaActivity.class), REQUEST_AUDIO_RECORD);
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    private void seleccionarAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Audio"), PICK_AUDIO_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_RECORD && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarGrabacionAudio();
        } else {
            showToast("Permiso denegado");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            handleActivityResult(requestCode, data);
        }
    }

    private void handleActivityResult(int requestCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                ivImagen.setImageBitmap(imageBitmap);
                imagenUri = getImageUri(this, imageBitmap);
                break;

            case PICK_IMAGE_REQUEST:
                imagenUri = data.getData();
                loadBitmapToImageView(imagenUri);
                break;

            case PICK_AUDIO_REQUEST:
                audioUri = data.getData();
                break;

            case REQUEST_AUDIO_RECORD:
                String filePath = data.getStringExtra("audioFilePath");
                if (filePath != null) {
                    audioUri = Uri.fromFile(new File(filePath));
                } else {
                    showToast("Error al grabar audio");
                }
                break;
        }
    }

    private void loadBitmapToImageView(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ivImagen.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Uri getImageUri(Context context, Bitmap image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), image, "EntrevistaImagen", null);
        return Uri.parse(path);
    }

    private void guardarEntrevista() {
        String descripcion = etDescripcion.getText().toString().trim();
        String periodista = etPeriodista.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();

        if (isInputValid(descripcion, periodista)) {
            uploadFile(imagenUri, "image.jpg", imageUrl ->
                    uploadFile(audioUri, "recording.mp3", audioUrl ->
                            saveEntrevistaToDatabase(descripcion, periodista, fecha, imageUrl, audioUrl)
                    )
            );
        }
    }

    private boolean isInputValid(String descripcion, String periodista) {
        if (TextUtils.isEmpty(descripcion) || TextUtils.isEmpty(periodista) || imagenUri == null || audioUri == null) {
            showToast("Seleccione una imagen y un audio");
            return false;
        }
        return true;
    }

    private void uploadFile(Uri fileUri, String fileName, OnUploadCompleteListener listener) {
        StorageReference fileRef = storageReference.child(entrevistaId + "/" + fileName);
        fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                        listener.onUploadComplete(uri.toString())
                )
        ).addOnFailureListener(e ->
                showToast("Error al subir " + fileName)
        );
    }

    private void saveEntrevistaToDatabase(String descripcion, String periodista, String fecha, String imagenUrl, String audioUrl) {
        String fechaModificacion = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        EntrevistaModel entrevista = new EntrevistaModel(entrevistaId, descripcion, periodista, fecha, imagenUrl, audioUrl);
        databaseReference.child(entrevistaId).setValue(entrevista).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showToast("Entrevista actualizada");
                finish();
            } else {
                showToast("Error al actualizar");
            }
        });
    }

    interface OnUploadCompleteListener {
        void onUploadComplete(String url);
    }
}
