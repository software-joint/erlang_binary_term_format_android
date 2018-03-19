package com.softwarejoint.bert;

import java.nio.charset.Charset;

@SuppressWarnings("WeakerAccess")
public abstract class Bert {

    protected Charset utf8Charset;

    protected @ExternalFormat int minorVersion = ExternalFormat.NEW;

    protected Bert(){
        utf8Charset = Charset.forName("UTF-8");
    }

    public void setErlangMinorVersion(@ExternalFormat int version) {
        minorVersion = version;
    }
}
