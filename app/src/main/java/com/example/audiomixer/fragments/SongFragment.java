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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.audiomixer.R;
import com.example.audiomixer.adapters.SongAdapter;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.utils.AppPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class SongFragment extends Fragment implements SongAdapter.OnSongClickListener {

    // Communicate to mainActivity
    public interface OnSongSelectListener {
        void onSongSelected(AudioFile song);
    }

    private Uri musicDirectory;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private SearchView songSearch;
    private Spinner sortSpinner;
    private ImageButton songSortButton;
    private boolean isAscending = true; // Track state of songSortButton
    private String sortCategory = "Added";
    private AudioFile currentSong;

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
        songAdapter = new SongAdapter(songs, this);
        recyclerView.setAdapter(songAdapter);

        songSearch = view.findViewById(R.id.songSearch);

        songSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                songAdapter.filterSongs(newText, sortCategory, isAscending);
                return true;
            }
        });

        sortSpinner = view.findViewById(R.id.songSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                view.getContext(),
                R.array.sort_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                sortCategory = parentView.getItemAtPosition(position).toString();
                songAdapter.sortSongs(sortCategory, isAscending);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        songSortButton = view.findViewById(R.id.songSortDirection);

        songSortButton.setOnClickListener(v -> {
            isAscending = !isAscending;

            if (isAscending) {
                songSortButton.animate().rotationX(0f).setDuration(300).start();
            } else {
                songSortButton.animate().rotationX(180f).setDuration(300).start();
            }

            songAdapter.sortSongs(sortCategory, isAscending);
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

    public void onPlayClick(AudioFile song) {
        if (song.equals(currentSong)) {
            // Song is already playing, toggle pause
            currentSong = null;
            songAdapter.setCurrentSong(null);
        } else {
            // New song clicked, start playing
            currentSong = song;
            songAdapter.setCurrentSong(song);
        }
    }
}