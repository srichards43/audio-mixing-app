package com.example.audiomixer.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audiomixer.R;
import com.example.audiomixer.objects.AudioFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    // Interface to communicate to corresponding fragment, which then communicates to activity
    public interface OnSongClickListener {
        void onPlayClick(int position);
    }

    private final List<AudioFile> songs; // All available songs
    private final List<AudioFile> filteredSongs; // Songs that match search query (default: all)
    private final OnSongClickListener listener;
    private final int colorPrimary;
    private final int colorDefault;
    private String currentlyPlayingPath = null;

    public SongAdapter(List<AudioFile> songs, OnSongClickListener listener, Context context) {
        this.songs = new ArrayList<>(songs);
        this.filteredSongs = new ArrayList<>(songs);
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
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create new row view if none to recycle
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        // Display data at position
        AudioFile song = filteredSongs.get(position);
        String songPath = song.getFilePath();

        holder.title.setText(song.getTitle());
        holder.songInfo.setText(song.getArtist());

        // Append album if exists
        if (!Objects.equals(song.getAlbum(), "")) {
            holder.songInfo.append(" • " + song.getAlbum());
        }

        holder.duration.setText(song.getFormattedDuration());

        if (song.getAlbumCover() != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(song.getAlbumCover(), 0, song.getAlbumCover().length);
            holder.albumCover.setImageBitmap(bmp);
        } else {
            // Show default album placeholder
            holder.albumCover.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.songConstraint.setOnClickListener(v -> {
            listener.onPlayClick(position);
            setCurrentlyPlaying(songPath);
        });

        // Set title color based on song currently playing
        if (Objects.equals(songPath, currentlyPlayingPath)) {
            holder.title.setTextColor(colorPrimary);
        } else {
            holder.title.setTextColor(colorDefault);
        }
    }

    @Override
    public int getItemCount() {
        // Find how many objects are already created for reuse
        return filteredSongs.size();
    }

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

    private int findPositionByPath(String path) {
        for (int i = 0; i < filteredSongs.size(); i++) {
            if (filteredSongs.get(i).getFilePath().equals(path)) {
                return i;
            }
        }
        return -1;
    }

    public List<AudioFile> getFilteredSongs() {
        return filteredSongs;
    }

    /**
     * Filter songs based on a search query, then sort them
     * @param query, what the user has searched
     * @param category, category selected in spinner, used in sortSongs() call
     * @param isAscending, state of sort button, used in eventual sortSongDirection() call
     */
    public void filterSongs(String query, String category, boolean isAscending) {
        filteredSongs.clear();
        if (query == null || query.isEmpty()) {
            filteredSongs.addAll(songs);
        } else {
            query = query.toLowerCase();
            for (AudioFile song : songs) {
                if (song.getTitle().toLowerCase().contains(query) ||
                        song.getArtist().toLowerCase().contains(query) ||
                        song.getAlbum().toLowerCase().contains(query)) {
                    filteredSongs.add(song);
                }
            }
        }
        sortSongs(category, isAscending);
    }

    /**
     * Sort songs based on a category selected in the spinner, separated from filterSongs to avoid
     * unnecessary overhead for spinner select/direction button click
     * @param category, category to sort by
     * @param isAscending, state of sort button, used in sortSongDirection() call
     */
    public void sortSongs(String category, boolean isAscending) {
        switch (category) {
            case "Added" :
                filteredSongs.sort((o1, o2) -> Long.compare(o1.getCreatedAt(), o2.getCreatedAt()));
            case "Title" :
                filteredSongs.sort((o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
                break;
            case "Artist" :
                filteredSongs.sort((o1, o2) -> o1.getArtist().compareToIgnoreCase(o2.getArtist()));
                break;
            case "Album" :
                filteredSongs.sort((o1, o2) -> o1.getAlbum().compareToIgnoreCase(o2.getAlbum()));
                break;
            case "Duration" :
                filteredSongs.sort((o1, o2) -> Long.compare(o1.getDuration(), o2.getDuration()));
                break;
            default:
                break;
        }

        if (!isAscending) {
            Collections.reverse(filteredSongs);
        }

        notifyDataSetChanged();
    }


    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, songInfo, duration;
        ImageView albumCover;
        ImageButton playButton;

        ConstraintLayout songConstraint;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.songTitle);
            songInfo = itemView.findViewById(R.id.songInfo);
            duration = itemView.findViewById(R.id.songDuration);
            albumCover = itemView.findViewById(R.id.songAlbumCover);
            songConstraint = itemView.findViewById(R.id.songConstraint);
        }
    }
}
