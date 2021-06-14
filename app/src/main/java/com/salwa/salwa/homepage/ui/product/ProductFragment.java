package com.salwa.salwa.homepage.ui.product;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salwa.salwa.databinding.FragmentProductBinding;

import org.jetbrains.annotations.NotNull;

public class ProductFragment extends Fragment {

    private FragmentProductBinding binding;
    private String uid;
    private boolean isVisible = true;

    private ProductAdapter productAdapter;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Produk Tersedia");
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductBinding.inflate(inflater, container, false);

        initRecyclerView();
        initViewModel();

        // cek apakah user yang sedang login ini admin atau user biasa
        checkIsAdminOrNot();




        // show hide selamat datang
        showHideNameAndGreeting();

        return binding.getRoot();
    }

    private void initRecyclerView() {
        // tampilkan daftar cookies
        binding.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        productAdapter = new ProductAdapter();
        binding.recyclerView.setAdapter(productAdapter);
    }

    // inisiasi view model untuk menampilkan list produk
    private void initViewModel() {
        ProductViewModel productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        binding.progressBar.setVisibility(View.VISIBLE);
        productViewModel.setProductList();
        productViewModel.getProductList().observe(getViewLifecycleOwner(), productList -> {
            Log.e("TAG", String.valueOf(productList.size()));
            if (productList.size() > 0) {
                binding.noData.setVisibility(View.GONE);
                productAdapter.setData(productList);
            } else {
                binding.noData.setVisibility(View.VISIBLE);
            }
            binding.progressBar.setVisibility(View.GONE);
        });
    }

    private void showHideNameAndGreeting() {
        binding.showHideName.setOnClickListener(view -> {
            if(isVisible) {
                binding.nameTv.setVisibility(View.GONE);
                binding.textView2.setVisibility(View.GONE);
                isVisible = false;
            } else {
                binding.nameTv.setVisibility(View.VISIBLE);
                binding.textView2.setVisibility(View.VISIBLE);
                isVisible = true;
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // klik tombol tambah produk
        binding.addProduct.setOnClickListener(view1 ->
                startActivity(new Intent(getActivity(), AddProductActivity.class)));
    }

    @SuppressLint("SetTextI18n")
    private void checkIsAdminOrNot() {
        // CEK APAKAH USER YANG SEDANG LOGIN ADMIN ATAU BUKAN, JIKA YA, MAKA TAMPILKAN tombol add product
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        binding.nameTv.setText("Halo, " + document.get("name"));
                        if (document.get("role") == "admin") {
                            binding.addProduct.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}