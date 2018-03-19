package com.softwarejoint.bert;

import java.util.ArrayList;

/**
 * Created by pankajsoni on 25/04/16.
 */
public class BertTuple extends ArrayList<Object> {

    public boolean isKV() {
        return size() == 2;
    }
}
