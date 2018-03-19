package com.softwarejoint.bert;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.softwarejoint.bert.ExternalFormat.NEW;
import static com.softwarejoint.bert.ExternalFormat.OLD;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 19/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({OLD, NEW})
public @interface ExternalFormat {
    int OLD = 0;
    int NEW = 1;
}
