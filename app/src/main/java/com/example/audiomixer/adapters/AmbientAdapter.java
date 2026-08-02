package com.example.audiomixer.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audiomixer.R;
import com.example.audiomixer.objects.AudioFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AmbientAdapter extends RecyclerView.Adapter<AmbientAdapter.AmbientViewHolder> {

    public interface OnAmbientClickListener {
        void onAmbientClick(AudioFile ambient);
    }

    private final List<AudioFile> ambientFiles;
    private final OnAmbientClickListener listener;
    private final int colorPrimary;
    private final int colorDefault;
    private String currentlyPlayingPath;

    public AmbientAdapter(List<AudioFile> ambientFiles, AmbientAdapter.OnAmbientClickListener listener, Context context) {
        this.ambientFiles = new ArrayList<>(ambientFiles);
        this.listener = listener;

        // Get colorPrimary from theme
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;

        // Get colorDefault from theme
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        colorDefault = typedValue.data;
    }

    @NonNull
    @Override
    public AmbientAdapter.AmbientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create new row view if none to recycle
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ambient_item, parent, false);
        return new AmbientAdapter.AmbientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AmbientAdapter.AmbientViewHolder holder, int position) {
        // Display data at position
        AudioFile ambient = ambientFiles.get(position);
        String ambientPath = ambient.getFilePath();

        holder.title.setText(ambient.getTitle());

        if (ambient.getAlbumCover() != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(ambient.getAlbumCover(), 0, ambient.getAlbumCover().length);
            holder.cover.setImageBitmap(bmp);
        } else {
            // Show default album placeholder
            holder.cover.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.itemView.setOnClickListener(v -> {
            listener.onAmbientClick(ambient);
            setCurrentlyPlaying(ambientPath);
        });

        // Set title color based on ambient currently playing
        if (Objects.equals(ambientPath, currentlyPlayingPath)) {
            holder.title.setTextColor(colorPrimary);
        } else {
            holder.title.setTextColor(colorDefault);
        }
    }

    @Override
    public int getItemCount() {
        return ambientFiles.size();
    }

    /**
     * Updates the highlighted item in the list
     * @param path of the audioFile
     */
    public void setCurrentlyPlaying(String path) {
        String previousPath = currentlyPlayingPath;
        currentlyPlayingPath = path;
        if (previousPath != null) {
            int previousPos = findPositionByPath(previousPath);
            if (previousPos != -1) {
                notifyItemChanged(previousPos);
            }
        }
        int newPos = findPositionByPath(path);
        if (newPos != -1) {
            notifyItemChanged(newPos);
        }
    }

    /**
     * Update recyclerview with new playlist of ambients
     * @param ambients list of songs
     */
    public void setAmbients(List<AudioFile> ambients) {
        this.ambientFiles.clear();
        this.ambientFiles.addAll(ambients);

        notifyDataSetChanged();
    }

    /**
     * Helper method that converts a path to a position
     * @param path of the audioFile
     * @return position of the audioFile in the filtered list
     */
    private int findPositionByPath(String path) {
        for (int i = 0; i < ambientFiles.size(); i++) {
            if (ambientFiles.get(i).getFilePath().equals(path)) {
                return i;
            }
        }
        return -1;
    }

    static class AmbientViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageView cover;

        public AmbientViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.ambientTitle);
            cover = itemView.findViewById(R.id.ambientCover);
        }
    }
}
