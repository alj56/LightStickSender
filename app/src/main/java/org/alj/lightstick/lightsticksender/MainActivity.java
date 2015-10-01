package org.alj.lightstick.lightsticksender;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private int MIN_PACKET_SIZE = 7;

    final int ACTIVITY_CHOOSE_FILE = 1;

    private EditText txtIpAddress;
    private EditText valSize;
    private EditText valFrames;
    private EditText txtFilename;
    private ProgressBar pgrDelay;
    private Button btnBlack;
    private Button btnRainbow;
    private Button btnWhite;
    private Button btnSelectFile;
    private Button btnStartFile;
    private Button btnStartFile5Sec;
    private Button btnUploadFile;
    private Button btnStartUploaded;

    private String filePath = null;

    private BusyUpdater busyUpdater = new BusyUpdater();
    private Handler busyUpdaterHandler = new Handler(Looper.getMainLooper());
    private AlertDialogDisplay alertDialogDisplay = new AlertDialogDisplay();
    private Handler alertDialogDisplayHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtIpAddress = (EditText)findViewById(R.id.txtIpAddress);
        valSize = (EditText)findViewById(R.id.valSize);
        valFrames = (EditText)findViewById(R.id.valFrames);
        txtFilename = (EditText)findViewById(R.id.txtFilename);
        pgrDelay = (ProgressBar)findViewById(R.id.pgrDelay);
        btnBlack = (Button)findViewById(R.id.btnBlack);
        btnRainbow = (Button)findViewById(R.id.btnRainbow);
        btnWhite = (Button)findViewById(R.id.btnWhite);
        btnSelectFile = (Button)findViewById(R.id.btnSelectFile);
        btnStartFile = (Button)findViewById(R.id.btnStartFile);
        btnStartFile5Sec = (Button)findViewById(R.id.btnStartFile5Sec);
        btnUploadFile = (Button)findViewById(R.id.btnUploadFile);
        btnStartUploaded = (Button)findViewById(R.id.btnStartUploaded);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case ACTIVITY_CHOOSE_FILE: {
                if (resultCode == RESULT_OK){
                    Uri uri = data.getData();
                    filePath = uri.getPath();
                    txtFilename.setText(uri.getLastPathSegment(), EditText.BufferType.EDITABLE);
                }
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private int getSize() {
        try {
            return Integer.parseInt(valSize.getText().toString());
        } catch (NumberFormatException ex) {
            Log.e(TAG, "error in getSite", ex);
            return 0;
        }
    }


    private int getBufferSize(int thePixelCount) {
        return thePixelCount * 3 + MIN_PACKET_SIZE;
    }


    private byte[] initBuffer(int theSize) {
        int aBufferSize = getBufferSize(theSize);
        byte aResult[] = new byte[aBufferSize];
        aResult[0] = 'L';
        aResult[1] = 'S';
        aResult[2] = 0;
        aResult[3] = 1;
        aResult[4] = 'L';
        aResult[5] = (byte)((aBufferSize - MIN_PACKET_SIZE) % 256);
        aResult[6] = (byte)((aBufferSize - MIN_PACKET_SIZE) / 256);
        return aResult;
    }


    private int getBufferIndex(int thePixel) {
        return MIN_PACKET_SIZE + thePixel * 3;
    }


    public void btnBlackClicked(View theView) {
        int aSize = getSize();
        byte[] aBuffer = initBuffer(aSize);
        for (int i = 0; i < aSize; i++) {
            int anIndex = getBufferIndex(i);
            aBuffer[anIndex] = 0;
            aBuffer[anIndex + 1] = 0;
            aBuffer[anIndex + 2] = 0;
        }
        sendBuffer(aBuffer, true, true);
    }


    public void btnRainbowClicked(View theView) {
        int aSize = getSize();
        byte[] aBuffer = initBuffer(aSize);
        for (int i = 0; i < aSize; i++) {
            int anIndex = getBufferIndex(i);
            int aColor = Color.HSVToColor(new float[]{(float) (i * 360 / aSize), 1.0f, 0.5f});
            aBuffer[anIndex] = (byte)Color.red(aColor);
            aBuffer[anIndex + 1] = (byte)Color.green(aColor);
            aBuffer[anIndex + 2] = (byte)Color.blue(aColor);
        }
        sendBuffer(aBuffer, true, true);
     }


    public void btnWhiteClicked(View theView) {
        int aSize = getSize();
        byte[] aBuffer = initBuffer(aSize);
        for (int i = 0; i < aSize; i++) {
            int anIndex = getBufferIndex(i);
            aBuffer[anIndex] = (byte)255;
            aBuffer[anIndex + 1] = (byte)255;
            aBuffer[anIndex + 2] = (byte)255;
        }
        sendBuffer(aBuffer, true, true);
    }


    public void btnSelectFileClicked(View theView) {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("file/*");
        intent = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
    }


    public void btnStartFileClicked(View theView) {
        doStartFile(theView);
    }


    public void btnStartFile5SecClicked(final View theView) {
        new DelayedStart() {
            protected void doAfterDelay() {
                doStartFile(theView);
            }
        }.execute();
    }


    public void btnUploadFileClicked(final View theView) {
        final String anIpAddress = txtIpAddress.getText().toString();
        Bitmap aBitmap = BitmapFactory.decodeFile(filePath);
        new UploadBitmap(anIpAddress, aBitmap).execute();
    }


    public void btnStartUploadedClicked(final View theView) {
       new DelayedStart() {
            protected void doAfterDelay() {
                doStartUploaded(theView);
            }
        }.execute();
    }


    private void doStartFile(View theView) {
        Bitmap aBitmap = BitmapFactory.decodeFile(filePath);
        String anIpAddress = txtIpAddress.getText().toString();
        int aFramesPerSecond = Integer.parseInt(valFrames.getText().toString());
        Thread aThread = new Thread(new SendBitmap(anIpAddress, aFramesPerSecond, aBitmap));
        aThread.setPriority(Thread.MAX_PRIORITY);
        aThread.start();
    }


    private void doStartUploaded(View theView) {
        String anIpAddress = txtIpAddress.getText().toString();
        int aFramesPerSecond = Integer.parseInt(valFrames.getText().toString());
        new StartUploaded(anIpAddress, aFramesPerSecond).execute();
    }


    private void sendBuffer(byte[] theBuffer, boolean theToUdp, boolean theToTcp) {
        String anIpAddress = txtIpAddress.getText().toString();
        new SendBufferUDP(anIpAddress)
                .setToUdp(theToUdp)
                .setToTcp(theToTcp)
                .execute(theBuffer);
    }


    private static final void sendBufferUDP(String theIpAddress, byte[] theBuffer, boolean theToUdp, boolean theToTcp) {
        try {
            if (theToUdp) {
                byte[] aBuffer = new byte[] {0x4c, 0x53, 0x00, 0x01, 0x55, 0x00}; // LS<0><1><U
                Socket aSocket = new Socket(theIpAddress, 7778);
                OutputStream anOS = aSocket.getOutputStream();
                anOS.write(aBuffer);
                anOS.flush();
                anOS.close();
                Thread.sleep(500);
            }
            DatagramSocket aSocket = new DatagramSocket();
            InetAddress anInetAdddress = InetAddress.getByName(theIpAddress);
            DatagramPacket aPacket = new DatagramPacket(theBuffer, theBuffer.length, anInetAdddress, 7777);
            aSocket.send(aPacket);
            if (theToTcp) {
                final byte[] aBuffer = new byte[] {'L', 'S', 0x00, 0x01, 'T'};
                aPacket = new DatagramPacket(aBuffer, aBuffer.length, anInetAdddress, 7777);
                aSocket.send(aPacket);
            }
        } catch (Exception ex) {
            Log.e(TAG, "error in doInBackground", ex);
        }
    }


    private abstract class DelayedStart extends  AsyncTask<Void, Integer, Void> {

        public DelayedStart() {
            pgrDelay.setProgress(0);
            setBusy(true);
        }

        protected Void doInBackground(Void... theNothing) {
            for (int i = 0; i < 50; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                publishProgress(i * 2);
            }
            return null;
        }

        protected void onProgressUpdate(Integer... theProgress) {
            pgrDelay.setProgress(theProgress[0]);
            if (theProgress[0] % 20 == 0) {
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 200);
            }
        }

        protected void onPostExecute(Void theResult) {
            pgrDelay.setProgress(100);
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            doAfterDelay();
        }

        abstract protected void doAfterDelay();
    };


    private class SendBufferUDP extends AsyncTask<byte[], Void, Void> {

        private static final String TAG = MainActivity.TAG + ".SendBufferUDP";

        private String ipAddress;
        private boolean toUdp = false;
        private boolean toTcp = false;

        public SendBufferUDP(String theIpAddress) {
            ipAddress = theIpAddress;
        }

        @Override
        protected Void doInBackground(byte[]... params) {
            try {
                setBusy(true);
                sendBufferUDP(ipAddress, params[0], toUdp, toTcp);
            } finally {
                setBusy(false);
            }
            return null;
        }


        public SendBufferUDP setToUdp(boolean theValue) {
            toUdp = theValue;
            return this;
        }


        public SendBufferUDP setToTcp(boolean theValue) {
            toTcp = theValue;
            return this;
        }
    }


    private class SendBitmap implements Runnable {

        private static final String TAG = MainActivity.TAG + ".SendBitmap";

        private String ipAddress;
        private int framesPerSecond;
        private Bitmap bitmap;

        public SendBitmap(String theIpAddress, int theFramesPerSecond, Bitmap theBitmap) {
            ipAddress = theIpAddress;
            framesPerSecond = theFramesPerSecond;
            bitmap = theBitmap;
        }


        @Override
        public void run() {
            try {
                setBusy(true);
                int aHeight = bitmap.getHeight();
                int aWidth = bitmap.getWidth();
                long aDelta = 1000 / framesPerSecond;
                byte[] aBuffer = initBuffer(aHeight);
                for (int i = 0; i < aWidth; i++) {
                    long aStart = System.currentTimeMillis();
                    for (int j = 0; j < aHeight; j++) {
                        int aPixel = bitmap.getPixel(i, j);
                        int anIndex = getBufferIndex(j);
                        aBuffer[anIndex] = (byte)Color.red(aPixel);
                        aBuffer[anIndex + 1] = (byte)Color.green(aPixel);
                        aBuffer[anIndex + 2] = (byte)Color.blue(aPixel);
                    }
                    sendBufferUDP(ipAddress, aBuffer, i == 0, false);
//                while (aStart + aDelta > System.currentTimeMillis()) {}
                    try {
                        long aDelay = aStart + aDelta - System.currentTimeMillis();
                        Thread.sleep(aDelay>0?aDelay:0);
                    } catch (InterruptedException ex) {
                    }
                }
                // black
                for (int i = 0; i < aHeight; i++) {
                    int anIndex = getBufferIndex(i);
                    aBuffer[anIndex] = 0;
                    aBuffer[anIndex + 1] = 0;
                    aBuffer[anIndex + 2] = 0;
                }
                sendBufferUDP(ipAddress, aBuffer, false, true);
            } finally {
                setBusy(false);
            }
         }
    }


    private class UploadBitmap extends AsyncTask<Void, Void, Void> {

        private String ipAddress;
        private Bitmap bitmap;

        public UploadBitmap(String theIpAddress, Bitmap theBitmap) {
            ipAddress = theIpAddress;
            bitmap = theBitmap;
       }


        @Override
        protected Void doInBackground(Void... params) {
            int aHeight = bitmap.getHeight();
            int aWidth = bitmap.getWidth();
            try {
                setBusy(true);
                Socket aSocket = new Socket(ipAddress, 7778);
                OutputStream anOS = aSocket.getOutputStream();
                InputStream anIS = aSocket.getInputStream();
                anOS.write(new byte[] {'L', 'S', 0x00, 0x01, 'L'});
                anOS.write(new byte[] {'L', 'S', 0x00, 0x01, 'B'});
                anOS.write(aHeight%256);
                anOS.write(aHeight/256);
                anOS.write(aWidth%256);
                anOS.write(aWidth/256);
                for (int i = 0; i < aWidth; i++) {
                    for (int j = 0; j < aHeight; j++) {
                        int aPixel = bitmap.getPixel(i, j);
                        anOS.write(Color.red(aPixel));
                        anOS.write(Color.green(aPixel));
                        anOS.write(Color.blue(aPixel));
                    }
                }
                anOS.write(0x00); // why?
                anOS.flush();
                showResultFromLightStick(anIS);
                anIS.close();
                anOS.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                setBusy(false);
            }
            return null;
        }
    }


    private class StartUploaded extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "MA.StartUploaded";

        private String ipAddress;
        private int framesPerSecond;

        public StartUploaded(String theIpAddress, int theFramesPerSecond) {
            ipAddress = theIpAddress;
            framesPerSecond = theFramesPerSecond;
        }


        @Override
        protected Void doInBackground(Void... params) {
            try {
                setBusy(true);
                int aDelayMs = 1000 / framesPerSecond;
                Socket aSocket = new Socket(ipAddress, 7778);
                OutputStream anOS = aSocket.getOutputStream();
                InputStream anIS = aSocket.getInputStream();
                anOS.write(new byte[] {'L', 'S', 0x00, 0x01, 'S'});
                anOS.write(aDelayMs % 256);
                anOS.write((aDelayMs / 256) % 256);
                anOS.write(0x00);
                anOS.flush();
                showResultFromLightStick(anIS);
                anIS.close();
                anOS.close();
            } catch (Exception ex) {
                Log.e(TAG, "error in StartUploaded", ex);
            } finally {
                setBusy(false);
            }
            return null;
        }
    }


    private String showResultFromLightStick(InputStream theIS) throws IOException {
        final int BUFFER_SIZE = 256;
        byte buffer[] = new byte[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) buffer[i] = 0;
        int aNumBytes = theIS.read(buffer, 0, BUFFER_SIZE);
        String aResult =  new String(buffer, 0, aNumBytes);
        if (!"OK".equals(aResult)) {
            alertDialogDisplay.setMessage(this, aResult);
            alertDialogDisplayHandler.post(alertDialogDisplay);
        }
        return aResult;
    }


    private class BusyUpdater implements Runnable {

        private boolean busy = false;

        private void setBusy(boolean theBusy) {
            busy = theBusy;
        }


        @Override
        public void run() {
            btnBlack.setEnabled(!busy);
            btnRainbow.setEnabled(!busy);
            btnWhite.setEnabled(!busy);
            btnSelectFile.setEnabled(!busy);
            btnStartFile.setEnabled(!busy);
            btnStartFile5Sec.setEnabled(!busy);
            btnUploadFile.setEnabled(!busy);
            btnStartUploaded.setEnabled(!busy);
        }
    }


    private void setBusy(boolean theBusy) {
        busyUpdater.setBusy(theBusy);
        busyUpdaterHandler.post(busyUpdater);
    }


    private class AlertDialogDisplay implements Runnable {

        private Context context = null;
        private String message = "???";

        private void setMessage(Context theContext, String theMessage) {
            context = theContext;
            message = theMessage;
        }


        @Override
        public void run() {
            new AlertDialog.Builder(context)
                    .setTitle("FAILED")
                    .setMessage(message)
                    .setNeutralButton("Close", null)
                    .show();
        }
    }
}
