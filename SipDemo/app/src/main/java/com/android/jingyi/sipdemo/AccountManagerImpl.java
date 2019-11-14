package com.android.jingyi.sipdemo;

import android.gov.nist.javax.sip.clientauthutils.AccountManager;
import android.gov.nist.javax.sip.clientauthutils.UserCredentials;
import android.javax.sip.ClientTransaction;

public class AccountManagerImpl implements AccountManager {

    String Username;
    String Password;
    String RemoteIp;

    public AccountManagerImpl(String username, String RemoteIp, String password)
    {
        this.Username = username;
        this.Password = password;
        this.RemoteIp = RemoteIp;

    }

    @Override
    public UserCredentials getCredentials(ClientTransaction clientTransaction, String s) {
        return new JainSipUserCredentialsImpl(Username, RemoteIp, Password);
    }
}
