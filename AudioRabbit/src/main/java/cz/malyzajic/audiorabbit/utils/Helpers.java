package cz.malyzajic.audiorabbit.utils;

import java.io.File;

/**
 *
 * @author daop
 */
public class Helpers {

    public static String getMIMEType(String fileName) {
        File myFile = new File(fileName);
        String type = "";
        String fName = myFile.getName();
        String end = fName
                .substring(fName.lastIndexOf(".") + 1, fName.length())
                .toLowerCase();
        if (end.equals("m4a") || end.equals("mp3")) {
            type = "audio/mpeg";
        } else if (end.equals("m4a")) {
            type = "audio/mp4";
        } else if (end.equals("ogg")) {
            type = "audio/x-ogg";
        } else if (end.equals("flac")) {
            type = "audio/x-flac";
        } else if (end.equals("wav")) {
            type = "audio/wav";
        } else if (end.equals("3gp") || end.equals("mp4")) {
            type = "video";
        } else if (end.equals("jpg") || end.equals("gif") || end.equals("png")
                || end.equals("jpeg") || end.equals("bmp")) {
            type = "image";
        } else {
            type = "*";
        }
        return type;
    }
}
