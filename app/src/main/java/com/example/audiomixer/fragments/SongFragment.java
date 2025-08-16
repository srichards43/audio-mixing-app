package com.example.audiomixer.fragments;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.audiomixer.R;
import com.example.audiomixer.adapters.SongAdapter;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.utils.AppPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class SongFragment extends Fragment {

    private Uri musicDirectory;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private SearchView songSearch;

    public SongFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        musicDirectory = AppPreferences.getMusicDirectoryUri(this.requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_song, container, false);

        recyclerView = view.findViewById(R.id.songRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<AudioFile> songs = loadAudioFiles(musicDirectory);
        adapter = new SongAdapter(songs);
        recyclerView.setAdapter(adapter);

        songSearch = view.findViewById(R.id.songSearch);

        songSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filterSongs(newText);
                return true;
            }
        });

        return view;
    }

    /**
     * Loads audio files from the music directory
     * @param directoryUri, the uri of the music directory to search
     * @return a list of audio files
     */
    private List<AudioFile> loadAudioFiles(Uri directoryUri) {
        List<AudioFile> audioFiles = new ArrayList<>();
        if (directoryUri == null) return audioFiles;

        DocumentFile directory = DocumentFile.fromTreeUri(requireContext(), directoryUri);
        if (directory == null) return audioFiles;

        for (DocumentFile file : directory.listFiles()) {
            if (file.isFile() && Objects.requireNonNull(file.getName()).endsWith(".mp3")) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(requireContext(), file.getUri());

                    String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if (!isMetadata(title)) {
                        title = file.getName();
                    }

                    String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    if (!isMetadata(artist)) {
                        artist = "Unknown";
                    }

                    String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    if (!isMetadata(album)) {
                        album = "Unknown Album";
                    }

                    String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long duration = 0;
                    if (isMetadata(durationString)) {
                        try {
                            duration = Long.parseLong(durationString);
                        } catch (NumberFormatException ignored) {}
                    }

                    byte[] albumCover = retriever.getEmbeddedPicture();

                    audioFiles.add(new AudioFile(
                            title,
                            artist,
                            album,
                            duration,
                            file.getUri().toString(),
                            albumCover
                    ));
                } catch (Exception e) {
                    Log.e("SongFragment", "Error loading audio file:", e);
                    audioFiles.add(new AudioFile(
                            file.getName(),
                            "Unknown",
                            "Unknown",
                            0,
                            file.getUri().toString(),
                            null
                    ));
                } finally {
                    try {
                        retriever.release();
                    } catch (IOException e) {
                        Log.e("SongFragment", "Error releasing retriever:", e);
                    }

                }
            }
        }

        return audioFiles;
    }

    /**
     * Helper method for metadata validation
     * @param metadata, the metadata category to check
     * @return true if the metadata is not null or empty
     */
    private boolean isMetadata(String metadata) {
        return metadata != null && !metadata.isEmpty();
    }
}