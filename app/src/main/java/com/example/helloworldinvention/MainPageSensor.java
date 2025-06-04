    package com.example.helloworldinvention;
    
    import static android.content.ContentValues.TAG;
    
    import android.app.NotificationChannel;
    import android.app.NotificationManager;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.graphics.Color;
    import android.os.Build;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.View;
    import android.widget.ImageView;
    import android.widget.LinearLayout;
    import android.widget.TextView;
    import android.widget.Toast;
    
    import androidx.activity.EdgeToEdge;
    import androidx.annotation.NonNull;
    import androidx.annotation.RequiresApi;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.NotificationCompat;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;
    
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.Query;
    import com.google.firebase.database.ValueEventListener;
    import com.google.firebase.firestore.DocumentReference;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;

    import java.util.Map;
    import static com.example.helloworldinvention.R.layout.activity_main;
    
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.text.InputType;
    import android.util.Log;
    import android.view.View;
    import android.widget.EditText;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.Task;
    import com.google.firebase.auth.AuthResult;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.firestore.DocumentReference;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.messaging.FirebaseMessaging;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.Task;
    
    
    public class MainPageSensor extends AppCompatActivity {
    
        LinearLayout location_field, co_layout, beat_layout;
        ImageView ivSettings;
        TextView fill_hr, fill_co, location, impact;
        private FirebaseFirestore db;
        private FirebaseAuth mAuth;
        private DatabaseReference gpsDataRef, gasDataRef, heartDataRef, ImpactDateRef;
        private ValueEventListener gpsValueListener, ImpactValueListener, gasValueListener, heartValueListener;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main_page_sensor);
    
            // View 초기화
            location_field = findViewById(R.id.location_field);
            ivSettings = findViewById(R.id.ivSettings);
            location = findViewById(R.id.location);
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            impact = findViewById(R.id.impact);
            // Firebase Realtime Database 참조
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            gpsDataRef = database.getReference("GPSsensor_data");
            gasDataRef = database.getReference("gassensor_data");
            heartDataRef = database.getReference("hreartsensor_data");
            ImpactDateRef = database.getReference("ImpactSensor_data");
    
            // 설정 아이콘 클릭 시 Settings 화면으로 이동
            ivSettings.setOnClickListener(view -> {
                Intent intent = new Intent(MainPageSensor.this, Settings.class);
                startActivity(intent);
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
            }
    
            // GPS 데이터 가져오기
            Query lastGpsValueQuery = gpsDataRef.orderByKey().limitToLast(1);
            gpsValueListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChildren()) {
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            Object childValue = childSnapshot.getValue();
                            if (childValue instanceof Map) {
                                Map<String, Object> data = (Map<String, Object>) childValue;
                                Object latitudeObj = data.get("latitude");
                                Object longitudeObj = data.get("longitude");
                                FirebaseUser user = mAuth.getCurrentUser();
                                getDocumentByUid(user.getUid(), (String) latitudeObj, (String) longitudeObj);
                            } else {
                                location.setText("GPS format error");
                            }
                            break;
                        }
                    } else {
                        location.setText("No GPS data");
                    }
                }
    
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    location.setText("GPS error: " + error.getMessage());
                }
            };
            lastGpsValueQuery.addValueEventListener(gpsValueListener);

            // 기존 쿼리와 리스너 제거
// Query lastImpactValueQuery = ImpactDateRef.orderByKey().limitToLast(1);

// 'value'라는 키 하나만 바로 접근
            ImpactValueListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Object impactValue = dataSnapshot.getValue();
                        if (impactValue != null && !impactValue.toString().equals("1023")) {
                            String title = "충격 위험 수치";
                            String msg = "큰 충격이 가해졌습니다.";
                            impact.setText("큰 충격이 가해졌습니다.");
                            createNotification(title, msg);
                        } else {
                            impact.setText("가해진 충격이 없습니다.");
                        }
                    } else {
                        impact.setText("No Impact data");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    impact.setText("Impact error: " + error.getMessage());
                }
            };

// 'ImpactSensor_data/value'로 직접 참조
            ImpactDateRef.child("value").addValueEventListener(ImpactValueListener);
    
    
    
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    
        private void createNotification(String title, String message) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String channelId = "default_channel_id";
    
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Default Channel",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Default Channel Description");
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
    
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.home_24px) // 알림 아이콘 설정
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true);
    
            notificationManager.notify(1, builder.build());
        }
    
        // 리스너 해제 (메모리 누수 방지)
        @Override
        protected void onStop() {
            super.onStop();
            if (heartValueListener != null) heartDataRef.removeEventListener(heartValueListener);
            if (gasValueListener != null) gasDataRef.removeEventListener(gasValueListener);
            if (gpsValueListener != null) gpsDataRef.removeEventListener(gpsValueListener);
        }
    
        private void getDocumentByUid(String userId,String latitudeObj, String longitudeObj) {
            DocumentReference docRef = db.collection("users").document(userId);
    
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
    
                        String atitude = document.getString("atitude");
                        String longitude = document.getString("longitude");
    
                        if (!(latitudeObj.equals(atitude)||longitudeObj.equals(longitude))){
                            String title = "안전 지역 이탈";
                            String msg = "이용자가 안전지역을 이탈하였습니다.";
                            createNotification(title, msg);
                            location.setText("이용자가 안전지역을 이탈하였습니다. 이용자의 안전을 확인하세요.");
                        }else {
                            String latStr = latitudeObj != null ? "Lat: " + latitudeObj : "Lat: null";
                            String lonStr = longitudeObj != null ? "Lon: " + longitudeObj : "Lon: null";
    
                            location.setText(latStr + ", " + lonStr);
                        }

                    } else {
                        Log.d(TAG, "No such document for UID: " + userId);
                        Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Firestore document fetch failed", task.getException());
                    Toast.makeText(this, "서버 오류로 사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == 1) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "알림 권한이 허용됨");
                } else {
                    Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }