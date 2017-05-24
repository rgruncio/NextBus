package com.rgruncio.nextbus;

import java.io.IOException;

/**
 * Created by Rafał on 24.05.2017.
 */

public class SQLQuery {

    private final String phpFile = "http://sundiamore.pl/nextbus/sendQuery.php";

    private String queryString;

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void sendQueryToPHP() {


    }
}
