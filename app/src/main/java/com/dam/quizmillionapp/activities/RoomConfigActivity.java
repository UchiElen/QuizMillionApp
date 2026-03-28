package com.dam.quizmillionapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.dam.quizmillionapp.BaseActivity;
import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.auth.UserSession;
import com.dam.quizmillionapp.interfaces.CreateRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadCategoriesCallback;
import com.dam.quizmillionapp.models.RoomCreationData;
import com.dam.quizmillionapp.repositories.RoomRepository;
import com.dam.quizmillionapp.repositories.UserRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoomConfigActivity extends BaseActivity {

    private EditText edtRoomName;
    private Button btnCreateRoom;

    private SwitchMaterial switchPublicRoom;
    private ImageView imgPrivacyIcon;
    private TextView txtPrivacyState;
    private TextView txtPrivacyHelp;

    private Slider sliderMaxPlayers;
    private TextView txtMaxPlayersValue;

    private ChipGroup chipGroupCategories;

    private ArrayList<String> selectedCategories;
    private RoomRepository roomRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_config);

        roomRepository = new RoomRepository();
        selectedCategories = new ArrayList<>();

        bindViews();
        setupMaxPlayersSlider();
        setupPrivacySwitch();
        setupActions();
        loadAvailableCategories();
    }

    private void bindViews() {
        edtRoomName = findViewById(R.id.edtRoomName);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);

        switchPublicRoom = findViewById(R.id.switchPublicRoom);
        imgPrivacyIcon = findViewById(R.id.imgPrivacyIcon);
        txtPrivacyState = findViewById(R.id.txtPrivacyState);
        txtPrivacyHelp = findViewById(R.id.txtPrivacyHelp);

        sliderMaxPlayers = findViewById(R.id.sliderMaxPlayers);
        txtMaxPlayersValue = findViewById(R.id.txtMaxPlayersValue);

        chipGroupCategories = findViewById(R.id.chipGroupCategories);
    }

    private void setupPrivacySwitch() {
        updatePrivacyUi(switchPublicRoom.isChecked());

        switchPublicRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updatePrivacyUi(isChecked);
        });
    }

    private void updatePrivacyUi(boolean isPublic) {
        if (isPublic) {
            imgPrivacyIcon.setImageResource(R.drawable.ic_unlock);
            imgPrivacyIcon.setColorFilter(ContextCompat.getColor(this, R.color.green));

            txtPrivacyState.setText("Visible en la lista de salas");
            txtPrivacyHelp.setText("Otros jugadores podrán encontrar esta sala desde el lobby");
        } else {
            imgPrivacyIcon.setImageResource(R.drawable.ic_lock);
            imgPrivacyIcon.setColorFilter(ContextCompat.getColor(this, R.color.red));

            txtPrivacyState.setText("Solo accesible con código");
            txtPrivacyHelp.setText("La sala no aparecerá en la lista y solo se podrá entrar mediante código");
        }
    }

    private void setupMaxPlayersSlider() {
        int initialValue = (int) sliderMaxPlayers.getValue();
        txtMaxPlayersValue.setText("Jugadores máximos: " + initialValue);

        sliderMaxPlayers.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                int selectedValue = (int) value;
                txtMaxPlayersValue.setText("Jugadores máximos: " + selectedValue);
            }
        });
    }

    private void setupActions() {
        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryCreateRoom();
            }
        });
    }

    private void tryCreateRoom() {

        String roomName = edtRoomName.getText().toString().trim();
        boolean isPublic = switchPublicRoom.isChecked();
        int maxPlayers = (int) sliderMaxPlayers.getValue();

        selectedCategories = getSelectedCategories();

        if (roomName.isEmpty()) {
            showToast("Introduce un nombre para la sala");
            return;
        }

        if (maxPlayers < 2 || maxPlayers > 30) {
            showToast("El número de jugadores debe estar entre 2 y 30");
            return;
        }

        if (selectedCategories.isEmpty()) {
            showToast("Selecciona al menos una categoría");
            return;
        }

        String uid = UserSession.getCurrentUid(this);
        if (uid == null || uid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario");
            return;
        }

        loadUserNameAndCreateRoom(uid, roomName, maxPlayers, isPublic);
    }

    private void loadUserNameAndCreateRoom(String uid, String roomName, int maxPlayers, boolean isPublic) {
        UserRepository userRepository = new UserRepository();

        userRepository.getUserNameByUid(uid, new UserRepository.OnUserNameLoadedCallback() {
            @Override
            public void onSuccess(String userName) {

                RoomCreationData data = new RoomCreationData(
                        roomName,
                        selectedCategories,
                        maxPlayers,
                        isPublic
                );

                createRoomInRepository(data, uid, userName);
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private void createRoomInRepository(RoomCreationData data, String uid, String userName) {
        roomRepository.createNewRoom(data, uid, userName, new CreateRoomCallback() {
            @Override
            public void onSuccess(String roomId) {
                openWaitingRoom(roomId);
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private void openWaitingRoom(String roomId) {
        Intent intent = new Intent(RoomConfigActivity.this, WaitingActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putStringArrayListExtra("selectedCategories", selectedCategories);
        startActivity(intent);
        finish();
    }

    private void loadAvailableCategories() {
        roomRepository.getAvailableCategories(new LoadCategoriesCallback() {
            @Override
            public void onCategoriesLoaded(List<String> categories) {
                drawCategoryChips(categories);
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    @SuppressLint("ResourceType")
    private void drawCategoryChips(List<String> categories) {
        chipGroupCategories.removeAllViews();

        // Las categorías se cargan de forma dinámica para que la pantalla
        // refleje siempre lo que haya disponible en la base de datos.
        for (String category : categories) {
            Chip chip = new Chip(this);

            chip.setText(formatCategoryName(category));
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setCheckedIconVisible(false);

            chip.setTextColor(ContextCompat.getColorStateList(this, R.drawable.chip_category_text_selector));
            chip.setChipBackgroundColorResource(R.drawable.chip_category_bg_selector);
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColorResource(R.drawable.chip_category_border_selector);

            chipGroupCategories.addView(chip);
        }
    }

    private ArrayList<String> getSelectedCategories() {
        ArrayList<String> categories = new ArrayList<>();

        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            View child = chipGroupCategories.getChildAt(i);

            if (child instanceof Chip) {
                Chip chip = (Chip) child;

                // Guardamos solo las categorías marcadas para enviarlas
                // luego a la sala y a la partida.
                if (chip.isChecked()) {
                    categories.add(chip.getText().toString().trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        return categories;
    }

    private String formatCategoryName(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "";
        }

        String cleanCategory = category.trim().toLowerCase(Locale.ROOT);
        return cleanCategory.substring(0, 1).toUpperCase(Locale.ROOT) + cleanCategory.substring(1);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}