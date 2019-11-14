package com.android.jingyi.sipdemo;

import android.gov.nist.javax.sip.clientauthutils.UserCredentials;

public class JainSipUserCredentialsImpl implements UserCredentials {
    private String userName;
    private String sipDomain;
    private String password;

    public JainSipUserCredentialsImpl(String userName, String sipDomain, String password)
    {
        this.userName = userName;
        this.sipDomain = sipDomain;
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }

    public String getSipDomain()
    {
        return sipDomain;
    }

    public String getUserName()
    {
        return userName;
    }
}
