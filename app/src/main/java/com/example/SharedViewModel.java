package com.example;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<String> avatarLink = new MutableLiveData<>();
    private final MutableLiveData<String> userEmail = new MutableLiveData<>();

    public void setUserName(String name) {
        userName.setValue(name);
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public void setAvatarLink(String link) {
        avatarLink.setValue(link);
    }

    public LiveData<String> getAvatarLink() {
        return avatarLink;
    }

    public void setUserEmail(String email) {
        userEmail.setValue(email);
    }

    public LiveData<String> getUserEmail() {
        return userEmail;
    }
}
