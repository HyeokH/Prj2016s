package com.example.prj2016s.RtspServer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.prj2016s.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 성혁화 on 2016-06-06.
 *
 */
public class RtspServerService extends Service {
    private final IBinder mBinder = new LocalBinder();

    public final static String KEY_ENABLED = "rtsp_enabled";

    public final static String KEY_PORT = "rtsp_port";
    public final static String TAG = "RtspServerService";
    public static String SERVER_NAME = "HyeokHwa RTSP Server";
    public static final int DEFAULT_VIDEO_PORT = 51372;
    public static final int DEFAULT_AUDIO_PORT = 49170;
    public static final int DEFAULT_RTSP_PORT = 8086;
    protected int mPort = DEFAULT_RTSP_PORT;
    protected SharedPreferences mSharedPreferences;
    static int FRAME_PERIOD = 100;
    static int VIDEO_LENGTH = 500;
    static int MJPEG_TYPE = 26;
    static int RTSP_ID = 123456; //ID of the RTSP session
    int RTP_dest_port = 0;

    private RequestListener mListenerThread;

    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    final static String CRLF = "\r\n";
    static int state;
    //private ServerSocket mListenSocket;
    private boolean mRestart = false;
    protected boolean mEnabled = true;

    public void start() {
        if (!mEnabled || mRestart) stop();
        if (mEnabled && mListenerThread == null) {
            try {
                mListenerThread = new RequestListener();
            } catch (Exception e) {
                mListenerThread = null;
                Log.d(TAG, String.valueOf(e));
            }
        }
        mRestart = false;
    }

    public void stop() {
        if (mListenerThread != null) {
            try {
                mListenerThread.kill();
            } catch (Exception e) {
            } finally {
                mListenerThread = null;
            }
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() start");

        // Let's restore the state of the service
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPort = Integer.parseInt(mSharedPreferences.getString(KEY_PORT, String.valueOf(mPort)));
        mEnabled = mSharedPreferences.getBoolean(KEY_ENABLED, mEnabled);

        // If the configuration is modified, the server will adjust
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

        start();
        return;
    }


    public void onDestroy() {
        Log.d(TAG, "Destroy()");
        stop();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(KEY_PORT)) {
                int port = Integer.parseInt(sharedPreferences.getString(KEY_PORT, String.valueOf(mPort)));
                if (port != mPort) {
                    mPort = port;
                    mRestart = true;
                    start();
                }
            }
            else if (key.equals(KEY_ENABLED)) {
                mEnabled = sharedPreferences.getBoolean(KEY_ENABLED, mEnabled);
                start();
            }
        }
    };

    class RequestListener extends Thread implements Runnable {

        private final ServerSocket mServer;

        public RequestListener() throws IOException {
            try {
                //mServer = new ServerSocket(mPort);
                mServer = new ServerSocket(8086);
                start();
            } catch (BindException e) {
                Log.e(TAG,"Port already in use !");
                throw e;
            }
        }

        public void run() {
            Log.i(TAG,"RTSP server listening on port "+mServer.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    state = INIT;

                    Log.d(TAG, "Waiting mServer.accept()");
                    Socket client = mServer.accept();
                    Log.d(TAG, "accepted.");
                    new WorkerThread(client).start();
                } catch (SocketException e) {
                    Log.e(TAG, "SocketExecption : " + e.getMessage());
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "IOExecption : " + e.getMessage());
                    continue;
                }
            }
            Log.i(TAG,"RTSP server stopped !");
        }

        public void kill() {
            try {
                mServer.close();
            } catch (IOException e) {}
            try {
                this.join();
            } catch (InterruptedException ignore) {}
        }

    }

    class WorkerThread extends Thread implements Runnable {

        private final Socket mRTSPsocket;
        private final OutputStream mOutput;
        private final BufferedReader mInput;
        private int sendDelay;
        private int imagenb;
        byte[] buf;
        private DatagramSocket mRTPsocket;
        private DatagramPacket senddp;
        private VideoStream video;



        public WorkerThread(final Socket RTSPsocket) throws IOException {
            mInput = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
            mOutput = RTSPsocket.getOutputStream();
            mRTSPsocket = RTSPsocket;
            imagenb = 0;
            mRTPsocket = new DatagramSocket();

            try {
                InputStream fis = getResources().openRawResource(R.raw.sample1);
                video = new VideoStream(fis);
            }
            catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        public void run() {
            Request request;
            Response response;

            Log.i(TAG, "Connection from " + mRTSPsocket.getInetAddress().getHostAddress());

            while (!Thread.interrupted()) {

                request = null;
                response = null;
                // Parse the request
                try {
                    request = Request.parseRequest(mInput);
                } catch (SocketException e) {
                    Log.d(TAG, "client has left : " + e.getMessage());
                    break;
                } catch (Exception e) {
                    // We don't understand the request :/
                    response = new Response();
                    response.status = Response.STATUS_BAD_REQUEST;
                }

                // Do something accordingly like starting the streams, sending a session description
                if (request != null) {
                    try {
                        response = processRequest(request);
                    } catch (Exception e) {
                        // This alerts the main thread that something has gone wrong in this thread
                        Log.e(TAG, e.getMessage() != null ? e.getMessage() : "An error occurred");
                        e.printStackTrace();
                        response = new Response(request);
                    }
                }

                // We always send a response
                // The client will receive an "INTERNAL SERVER ERROR" if an exception has been thrown at some point
                try {
                    response.send(mOutput);
                    Log.d(TAG, "Response OK");
                } catch (IOException e) {
                    Log.e(TAG, "Response was not sent properly");
                    break;
                }
            }
            try {
                mRTSPsocket.close();
                Log.d(TAG, "mRTSPsocket.close() OK");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            Log.i(TAG, "Client disconnected");

        }

        public Response processRequest(Request request) throws IOException {
            Response response = new Response(request);
            if (request.method.equalsIgnoreCase("DESCRIBE")) {
                Log.d(TAG, "New RTSP request: DESCRIBE");

                String requestContent = getSessionDescription();
                String requestAttributes =
                        "Content-Base: " + mRTSPsocket.getLocalAddress().getHostAddress() + ":" + mRTSPsocket.getLocalPort() + "/\r\n" +
                                "Content-Type: application/sdp\r\n";

                response.attributes = requestAttributes;
                response.content = requestContent;

                // If no exception has been thrown, we reply with OK
                response.status = Response.STATUS_OK;

            } else if (request.method.equalsIgnoreCase("OPTIONS")) {
                Log.d(TAG, "RTSP request: OPTIONS");

            } else if (request.method.equalsIgnoreCase("SETUP")) {
                Log.d(TAG, "RTSP request: SETUP");
                Log.d(TAG, "New RTSP state: READY");
            } else if (request.method.equalsIgnoreCase("PLAY") && state == READY) {
                Log.d(TAG, "RTSP request: PLAY");
                Log.d(TAG, "New RTSP state: PLAYING");
                state = PLAYING;

            } else if (request.method.equalsIgnoreCase("PAUSE") && state == PLAYING) {
                Log.d(TAG, "RTSP request: READY");
                Log.d(TAG, "New RTSP state: READY");
                state = READY;

            } else if (request.method.equalsIgnoreCase("TEARDOWN")) {
                Log.d(TAG, "RTSP request: TEARDOWN");
                mRTSPsocket.close();
                mRTPsocket.close();
            } else {
                Log.e(TAG, "Command unknown: " + request);
            }
            return response;
        }

        public String getSessionDescription() {
            StringWriter writer1 = new StringWriter();
            StringWriter writer2 = new StringWriter();

            // Write the body first so we can get the size later
            writer2.write("v=0" + CRLF);
            writer2.write("m=video " + DEFAULT_RTSP_PORT + " RTP/AVP " + MJPEG_TYPE + CRLF);
            writer2.write("a=control:streamid=" + RTSP_ID + CRLF);
            writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
            writer2.write("a=rtpmap:"+ MJPEG_TYPE +" MP4V-ES/5544" + CRLF);
            String body = writer2.toString();

            return body;
            /*
            StringBuilder sessionDescription = new StringBuilder();
            sessionDescription.append(
                    "m=video " + DEFAULT_VIDEO_PORT + " RTP/AVP 96\n" +
                            //"a=control:streamid=0\n" +
                            //"a=range:npt=0-7.741000\n" +
                            //"a=length:npt=7.741000\n" +
                            //"a=rtpmap:96 MP4V-ES/5544\n" +
                            //"a=mimetype:string;\"video/MP4V-ES\"\n" +
                            //"a=AvgBitRate:integer;304018\n" +
                            //"a=StreamName:string;\"hinted video track\"\n" +
                            //"m=audio "+ DEFAULT_AUDIO_PORT + " RTP/AVP 97\n" +
                            //"a=control:streamid=1\n" +
                            //"a=range:npt=0-7.712000\n" +
                            //"a=length:npt=7.712000\n" +
                            //"a=rtpmap:97 mpeg4-generic/32000/2\n" +
                            //"a=mimetype:string;\"audio/mpeg4-generic\"\n" +
                            //"a=AvgBitRate:integer;65790\n" +
                            //"a=StreamName:string;\"hinted audio track\""
                            ""
            );
            return sessionDescription.toString();*/
        }

        public void actionPerformed() {
            byte[] frame;

            //if the current image nb is less than the length of the video
            if (imagenb < VIDEO_LENGTH) {
                imagenb++;
                try {
                    //get next frame to send from the video, as well as its size
                    int image_length = video.getnextframe(buf);

                    //Builds an RTPpacket object containing the frame
                    RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, buf, image_length);

                    //get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getlength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    senddp = new DatagramPacket(packet_bits, packet_length, mRTSPsocket.getInetAddress(), RTP_dest_port);
                    mRTPsocket.send(senddp);

                    //System.out.println("Send frame #"+imagenb);
                    //print the header bitstream
                    rtp_packet.printheader();
                }
                catch(Exception ex)
                {
                    Log.e("TAG", ex.getMessage());
                }
            }
        }
    }

    static class Request {

        // Parse method & uri
        public static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP",Pattern.CASE_INSENSITIVE);
        // Parse a request header
        public static final Pattern rexegHeader = Pattern.compile("(\\S+):(.+)",Pattern.CASE_INSENSITIVE);

        public String method;
        public String uri;
        public HashMap<String,String> headers = new HashMap<String,String>();

        /** Parse the method, uri & headers of a RTSP request */
        public static Request parseRequest(BufferedReader input) throws IOException, IllegalStateException, SocketException {
            Request request = new Request();
            String line;
            Matcher matcher;

            // Parsing request method & uri
            if ((line = input.readLine())==null) throw new SocketException("Client disconnected case1");
            matcher = regexMethod.matcher(line);
            matcher.find();
            request.method = matcher.group(1);
            request.uri = matcher.group(2);

            // Parsing headers of the request
            while ( (line = input.readLine()) != null && line.length()>3 ) {
                matcher = rexegHeader.matcher(line);
                matcher.find();
                request.headers.put(matcher.group(1).toLowerCase(Locale.US),matcher.group(2));
            }
            if (line==null) throw new SocketException("Client disconnected case2");

            // It's not an error, it's just easier to follow what's happening in logcat with the request in red
            Log.e(TAG,request.method+" "+request.uri);

            return request;
        }
    }

    static class Response {

        // Status code definitions
        public static final String STATUS_OK = "200 OK";
        public static final String STATUS_BAD_REQUEST = "400 Bad Request";
        public static final String STATUS_UNAUTHORIZED = "401 Unauthorized";
        public static final String STATUS_NOT_FOUND = "404 Not Found";
        public static final String STATUS_INTERNAL_SERVER_ERROR = "500 Internal Server Error";

        public String status = STATUS_INTERNAL_SERVER_ERROR;
        public String content = "";
        public String attributes = "";

        private final Request mRequest;

        public Response(Request request) {
            this.mRequest = request;
        }

        public Response() {
            // Be carefull if you modify the send() method because request might be null !
            mRequest = null;
        }

        public void send(OutputStream output) throws IOException {
            int seqid = -1;

            try {
                seqid = Integer.parseInt(mRequest.headers.get("cseq").replace(" ",""));
            } catch (Exception e) {
                Log.e(TAG,"Error parsing CSeq: "+(e.getMessage()!=null?e.getMessage():""));
            }

            String response = 	"RTSP/1.0 "+status+"\r\n" +
                    "Server: "+SERVER_NAME+"\r\n" +
                    (seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
                    "Content-Length: " + content.length() + "\r\n" +
                    attributes +
                    "\r\n" +
                    content;

            Log.d(TAG,response.replace("\r", ""));

            output.write(response.getBytes());
        }
    }

    public class LocalBinder extends Binder {
        public RtspServerService getService() {
            return RtspServerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
