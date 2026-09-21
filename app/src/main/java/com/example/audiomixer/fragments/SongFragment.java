package com.example.audiomixer.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.example.audiomixer.activities.MainActivity;
import com.example.audiomixer.adapters.SongAdapter;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.utils.MediaScanner;

import java.util.ArrayList;
import java.util.List;

public class SongFragment extends Fragment implements SongAdapter.OnSongClickListener {

    public interface OnSongSelectListener {
        void onSongSelected(List<AudioFile> playlist, int position);
    }

    private SongAdapter songAdapter;
    private ImageButton songSortButton;
    private boolean isAscending = true;
    private String sortCategory = "Added";
    private OnSongSelectListener onSongSelectListener;
    private Context savedContext;
    private TextView splashText;

    public SongFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        savedContext = context;
        if (context instanceof OnSongSelectListener) {
            onSongSelectListener = (OnSongSelectListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnSongSelectListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.songRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        songAdapter = new SongAdapter(new ArrayList<>(), this, savedContext);
        recyclerView.setAdapter(songAdapter);
        splashText = view.findViewById(R.id.splashText);

        loadSongs();

        // Search listener
        SearchView songSearch = view.findViewById(R.id.songSearch);
        songSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                songAdapter.filterSongs(newText, sortCategory, isAscending);
                return true;
            }
        });

        // Sorting spinner
        Spinner sortSpinner = view.findViewById(R.id.songSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                view.getContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                sortCategory = parentView.getItemAtPosition(position).toString();
                songAdapter.sortSongs(sortCategory, isAscending);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Sort direction button
        songSortButton = view.findViewById(R.id.songSortDirection);
        songSortButton.setOnClickListener(v -> {
            isAscending = !isAscending;
            songSortButton.animate().rotationX(isAscending ? 0f : 180f).setDuration(300).start();
            songAdapter.sortSongs(sortCategory, isAscending);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe service and refresh if needed (e.g., after permission granted)
        ((MainActivity)requireActivity()).serviceLiveData.observe(getViewLifecycleOwner(), service -> {
            if (songAdapter.getItemCount() == 0) {
                loadSongs();
            }
            service.getCurrentSongInternal().observe(getViewLifecycleOwner(), song -> {
                if (songAdapter != null) {
                    songAdapter.setCurrentlyPlaying(song == null ? null : song.getFilePath());
                }
            });
        });
    }

    private void loadSongs() {
        new Thread(() -> {
            List<AudioFile> songs = MediaScanner.scanDeviceForAudio(requireContext());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    songAdapter.setSongs(songs);
                    songAdapter.filterSongs("", sortCategory, isAscending);

                    if (songs.isEmpty()) {
                        splashText.setText(R.string.nothing_found_error);
                        splashText.setVisibility(View.VISIBLE);
                    } else {
                        splashText.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onPlayClick(int position) {
        List<AudioFile> playlist = songAdapter.getFilteredSongs();
        if (onSongSelectListener != null) {
            onSongSelectListener.onSongSelected(playlist, position);
        }
    }
}
