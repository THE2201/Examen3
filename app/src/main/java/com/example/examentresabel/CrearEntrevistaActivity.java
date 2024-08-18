package com.example.examentresabel;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrearEntrevistaActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_AUDIO_REQUEST = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_AUDIO_RECORD = 4;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private EditText etDescripcion, etPeriodista, etFecha;
    private ImageView ivImagen;
    private TextView tvAudio;
    private Uri imagenUri, audioUri;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Button btnSubirImagen, btnSubirAudio, btnGrabarAudio, btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_entrevista);

        initFirebase();
        initUI();
        setListeners();
    }

    private void initFirebase() {
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        databaseReference = FirebaseDatabase.getInstance().getReference("entrevistas");
        storageReference = FirebaseStorage.getInstance().getReference("entrevistas");
    }

    private void initUI() {
        etDescripcion = findViewById(R.id.descripcion);
        etPeriodista = findViewById(R.id.periodista);
        etFecha = findViewById(R.id.fecha);
        ivImagen = findViewById(R.id.imagen);
        btnSubirImagen = findViewById(R.id.btnsubirImagen);
        btnSubirAudio = findViewById(R.id.btnsubirAudio);
        btnGrabarAudio = findViewById(R.id.btngrabarAudio);
        btnGuardar = findViewById(R.id.btnGuardar);
    }

    private void setListeners() {
        btnSubirImagen.setOnClickListener(v -> showImageSelectionDialog());
        btnSubirAudio.setOnClickListener(v -> selectAudio());
        btnGrabarAudio.setOnClickListener(v -> checkPermissionAndExecute(
                Manifest.permission.RECORD_AUDIO, REQUEST_AUDIO_RECORD, this::startAudioRecording));
        btnGuardar.setOnClickListener(v -> saveInterview());

        etFecha.addTextChangedListener(new DateTextWatcher());
        findViewById(R.id.btnVer).setOnClickListener(v -> startActivity(new Intent(this, ListaActivity.class)));
    }

    private void showImageSelectionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Imagen")
                .setItems(new CharSequence[]{"Seleccionar de la galería", "Tomar foto"},
                        (dialog, which) -> {
                            if (which == 0) {
                                selectImage();
                            } else {
                                checkPermissionAndExecute(Manifest.permission.CAMERA, REQUEST_IMAGE_CAPTURE, this::captureImage);
                            }
                        })
                .show();
    }

    private void checkPermissionAndExecute(String permission, int requestCode, Runnable action) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        } else {
            action.run();
        }
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void startAudioRecording() {
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/entrevista_audio.3gp";
        File audioFile = new File(audioFilePath);

        try {
            if (audioFile.exists()) audioFile.delete();
            audioFile.createNewFile();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            setFechaActual();
            startActivity(new Intent(this, RecordEntrevistaActivity.class).putExtra("audioFilePath", audioFilePath));

        } catch (IOException e) {
            Log.e("AudioRecording", "Error starting audio recording", e);
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    private void selectAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Audio"), PICK_AUDIO_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_AUDIO_RECORD) {
                startAudioRecording();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                captureImage();
            }
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    handleImageCaptureResult(data);
                    break;
                case PICK_IMAGE_REQUEST:
                    handleImagePickResult(data);
                    break;
                case PICK_AUDIO_REQUEST:
                    handleAudioPickResult(data);
                    break;
                case REQUEST_AUDIO_RECORD:
                    handleAudioRecordResult(data);
                    break;
            }
        }
    }

    private void handleImageCaptureResult(Intent data) {
        Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
        ivImagen.setImageBitmap(imageBitmap);
        imagenUri = getImageUri(this, imageBitmap);
    }

    private void handleImagePickResult(Intent data) {
        imagenUri = data.getData();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagenUri);
            ivImagen.setImageBitmap(bitmap);
        } catch (IOException e) {
            Log.e("ImagePick", "Error loading image", e);
        }
    }

    private void handleAudioPickResult(Intent data) {
        audioUri = data.getData();
        tvAudio.setText("Audio seleccionado");
        setFechaActual();
    }

    private void handleAudioRecordResult(Intent data) {
        String filePath = data.getStringExtra("audioFilePath");
        if (filePath != null) {
            audioUri = Uri.fromFile(new File(filePath));
            tvAudio.setText("Audio grabado");
            setFechaActual();
        } else {
            Toast.makeText(this, "Error al grabar audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFechaActual() {
        etFecha.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
    }

    private Uri getImageUri(Context context, Bitmap image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), image, "Imagen", null);
        return Uri.parse(path);
    }

    private void saveInterview() {
        String descripcion = etDescripcion.getText().toString().trim();
        String periodista = etPeriodista.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();

        if (TextUtils.isEmpty(descripcion) || TextUtils.isEmpty(periodista) || TextUtils.isEmpty(fecha)) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadData(descripcion, periodista, fecha);
    }

    private void uploadData(String descripcion, String periodista, String fecha) {
        if (imagenUri != null) {
            uploadFile(imagenUri, "jpg", imagenUrl -> uploadFile(audioUri, "3gp", audioUrl -> saveInterviewToDatabase(descripcion, periodista, fecha, imagenUrl, audioUrl)));
        } else {
            uploadFile(audioUri, "3gp", audioUrl -> saveInterviewToDatabase(descripcion, periodista, fecha, null, audioUrl));
        }
    }

    private void uploadFile(Uri fileUri, String extension, OnSuccessListener<String> onSuccessListener) {
        if (fileUri != null) {
            StorageReference fileRef = storageReference.child(System.currentTimeMillis() + "." + extension);
            fileRef.putFile(fileUri).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> onSuccessListener.onSuccess(uri.toString()));
                } else {
                    Toast.makeText(this, "Error al subir archivo", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            onSuccessListener.onSuccess(null);
        }
    }

    private void saveInterviewToDatabase(String descripcion, String periodista, String fecha, @Nullable String imagenUrl, @Nullable String audioUrl) {
        EntrevistaModel entrevista = new EntrevistaModel(descripcion, periodista, fecha, imagenUrl, audioUrl);
        databaseReference.push().setValue(entrevista)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Entrevista guardada exitosamente", Toast.LENGTH_SHORT).show();
                        clearFields();
                    } else {
                        Toast.makeText(this, "Error al guardar entrevista", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearFields() {
        etDescripcion.setText("");
        etPeriodista.setText("");
        etFecha.setText("");
        ivImagen.setImageResource(R.drawable.persona_img);
        tvAudio.setText("");
        imagenUri = null;
        audioUri = null;
    }

    private static class DateTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No implementation needed
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No implementation needed
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Date validation or formatting can be implemented here if needed
        }
    }
}
