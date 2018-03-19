package com.softwarejoint.bert;

import android.text.TextUtils;

public class BertAtom{

    private static final int ATOM_EXT_MAX_LEN = 255;

    private String atom;

    public BertAtom(String atom) throws IllegalArgumentException{
        if (TextUtils.isEmpty(atom)) {
            throw new IllegalArgumentException("Atom cannot be null");
        }
        if (atom.length() > ATOM_EXT_MAX_LEN) {
            throw new IllegalArgumentException("Atom max length can be only " + ATOM_EXT_MAX_LEN);
        }
        this.atom = atom;
    }

    public String get() {
        return atom;
    }

    @Override
    public String toString() {
        return atom;
    }
}
