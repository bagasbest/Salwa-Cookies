package com.salwa.salwa.homepage.ui.delivery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salwa.salwa.R;
import com.salwa.salwa.databinding.ActivityDetailDeliveryBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DetailDeliveryActivity extends AppCompatActivity {

    private ActivityDetailDeliveryBinding binding;


    public static final String ITEM_DELIVERY = "delivery";
    private String addedAt;
    private String bookedBy;
    private int price;
    private String productDp;
    private String kecamatan;
    private String kelurahan;
    private String address;
    private String phone;
    private String title;
    private int totalProduct;
    private String userUid;
    private String deliveryStatus;
    private String deliveryId;
    private String uid = "";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailDeliveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // dapatkan atribut dari produk yang di klik pengguna
        DeliveryModel dm = getIntent().getParcelableExtra(ITEM_DELIVERY);
        title = dm.getTitle();

        // tambahkan judul dan ikon back
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(title);
        actionBar.setDisplayHomeAsUpEnabled(true);

        addedAt = dm.getAddedAt();
        bookedBy = dm.getBookedBy();
        price = dm.getPrice();
        productDp = dm.getProductDp();
        kecamatan = dm.getKecamatan();
        kelurahan = dm.getKelurahan();
        address = dm.getAddress();
        phone = dm.getPhone();
        totalProduct = dm.getTotalProduct();
        userUid = dm.getUserUid();
        deliveryStatus = dm.getDeliveryStatus();
        deliveryId = dm.getDeliveryId();

        binding.title.setText(title);
        binding.name.setText("Pemesan: " + bookedBy);
        binding.waktu.setText("Waktu pemesanan: " + addedAt);
        binding.totalProduct.setText("Total Pembelian Produk: " + totalProduct);
        binding.kecamatan.setText("Kecamatan: " + kecamatan);
        binding.kelurahan.setText("Kelurahan: " + kelurahan);
        binding.address.setText("Detail Alamat: " + address);
        binding.textView7.setText("No.Telepon: " + phone);
        binding.price.setText("Total Harga: " + price);

        Glide.with(this)
                .load(productDp)
                .placeholder(R.drawable.ic_baseline_broken_image_24)
                .error(R.drawable.ic_baseline_broken_image_24)
                .into(binding.productDp);

        if (deliveryStatus.equals("Sudah Dikirim")) {
            binding.deleteDelivery.setVisibility(View.VISIBLE);
        }

        // konfirmasi penghapusan riwayat delivery
        deleteDelivieryHistory();

    }

    // konfirmasi penghapusan riwayat delivery
    private void deleteDelivieryHistory() {
        binding.deleteDelivery.setOnClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Hapus Riwayat Delivery")
                    .setMessage("Apakah anada yakin ingin menghapus riwayat delivery ini ?")
                    .setPositiveButton("YA", (dialogInterface, i) -> {
                        binding.progressBar.setVisibility(View.VISIBLE);
                        // hapus delivery dari database
                        deleteDeliveryFromDatabase();
                    })
                    .setNegativeButton("TIDAK", null)
                    .setIcon(R.drawable.ic_baseline_delete_24)
                    .show();
        });
    }

    private void deleteDeliveryFromDatabase() {
        FirebaseFirestore
                .getInstance()
                .collection("delivery")
                .document(deliveryId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailDeliveryActivity.this, "Berhasil menghapus riwayat delivery ini", Toast.LENGTH_SHORT).show();
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailDeliveryActivity.this, "Gagal menghapus riwayat delivery ini", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_proof_payment, menu);

        // jika bukan admin maka sembunyikan ikon ceklis untuk verifikasi bahwa produk sedang on delivery
        MenuItem item = menu.findItem(R.id.menu_accept).setVisible(false);
        // sembunyikan delete icon
        MenuItem item2 = menu.findItem(R.id.menu_delete).setVisible(false);


        if (deliveryStatus.equals("Belum Dikirim")) {
            // CEK APAKAH USER YANG SEDANG LOGIN ADMIN ATAU BUKAN, JIKA YA, MAKA TAMPILKAN IKON VERIFIKASI BUKTI PEMBAYARAN
            FirebaseFirestore
                    .getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (("" + document.get("role")).equals("admin")) {
                                item.setVisible(true);
                            }
                        }
                    });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_accept) {
            // tampilkan konfirmasi untuk mengubah deliveryStatus menjadi sudah dikirim
            showConfirmDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Status Delivery")
                .setMessage("Apakah anda yakin bahwa produk sudah anda kirim menuju alamat kustomer ?")
                .setPositiveButton("YA", (dialogInterface, i) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    // perbarui status delivery menjadi Sudah Dikirim
                    updateDeliveryStatus();
                })
                .setNegativeButton("TIDAK", null)
                .setIcon(R.drawable.ic_baseline_check_circle_24)
                .show();
    }

    private void updateDeliveryStatus() {

        // ambil tanggal hari ini dengan format: dd - MMM - yyyy, HH:mm:ss
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat getDate = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
        String format = getDate.format(new Date());

        Map<String, Object> delivery = new HashMap<>();
        delivery.put("deliveryStatus", "Sudah Dikirim");
        delivery.put("addedAt", format);

        binding.waktu.setText(format);

        Log.e("TAG", deliveryId);

        FirebaseFirestore
                .getInstance()
                .collection("delivery")
                .document(deliveryId)
                .update(delivery)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailDeliveryActivity.this, "Berhasil memperbarui status delivery menjadi Sudah Dikirim", Toast.LENGTH_SHORT).show();
                        binding.deleteDelivery.setVisibility(View.VISIBLE);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailDeliveryActivity.this, "Gagal memperbarui delivery", Toast.LENGTH_SHORT).show();
                    }
                });

        // INPUT DATA YANG SUDAH TERDELIVERY KE EXCEL
        inputDataToExcel();

    }

    private void inputDataToExcel() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbyVv8YOr44I6SWgSmxwapMWbaD0v_NysDY-We5jbm2SSxPaPqK4awk5lgDE3uYzDlTg/exec",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(DetailDeliveryActivity.this, response, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }

        ) {
            @Override
            protected Map<String, String> getParams()  {
                Map<String, String> params = new HashMap<>();

                params.put("action", "addItem");
                params.put("addedAt", addedAt);
                params.put("productName", title);
                params.put("totalProduct", String.valueOf(totalProduct));
                params.put("bookedBy", bookedBy);
                params.put("price", String.valueOf(price));

                return params;
            }
        };

        int socketTimeOut = 50000;
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}