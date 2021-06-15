package com.salwa.salwa.homepage.ui.product;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salwa.salwa.R;
import com.salwa.salwa.databinding.ActivityDetailProductBinding;
import com.salwa.salwa.homepage.HomeActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DetailProductActivity extends AppCompatActivity {

    private ActivityDetailProductBinding binding;

    public static final String ITEM_PRODUCT = "item";
    private String id;
    private String title;
    private String description;
    private int price;
    private String dp;


    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // tambahkan judul dan ikon back
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Detail Produk");
        actionBar.setDisplayHomeAsUpEnabled(true);

        // dapatkan atribut dari produk yang di klik pengguna
        ProductModel pm = getIntent().getParcelableExtra(ITEM_PRODUCT);
        id = pm.getProductId();
        title = pm.getTitle();
        description = pm.getDescription();
        price = pm.getPrice();
        dp = pm.getImage();
        double likes = pm.getLikes();
        int personRated = pm.getPersonRated();

        binding.title.setText(title);
        binding.description.setText(description);
        binding.price.setText("Rp. " + price);
        binding.rating.setText(String.format("%.1f", likes) + " | " + personRated + " Penilaian");

        binding.imageView6.setVisibility(View.GONE);

        Glide.with(this)
                .load(pm.getImage())
                .placeholder(R.drawable.ic_baseline_broken_image_24)
                .error(R.drawable.ic_baseline_broken_image_24)
                .into(binding.productDp);


        // beri penilaian terhadap produk / rating
        giveRatingAboutProduct();

        // tambahkan produk ke keranjang
        addProductToCart();

        // lihat review produk ini
        showReviewProduct();


    }

    private void showReviewProduct() {
        binding.comment.setOnClickListener(view -> {
            Intent intent = new Intent(DetailProductActivity.this, ReviewProductActivity.class);
            intent.putExtra(ReviewProductActivity.EXTRA_PRODUCT_NAME, title);
            intent.putExtra(ReviewProductActivity.EXTRA_ID, id);
            startActivity(intent);
        });
    }

    private void addProductToCart() {
        binding.addToCart.setOnClickListener(view -> showTotalProductAdded());
    }


    private void giveRatingAboutProduct() {
        binding.ratingBtn.setOnClickListener(view -> showRatingPopUp());
    }


    private void showTotalProductAdded() {
        Dialog dialog;
        Button btnAddToCart, btnDismiss;
        EditText etTotalProduct;
        ProgressBar pb;

        dialog = new Dialog(this);

        dialog.setContentView(R.layout.popup_cart);
        dialog.setCanceledOnTouchOutside(false);

        btnAddToCart = dialog.findViewById(R.id.addCart);
        btnDismiss = dialog.findViewById(R.id.dismissBtn);
        etTotalProduct = dialog.findViewById(R.id.totalProduct);
        pb = dialog.findViewById(R.id.progress_bar);


        btnDismiss.setOnClickListener(view -> dialog.dismiss());

        btnAddToCart.setOnClickListener(view -> {
            // jika pengguna menginputkan produk kosong atau 0
            if (etTotalProduct.getText().toString().trim().isEmpty() || Integer.parseInt(etTotalProduct.getText().toString().trim()) == 0) {
                Toast.makeText(DetailProductActivity.this, "Minimal 1 produk", Toast.LENGTH_SHORT).show();
                return;
            }

            // jika pengguna menginputkan produk >= 1
            pb.setVisibility(View.VISIBLE);
            int totalProduct = Integer.parseInt(etTotalProduct.getText().toString().trim());
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // total harga produk yang diambil
            int totalPrice = price * totalProduct;

            // ambil nama pembeli
            FirebaseFirestore
                    .getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // sembunyikan progress bar untuk selesai loading
                        saveCartProductToDatabase(id, title, description, totalPrice, dp, uid, pb, totalProduct, documentSnapshot.get("name").toString(), dialog);
                    })
                    .addOnFailureListener(e -> {
                        // sembunyikan progress bar untuk selesai loading
                        pb.setVisibility(View.GONE);
                        Toast.makeText(DetailProductActivity.this, "Gagal mendapatkan nama pembeli", Toast.LENGTH_SHORT).show();
                    });
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }


    private void showRatingPopUp() {
        Dialog dialog;
        Button btnSubmitRating, btnDismiss;
        RatingBar ratingBar;
        ProgressBar pb;

        dialog = new Dialog(this);

        dialog.setContentView(R.layout.popup_rating);
        dialog.setCanceledOnTouchOutside(false);


        btnSubmitRating = dialog.findViewById(R.id.submitRating);
        btnDismiss = dialog.findViewById(R.id.dismissBtn);
        ratingBar = dialog.findViewById(R.id.ratingBar);
        pb = dialog.findViewById(R.id.progress_bar);


        btnSubmitRating.setOnClickListener(view -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            saveRatingInDatabase(ratingBar.getRating(), pb, uid, dialog);
        });

        btnDismiss.setOnClickListener(view -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

    }


    private void saveCartProductToDatabase(
            String id,
            String title,
            String description,
            int totalPrice,
            String dp,
            String uid,
            ProgressBar pb,
            int totalProduct,
            String name,
            Dialog dialog) {

        // timeInMillis
        String timeInMillis = String.valueOf(System.currentTimeMillis());

        // ambil tanggal hari ini dengan format: dd - MMM - yyyy, HH:mm:ss
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat getDate = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
        String format = getDate.format(new Date());

        Map<String, Object> users = new HashMap<>();
        users.put("productId", id);
        users.put("bookedBy", name);
        users.put("userUid", uid);
        users.put("title", title);
        users.put("description", description);
        users.put("price", totalPrice);
        users.put("totalProduct", totalProduct);
        users.put("productDp", dp);
        users.put("addedAt", format);
        users.put("cartId", timeInMillis);


        FirebaseFirestore
                .getInstance()
                .collection("cart")
                .document(timeInMillis)
                .set(users)
                .addOnSuccessListener(unused -> {
                    // sembunyikan progress bar untuk selesai loading
                    pb.setVisibility(View.GONE);
                    Toast.makeText(DetailProductActivity.this, "Berhasil menambahkan produk ke keranjang", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    // sembunyikan progress bar untuk selesai loading
                    pb.setVisibility(View.GONE);
                    Toast.makeText(DetailProductActivity.this, "Ups, tidak berhasil menambahkan produk ke keranjang", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });

    }

    private void saveRatingInDatabase(float rating, ProgressBar pb, String uid, Dialog dialog) {

        Map<String, Object> rate = new HashMap<>();
        rate.put("uid", uid);
        rate.put("rating", rating);


        // simpan rating pengguna ke database
        pb.setVisibility(View.VISIBLE);
        FirebaseFirestore
                .getInstance()
                .collection("product")
                .document(id)
                .collection("rating")
                .document(uid)
                .set(rate)
                .addOnSuccessListener(unused -> {
                    pb.setVisibility(View.GONE);
                    dialog.dismiss();
                    Toast.makeText(DetailProductActivity.this, "Berhasil memberi penilaian", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    pb.setVisibility(View.GONE);
                    dialog.dismiss();
                    Toast.makeText(DetailProductActivity.this, "Gagal memberi penilaian", Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_edit_delete, menu);
        // jika bukan admin maka sembunyikan ikon ceklis untuk memverifikasi pembayaran
        MenuItem item = menu.findItem(R.id.edit).setVisible(false);
        MenuItem item2 = menu.findItem(R.id.delete).setVisible(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (("" + documentSnapshot.get("role")).equals("admin")) {
                        item.setVisible(true);
                        item2.setVisible(true);
                    }
                });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // update produk
        if (item.getItemId() == R.id.edit) {
            Intent intent = new Intent(this, UpdateProductActivity.class);
            intent.putExtra(UpdateProductActivity.PRODUCT_ID, id);
            intent.putExtra(UpdateProductActivity.TITLE, title);
            intent.putExtra(UpdateProductActivity.DESCRIPTION, description);
            intent.putExtra(UpdateProductActivity.PRICE, price);
            intent.putExtra(UpdateProductActivity.PRODUCT_DP, dp);
            startActivity(intent);
        }
        // Hapus produk
        else if (item.getItemId() == R.id.delete) {
            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Hapus Produk")
                    .setMessage("Apakah anada yakin ingin menghapus produk " + title + " ?")
                    .setPositiveButton("YA", (dialogInterface, i) -> {
                        FirebaseFirestore
                                .getInstance()
                                .collection("product")
                                .document(id)
                                .delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Intent intent = new Intent(this, HomeActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(DetailProductActivity.this, "Gagal menghapus produk", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("TIDAK", null)
                    .setIcon(R.drawable.ic_baseline_delete_24)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}