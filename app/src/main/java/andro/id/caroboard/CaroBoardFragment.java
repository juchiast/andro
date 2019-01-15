package andro.id.caroboard;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

public class CaroBoardFragment extends Fragment {
    MainActivity main;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.caro_board_layout, container, false);
        main.boardReady(view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        main = (MainActivity) context;
    }
}
