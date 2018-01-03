package com.kushank.olaplaybykushank;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Portfolio extends AppCompatActivity {

    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendEmailBug(null);
            }
        });

        ((ImageView) findViewById(R.id.my_photo))
                .setImageBitmap(getCroppedBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.my_photo)
                ));

        fileName = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + "/Resume-Kushank_Arora.pdf";

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    //This is used to open Email Intent.
    //It is called on click of floating action bar or emailid.
    public void sendEmailBug(View view) {
        String to = "kushank97@gmail.com";
        String subject = "Response from OLA-Android Contest";
        String msg = "Hey!\nYou are selected!";

        Uri uri = Uri.parse("mailto:")
                .buildUpon()
                .appendQueryParameter("subject", subject)
                .appendQueryParameter("body", msg)
                .build();

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        //emailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        startActivity(Intent.createChooser(emailIntent, "Choose an Email client :"));
    }

    //This is used to crop the bitmap in circular shape.
    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    //Function to download Portfolio
    //It is called on click of download button
    public void downloadPortfolio(View view) {
        try {
            InputStream input = getResources().openRawResource(R.raw.portfolio);
            OutputStream output = new FileOutputStream(fileName);

            byte data[] = new byte[1024];

            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();

            output.close();
            input.close();
            Toast.makeText(Portfolio.this, "PDF Downloaded!", Toast.LENGTH_SHORT).show();

            File file = new File(fileName);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
