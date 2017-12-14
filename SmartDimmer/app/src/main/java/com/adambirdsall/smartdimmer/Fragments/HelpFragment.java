package com.adambirdsall.smartdimmer.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.adambirdsall.smartdimmer.R;
import com.adambirdsall.smartdimmer.Utils.EventListener;

/**
 * Created by AdamBirdsall on 10/10/17.
 */

public class HelpFragment extends Fragment {

    private EventListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof EventListener) {
            listener = (EventListener)context;
        } else {
            // Throw an error!
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listener.disconnectFromDevices();

        listener.helpVariables();

        TextView titleTextview = (TextView) getActivity().findViewById(R.id.titleTextView);
        titleTextview.setVisibility(View.VISIBLE);
        titleTextview.setText("Help");

        getActivity().findViewById(R.id.toolbarImage).setVisibility(View.INVISIBLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_help, container, false);
    }

}
