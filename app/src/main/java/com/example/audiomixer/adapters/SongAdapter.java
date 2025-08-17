package com.example.audiomixer.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        void onPlayClick(AudioFile song);
    }

    private final List<AudioFile> songs; // All available songs
    private final List<AudioFile> filteredSongs; // Songs that match search query (default: all)
    private final OnSongClickListener listener;
    private AudioFile currentSong; // No position

    public SongAdapter(List<AudioFile> songs, OnSongClickListener listener) {
        this.songs = new ArrayList<>(songs);
        this.filteredSongs = new ArrayList<>(songs);
        this.listener = listener;
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

        holder.title.setText(song.getTitle());
        holder.songInfo.setText(song.getArtist());
        if (!Objects.equals(song.getAlbum(), "Unknown Album")) {
            holder.songInfo.append(" • " + song.getAlbum());
        }
        holder.duration.setText(song.getFormattedDuration());

        if (song.getAlbumCover() != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(song.getAlbumCover(), 0, song.getAlbumCover().length);
            holder.albumCover.setImageBitmap(bmp);
        } else {
            // show default album placeholder
            holder.albumCover.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        if (song.equals(currentSong)) {
            holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            holder.playButton.setImageResource(android.R.drawable.ic_media_play);
        }

        holder.playButton.setOnClickListener(v -> {
                listener.onPlayClick(song);
        });
    }

    @Override
    public int getItemCount() {
        // Find how many objects are already created for reuse
        return filteredSongs.size();
    }

    public void setCurrentSong(AudioFile song) {
        AudioFile oldSong = currentSong;
        currentSong = song;

        // Refresh old item
        if (oldSong != null) {
            int oldIndex = filteredSongs.indexOf(oldSong);
            if (oldIndex != -1) notifyItemChanged(oldIndex);
        }

        // Refresh new item
        int newIndex = filteredSongs.indexOf(song);
        if (newIndex != -1) notifyItemChanged(newIndex);
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
        boolean isPlaying;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.songTitle);
            songInfo = itemView.findViewById(R.id.songInfo);
            duration = itemView.findViewById(R.id.songDuration);
            albumCover = itemView.findViewById(R.id.songAlbumCover);
            playButton = itemView.findViewById(R.id.songPlay);

        }
    }
}
