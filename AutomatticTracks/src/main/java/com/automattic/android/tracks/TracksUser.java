package com.automattic.android.tracks;

public class TracksUser {

    private String mUserID;
    private String mEmail;
    private String mUsername;

    public TracksUser(String userID, String email, String username) {
        mUserID = userID;
        mEmail = email;
        mUsername = username;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getUserID() {
        return mUserID;
    }
}
