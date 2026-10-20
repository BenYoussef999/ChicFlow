package com.chicflow.tunisia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chicflow.tunisia.R;

/** Temporary screen for tabs that are built in the next parts of the frontend. */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_TEXT = "text";

    public static PlaceholderFragment newInstance(String title, String text) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        Bundle args = getArguments();
        if (args != null) {
            ((TextView) view.findViewById(R.id.tvPlaceholderTitle)).setText(args.getString(ARG_TITLE));
            ((TextView) view.findViewById(R.id.tvPlaceholderText)).setText(args.getString(ARG_TEXT));
        }
        return view;
    }
}
