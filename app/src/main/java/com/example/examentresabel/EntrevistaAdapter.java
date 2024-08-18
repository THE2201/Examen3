package com.example.examentresabel;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;

import java.util.List;

public class EntrevistaAdapter extends ArrayAdapter<EntrevistaModel> {
    private Activity context;
    private List<EntrevistaModel> lista;

    public EntrevistaAdapter(Activity context, List<EntrevistaModel> lista) {
        super(context, R.layout.entrevista_item, lista);
        this.context = context;
        this.lista = lista;
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View listViewItem = inflater.inflate(R.layout.entrevista_item, null, true);

        ImageView imageEntrevista = listViewItem.findViewById(R.id.imageEntrevista);
        TextView txtDescripcion = listViewItem.findViewById(R.id.txtDescripcion);
        TextView txtFecha = listViewItem.findViewById(R.id.txtFecha);
        TextView txtPeriodista = listViewItem.findViewById(R.id.txtPeriodista);
        EntrevistaModel entrevistaModel = lista.get(position);
        txtDescripcion.setText(entrevistaModel.getDescripcion());
        txtPeriodista.setText(entrevistaModel.getPeriodista());
        txtFecha.setText(entrevistaModel.getFecha());

        Glide.with(context).load(entrevistaModel.getImagenUrl()).into(imageEntrevista);

        return listViewItem;
    }
}
