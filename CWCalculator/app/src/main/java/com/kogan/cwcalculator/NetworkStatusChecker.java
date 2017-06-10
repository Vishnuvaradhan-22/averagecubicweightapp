package com.kogan.cwcalculator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Vish on 25/05/2017.
 */
public class NetworkStatusChecker {
    public static boolean getNetworkConnectivity(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if(activeNetwork != null)
            return true;
        else
            return false;
    }
}
