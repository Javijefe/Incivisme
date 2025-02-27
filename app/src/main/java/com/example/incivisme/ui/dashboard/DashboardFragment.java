package com.example.incivisme.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.incivisme.Incidencia;
import com.example.incivisme.databinding.FragmentDashboardBinding;
import com.example.incivisme.databinding.RvincidenciesitemBinding;
import com.example.incivisme.ui.home.SharedViewModel;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseUser authUser;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        SharedViewModel sharedViewModel = new ViewModelProvider(
                requireActivity()
        ).get(SharedViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        sharedViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            authUser = user;

            if (user != null) {
                DatabaseReference base = FirebaseDatabase.getInstance().getReference();

                DatabaseReference users = base.child("users");
                DatabaseReference uid = users.child(user.getUid());
                DatabaseReference incidencies = uid.child("incidencies");

                FirebaseRecyclerOptions<Incidencia> options = new FirebaseRecyclerOptions.Builder<Incidencia>()
                        .setQuery(incidencies, Incidencia.class)
                        .setLifecycleOwner(this)
                        .build();

                IncidenciaAdapter adapter = new IncidenciaAdapter(options);

                binding.rvIncidencies.setAdapter(adapter);
                binding.rvIncidencies.setLayoutManager(
                new LinearLayoutManager(requireContext())
                );

            }
        });

        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    class IncidenciaAdapter extends FirebaseRecyclerAdapter<Incidencia, IncidenciaAdapter.IncidenciaViewholder> {
        public IncidenciaAdapter(@NonNull FirebaseRecyclerOptions<Incidencia> options) {
            super(options);
        }

        @Override
        protected void onBindViewHolder(
        @NonNull IncidenciaViewholder holder, int position, @NonNull Incidencia model
            ) {
            holder.binding.txtDescripcio.setText(model.getProblema());
            holder.binding.txtAdreca.setText(model.getDireccio());
        }

        @NonNull
        @Override
        public IncidenciaViewholder onCreateViewHolder(
        @NonNull ViewGroup parent, int viewType
            ) {
            return new IncidenciaViewholder(RvincidenciesitemBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent, false));
        }

        class IncidenciaViewholder extends RecyclerView.ViewHolder {
            RvincidenciesitemBinding binding;

            public IncidenciaViewholder(RvincidenciesitemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}