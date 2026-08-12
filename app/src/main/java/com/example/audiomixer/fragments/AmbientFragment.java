package com.example.audiomixer.fragments;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.audiomixer.R;
import com.example.audiomixer.activities.MainActivity;
import com.example.audiomixer.adapters.AmbientAdapter;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.utils.AppPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class AmbientFragment extends Fragment implements AmbientAdapter.OnAmbientClickListener {

    public interface OnAmbientSelectListener {
        void onAmbientSelected(AudioFile ambient);
    }

    private AmbientAdapter ambientAdapter;
    private OnAmbientSelectListener onAmbientSelectListener;
    private TextView splashText;
    private Uri ambientDirectory;

    public AmbientFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        // Connect interface to MainActivity
        super.onAttach(context);
        if (context instanceof OnAmbientSelectListener) {
            onAmbientSelectListener = (OnAmbientSelectListener) context;
        } else {
            throw new RuntimeException(context + " must implement onAmbientSelectListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ambientDirectory = AppPreferences.getAmbientDirectoryUri(this.requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ambient, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.ambientRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        ambientAdapter = new AmbientAdapter(new ArrayList<>(), this, requireContext()); //init with empty arraylist
        recyclerView.setAdapter(ambientAdapter);

        splashText = view.findViewById(R.id.splashText);
        // Load ambients from music directory in separate thread.
        new Thread(() -> {
            List<AudioFile> ambients = loadAudioFiles(ambientDirectory);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ambientAdapter.setAmbients(ambients);
                    if (ambients.isEmpty()) {
                        splashText.setVisibility(View.VISIBLE);
                        splashText.setText(R.string.nothing_found_error);
                    } else {
                        splashText.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
        recyclerView.setAdapter(ambientAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe once service is live
        ((MainActivity)requireActivity()).serviceLiveData.observe(getViewLifecycleOwner(), service -> {
            service.getCurrentAmbientInternal().observe(getViewLifecycleOwner(), ambience -> {
                if (ambience != null && ambientAdapter != null) {
                    ambientAdapter.setCurrentlyPlaying(ambience.getFilePath());
                }
            });
        });
    }


    /**
     * Loads audio files from the ambient directory
     * @return a list of audio files
     */
    private List<AudioFile> loadAudioFiles(Uri directoryUri) {
        List<AudioFile> audioFiles = new ArrayList<>();
        AssetManager assetManager = requireContext().getAssets();

        try {
            // List all files in ambient assets folder
            String[] files = assetManager.list("ambient");
            if (files == null) return audioFiles;

            for (String fileName : files) {
                if (fileName.endsWith(".mp3") || fileName.endsWith(".m4a") || fileName.endsWith(".ogg")) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    String assetPath = "ambient/" + fileName;

                    try {
                        // Open the asset and get metadata
                        AssetFileDescriptor afd = assetManager.openFd(assetPath);
                        retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();

                        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        if (title == null || title.isEmpty()) title = fileName;

                        byte[] cover = retriever.getEmbeddedPicture();

                        // Create AudioFile with asset prefix
                        audioFiles.add(new AudioFile(
                                title, "Internal", "", 0, "asset:///" + assetPath, cover, System.currentTimeMillis()
                        ));
                    } catch (Exception e) {
                        // Create placeholder metadata so file still playable
                        audioFiles.add(new AudioFile(fileName, "Internal", "", 0, "asset:///" + assetPath, null, System.currentTimeMillis()));
                    } finally {
                        retriever.release();
                    }
                }
            }
        } catch (IOException e) {
            Log.e("AmbientFragment", "Error loading assets", e);
        }

        // If ambient path preference set, add those files too
        if (directoryUri == null) return audioFiles;

        DocumentFile directory = DocumentFile.fromTreeUri(requireContext(), directoryUri);
        if (directory == null) return audioFiles;

        for (DocumentFile file : directory.listFiles()) {
            String type = file.getType();

            // Accept all audio file types
            if (file.isFile() && type !=null && type.startsWith("audio/")) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(requireContext(), file.getUri());

                    String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if (!isMetadata(title)) {
                        title = file.getName(); // default
                    }

                    byte[] coverArt = retriever.getEmbeddedPicture();

                    // Create audio file
                    audioFiles.add(new AudioFile(
                            title,
                            "",
                            "",
                            0,
                            file.getUri().toString(),
                            coverArt,
                            file.lastModified()
                    ));
                } catch (Exception e) {
                    Log.e("AmbientFragment", "Error loading audio file:", e);

                    // On exception, create placeholder audioFile
                    audioFiles.add(new AudioFile(
                            file.getName(),
                            "",
                            "",
                            0,
                            file.getUri().toString(),
                            null,
                            file.lastModified()
                    ));
                } finally {
                    try {
                        retriever.release();
                    } catch (IOException e) {
                        Log.e("AmbientFragment", "Error releasing retriever:", e);
                    }

                }
            }
        }


        return audioFiles;
    }

    private boolean isMetadata(String metadata) {
        return metadata != null && !metadata.isEmpty();
    }

    /**
     * Update the currently playing song in the adapter, called from MainActivity
     * @param songPath path of the song playing
     */
    public void updateCurrentSong(String songPath) {
        ambientAdapter.setCurrentlyPlaying(songPath);
    }

    /**
     * Get playlist from adapter and send to mainActivity with position
     * @param ambient audio file
     */
    public void onAmbientClick(AudioFile ambient) {
        if (onAmbientSelectListener != null) {
            onAmbientSelectListener.onAmbientSelected(ambient);
        }
    }
}