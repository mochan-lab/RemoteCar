package com.mochan.unitynativepluginbcore4;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.unity3d.player.UnityPlayer;

public class AndroidNativeDialog {
    static public void showNativeDialog(Context context, String title, String message){
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("はい", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sendMessage(message + "：はい");
                    }
                })
                .setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sendMessage(message + "：いいえ");
                    }
                })
                .show();
    }

    static private void sendMessage(String str){
        UnityPlayer.UnitySendMessage("Canvas", "CalledFromAndroid",str);
    }
}