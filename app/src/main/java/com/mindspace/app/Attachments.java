package com.mindspace.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

final class Attachments {
    static String save(Context ctx, Uri uri) throws Exception {
        File dir = new File(ctx.getFilesDir(), "attachments");
        dir.mkdirs();
        InputStream in = ctx.getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("Cannot open file");
        Bitmap bmp = BitmapFactory.decodeStream(in);
        in.close();
        if (bmp == null) throw new Exception("Not an image");
        float scale = Math.min(1f, 1024f / Math.max(bmp.getWidth(), bmp.getHeight()));
        if (scale < 1f) {
            bmp = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * scale),
                    Math.round(bmp.getHeight() * scale), true);
        }
        File out = new File(dir, System.currentTimeMillis() + ".jpg");
        FileOutputStream fos = new FileOutputStream(out);
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        fos.close();
        return out.getAbsolutePath();
    }

    static String dataUrl(String path) throws Exception {
        FileInputStream in = new FileInputStream(path);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        in.close();
        return "data:image/jpeg;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }
}
