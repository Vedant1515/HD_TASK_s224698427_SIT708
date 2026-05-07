package com.example.nutriai;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.nutriai.fragment.LoginFragment;
import com.example.nutriai.fragment.SignupFragment;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        if (savedInstanceState == null) {
            showLogin(false);
        }
    }

    public void showLogin(boolean addToBackStack) {
        replaceFragment(new LoginFragment(), addToBackStack);
    }

    public void showSignup() {
        replaceFragment(new SignupFragment(), true);
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction()
                .replace(R.id.authContainer, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }
}
