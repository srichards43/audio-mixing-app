package com.example.audiomixer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audiomixer.R;
import com.example.audiomixer.adapters.SongAdapter;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.services.MusicPlaybackService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import java.util.List;

public class QueueFragment extends BottomSheetDialogFragment {

    private MusicPlaybackService playbackService;
    private SongAdapter songAdapter;
    private List<AudioFile> playlist;

    // Vars used to track where user is dragging a song to be used when released.
    private int draggingFrom = -1;
    private int draggingTo = -1;
    public QueueFragment(MusicPlaybackService service) {
        this.playbackService = service;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (playbackService != null) {
            playbackService.getCurrentSongInternal().observe(getViewLifecycleOwner(), song -> {
                if (song != null) {
                    refreshQueue();
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_queue, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.queueRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        playlist = playbackService.getUpNext();
        songAdapter = new SongAdapter(playlist, this :: onQueueSongClick, requireContext());
        recyclerView.setAdapter(songAdapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            // Track dragged song position and update UI
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();

                if (from == RecyclerView.NO_POSITION && to == RecyclerView.NO_POSITION) {
                    return false;
                }

                if (draggingFrom == -1) {
                    draggingFrom = from;
                }
                draggingTo = to;
                java.util.Collections.swap(playlist, from, to);

                songAdapter.notifyItemMoved(from, to);
                return true;
            }

            // Drop song after dragging, update playlist order
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // Check bounds
                if (draggingFrom != -1 && draggingTo != -1 && draggingFrom != draggingTo) {
                    int offset = playbackService.getCurrentIndex();
                    int draggedFrom = draggingFrom + offset;
                    int draggedTo = draggingTo + offset;
                    int max = playbackService.getPlaylist().size();

                    if (draggedFrom >= 0 && draggedFrom < max && draggedTo >= 0 && draggedTo < max) {
                        playbackService.moveItemInQueue(draggedFrom, draggedTo);
                        // If new song dragged to top start playing
                        if (draggingTo == 0) {
                            playbackService.play(draggedTo);
                        }
                    };
                }

                // Reset tracking
                draggingFrom = -1;
                draggingTo = -1;
            }
            // todo: delete from queue
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };

        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);

        AudioFile currentSong = playbackService.getCurrentSong();
        songAdapter.setCurrentlyPlaying(currentSong.getFilePath());

        return view;
    }

    public void notifySongChanged() {
        refreshQueue();
    }

    private void refreshQueue() {
        if (playbackService != null) {
            // Re-fetch the filtered "Up Next" list starting from the current song
            List<AudioFile> freshUpNext = playbackService.getUpNext();
            playlist.clear();
            playlist.addAll(freshUpNext);

            // Update highlight
            AudioFile currentSong = playbackService.getCurrentSong();
            if (currentSong != null) {
                songAdapter.setCurrentlyPlaying(currentSong.getFilePath());
            }

            songAdapter.notifyDataSetChanged();
        }
    }

    private void onQueueSongClick(int position) {

        if (playbackService == null) {
            return;
        }

        int index = playbackService.getCurrentIndex() + position;
        playbackService.play(index);
    }
}
