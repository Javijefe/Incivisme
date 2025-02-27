package com.example.incivisme.ui.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.incivisme.Incidencia;
import com.example.incivisme.R;
import com.example.incivisme.databinding.FragmentNotificationsBinding;
import com.example.incivisme.ui.home.SharedViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private SharedViewModel sharedViewModel;
    private FirebaseAuth auth;
    private DatabaseReference incidencies;
    String mCurrentPhotoPath;
    private Uri photoURI;
    private ImageView foto;
    static final int REQUEST_TAKE_PHOTO = 1;
    private StorageReference storageRef;
    private String downloadUrl;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        binding.map.setTileSource(TileSourceFactory.MAPNIK);
        binding.map.setMultiTouchControls(true);
        IMapController mapController = binding.map.getController();
        mapController.setZoom(14.5);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        sharedViewModel.getCurrentLatLng().observe(getViewLifecycleOwner(), location -> {
            if (location != null) {
                GeoPoint geoPoint = new GeoPoint(location.latitude, location.longitude);
                mapController.setCenter(geoPoint);
            }
        });
        ImageView Foto = root.findViewById(R.id.foto);
        Button buttonFoto = root.findViewById(R.id.button_foto);

        buttonFoto.setOnClickListener(button -> {
            dispatchTakePictureIntent();
            buttonFoto.setOnClickListener(view -> {
                Incidencia incidencia = new Incidencia();
                incidencia.setDireccio(txtDireccio.getText().toString());
                incidencia.setLatitud(txtLatitud.getText().toString());
                incidencia.setLongitud(txtLongitud.getText().toString());
                incidencia.setProblema(txtDescripcio.getText().toString());
                incidencia.setUrl(downloadUrl); // Guardem la url de la imatge pujada.
                // ...
            });
        });


        MyLocationNewOverlay myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), binding.map);
        myLocationNewOverlay.enableMyLocation();
        binding.map.getOverlays().add(myLocationNewOverlay);

        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), binding.map);
        compassOverlay.enableCompass();
        binding.map.getOverlays().add(compassOverlay);

        auth = FirebaseAuth.getInstance();
        DatabaseReference base = FirebaseDatabase.getInstance().getReference();
        DatabaseReference users = base.child("users");
        DatabaseReference uid = users.child(auth.getUid());
        incidencies = uid.child("incidencies");

        incidencies.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Incidencia incidencia = dataSnapshot.getValue(Incidencia.class);

                if (incidencia != null) {
                    GeoPoint location = new GeoPoint(
                            Double.parseDouble(incidencia.getLatitud()),
                            Double.parseDouble(incidencia.getLongitud())
                    );

                    Marker marker = new Marker(binding.map);
                    marker.setPosition(location);
                    marker.setTitle(incidencia.getProblema());
                    marker.setSnippet(incidencia.getDireccio());

                    binding.map.getOverlays().add(marker);
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        return binding.getRoot();
    }
    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(
                getContext().getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {


            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getContext(),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                Glide.with(this).load(photoURI).into(foto);

                FirebaseStorage storage = FirebaseStorage.getInstance();
                storageRef = storage.getReference();

                StorageReference imageRef = storageRef.child(mCurrentPhotoPath);
                UploadTask uploadTask = imageRef.putFile(photoURI);


                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnCompleteListener(task -> {
                        Uri downloadUri = task.getResult();
                        Glide.with(this).load(downloadUri).into(foto);

                        downloadUrl = downloadUri.toString();
                    });

                });
                
                
            } else {
                Toast.makeText(getContext(),
                        "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }

    }



    @Override
    public void onResume() {
        super.onResume();
        binding.map.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        binding.map.onPause();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


