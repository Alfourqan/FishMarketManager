package com.poissonnerie.view;

@FunctionalInterface
public interface LoginSuccessListener {
    void onLoginSuccess(String username);
}