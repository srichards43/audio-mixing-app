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

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private final List<AudioFile> songs;

    public SongAdapter(List<AudioFile> songs) {
        this.songs = songs;
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
        AudioFile song = songs.get(position);

        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.duration.setText(song.getFormattedDuration());

        if (song.getAlbumCover() != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(song.getAlbumCover(), 0, song.getAlbumCover().length);
            holder.albumCover.setImageBitmap(bmp);
        } else {
            // show default album placeholder
            holder.albumCover.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.playButton.setOnClickListener(v -> {
            // Toggle state of song, change image to represent
            holder.isPlaying = !holder.isPlaying;
            if (holder.isPlaying) {
                holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                holder.playButton.setImageResource(android.R.drawable.ic_media_play);
            }
        });
    }

    @Override
    public int getItemCount() {
        // Find how many objects are already created for reuse
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist, duration;
        ImageView albumCover;
        ImageButton playButton;
        boolean isPlaying;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.songTitle);
            artist = itemView.findViewById(R.id.songArtist);
            duration = itemView.findViewById(R.id.songDuration);
            albumCover = itemView.findViewById(R.id.songAlbumCover);
            playButton = itemView.findViewById(R.id.songPlay);

        }
    }
}
