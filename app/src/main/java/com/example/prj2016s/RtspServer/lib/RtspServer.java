/*
 * Copyright (C) 2011-2014 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.example.prj2016s.RtspServer.lib;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.example.prj2016s.Manager.M3u8Manager;
import com.example.prj2016s.Manager.ManagerActivity;
import com.example.prj2016s.Manager.ManagerActivity.*;
import com.example.prj2016s.etc.CallbackEvent;
import com.example.prj2016s.etc.EventRegistration;

import com.example.prj2016s.R;
import com.example.prj2016s.RtspServer.VideoStream;
import com.example.prj2016s.RtspServer.lib_spy.H264Packetizer;

import net.majorkernelpanic.streaming.rtsp.UriParser;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of a subset of the RTSP protocol (RFC 2326).
 * 
 * It allows remote control of an android device cameras & microphone.
 * For each connected client, a Session is instantiated.
 * The Session will start or stop streams according to what the client wants.
 * 
 */
public class RtspServer extends Service {
//	public static String LOCAL_IP = "10.0.2.15";
	public static String LOCAL_IP = "127.0.0.1";

	public final static String TAG = "RtspServer";
	public final static String HHTAG = "HyeokHwa RtspServer";

	/** The server name that will appear in responses. */
	public static String SERVER_NAME = "MajorKernelPanic RTSP Server";

	/** Port used by default. */
	public static final int DEFAULT_RTSP_PORT = 8086;
	public static final int DEFAULT_VIDEO_PORT = 51372;
	public static final int DEFAULT_AUDIO_PORT = 49170;

	public final static int ERROR_BIND_FAILED = 0x00;
	public final static int ERROR_START_FAILED = 0x01;
	public final static int MESSAGE_STREAMING_STARTED = 0X00;
	public final static int MESSAGE_STREAMING_STOPPED = 0X01;
	public final static String KEY_ENABLED = "rtsp_enabled";

	static int MJPEG_TYPE = 98;
	static int FRAME_PERIOD = 1000;
	static int VIDEO_LENGTH = 500;
	static int RTSP_ID = 123456;
	static int DEFAULT_TTL = 1;

	/** Key used in the SharedPreferences for the port used by the RTSP server. */
	public final static String KEY_PORT = "rtsp_port";
	public final static String KEY_IS_AUTO = "is_auto";
	public final static String KEY_BW = "bw";
	public final static String KEY_NAME = "filename";

	final static String CRLF = "\r\n";


	protected SessionBuilder mSessionBuilder;
	protected SharedPreferences mSharedPreferences;
	protected boolean mEnabled = true;	
	protected int mPort = DEFAULT_RTSP_PORT;

	protected WeakHashMap<Session,Object> mSessions = new WeakHashMap<Session,Object>(2);
	
	private RequestListener mListenerThread;
	private final IBinder mBinder = new LocalBinder();
	private boolean mRestart = false;
	private final LinkedList<CallbackListener> mListeners = new LinkedList<CallbackListener>();

    /** Credentials for Basic Auth */
    private String mUsername;
    private String mPassword;

	private int[][] srcPort = new int[10][2];

	int mBwSet = 200;
	boolean mIsAuto = false;

	public RtspServer() {
	}

	/** Be careful: those callbacks won't necessarily be called from the ui thread ! */
	public interface CallbackListener {

		/** Called when an error occurs. */
		void onError(RtspServer server, Exception e, int error);

		/** Called when streaming starts/stops. */
		void onMessage(RtspServer server, int message);
		
	}

	/**
	 * See {@link RtspServer.CallbackListener} to check out what events will be fired once you set up a listener.
	 * @param listener The listener
	 */
	public void addCallbackListener(CallbackListener listener) {
		synchronized (mListeners) {
			if (mListeners.size() > 0) {
				for (CallbackListener cl : mListeners) {
					if (cl == listener) return;
				}
			}
			mListeners.add(listener);
		}
	}

	/**
	 * Removes the listener.
	 * @param listener The listener
	 */
	public void removeCallbackListener(CallbackListener listener) {
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}

	/** Returns the port used by the RTSP server. */
	public int getPort() {
		return mPort;
	}

	/**
	 * Sets the port for the RTSP server to use.
	 * @param port The port
	 */
	public void setPort(int port) {
		Editor editor = mSharedPreferences.edit();
		editor.putString(KEY_PORT, String.valueOf(port));
		editor.commit();
	}

    /**
     * Set Basic authorization to access RTSP Stream
     * @param username username
     * @param password password
     */
    public void setAuthorization(String username, String password)
    {
        mUsername = username;
        mPassword = password;
    }

	/** 
	 * Starts (or restart if needed, if for example the configuration 
	 * of the server has been modified) the RTSP server. 
	 */
	public void start() {
		if (!mEnabled || mRestart) stop();
		if (mEnabled && mListenerThread == null) {
			try {
				mListenerThread = new RequestListener();
			} catch (Exception e) {
				mListenerThread = null;
			}
		}
		mRestart = false;
	}

	/** 
	 * Stops the RTSP server but not the Android Service. 
	 * To stop the Android Service you need to call {@link android.content.Context#stopService(Intent)}; 
	 */
	public void stop() {
		if (mListenerThread != null) {
			try {
				mListenerThread.kill();
				for ( Session session : mSessions.keySet() ) {
				    if ( session != null ) {
				    	if (session.isStreaming()) session.stop();
				    } 
				}
			} catch (Exception e) {
			} finally {
				mListenerThread = null;
			}
		}
	}

	/** Returns whether or not the RTSP server is streaming to some client(s). */
	public boolean isStreaming() {
		for ( Session session : mSessions.keySet() ) {
		    if ( session != null ) {
		    	if (session.isStreaming()) return true;
		    } 
		}
		return false;
	}
	
	public boolean isEnabled() {
		return mEnabled;
	}

	/** Returns the bandwidth consumed by the RTSP server in bits per second. */
	public long getBitrate() {
		long bitrate = 0;
		for ( Session session : mSessions.keySet() ) {
		    if ( session != null ) {
		    	if (session.isStreaming()) bitrate += session.getBitrate();
		    } 
		}
		return bitrate;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onCreate() {

		// Let's restore the state of the service 
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPort = Integer.parseInt(mSharedPreferences.getString(KEY_PORT, String.valueOf(mPort)));
		mEnabled = mSharedPreferences.getBoolean(KEY_ENABLED, mEnabled);

		// If the configuration is modified, the server will adjust
		mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

		start();
	}

	@Override
	public void onDestroy() {
		stop();
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
	}

	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
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

	/** The Binder you obtain when a connection with the Service is established. */
	public class LocalBinder extends Binder {
		public RtspServer getService() {
			return RtspServer.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	protected void postMessage(int id) {
		synchronized (mListeners) {
			if (mListeners.size() > 0) {
				for (CallbackListener cl : mListeners) {
					cl.onMessage(this, id);
				}
			}			
		}
	}
	
	protected void postError(Exception exception, int id) {
		synchronized (mListeners) {
			if (mListeners.size() > 0) {
				for (CallbackListener cl : mListeners) {
					cl.onError(this, exception, id);
				}
			}			
		}
	}

	/** 
	 * By default the RTSP uses {@link UriParser} to parse the URI requested by the client
	 * but you can change that behavior by override this method.
	 * @param uri The uri that the client has requested
	 * @param client The socket associated to the client
	 * @return A proper session
	 */
	protected Session handleRequest(String uri, Socket client) throws IllegalStateException, IOException {
		Session session = UriParser.parse(uri);
		Log.d(HHTAG, "handleRequest 1 : " + uri);
		session.setOrigin(client.getLocalAddress().getHostAddress());
		Log.d(HHTAG, "handleRequest 2 : " + client.getLocalAddress().getHostAddress());
		if (session.getDestination()==null) {
			session.setDestination(client.getInetAddress().getHostAddress());
		}
		return session;
	}
	
	class RequestListener extends Thread implements Runnable {

		private final ServerSocket mServer;

		public RequestListener() throws IOException {
			try {
//				InetAddress addr = InetAddress.getByName("127.0.0.1");
//				mServer = new ServerSocket(mPort,50,addr);
				mServer = new ServerSocket(mPort);
				String address = mServer.getInetAddress().getHostAddress();
				Log.i("Local Socket Address:", address);
				start();
			} catch (BindException e) {
				Log.e(TAG,"Port already in use !");
				postError(e, ERROR_BIND_FAILED);
				throw e;
			}
		}

		public void run() {
			Log.i(TAG,"RTSP server listening on port "+mServer.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					new WorkerThread(mServer.accept()).start();
				} catch (SocketException e) {
					break;
				} catch (IOException e) {
					Log.e(TAG,e.getMessage());
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


	private M3u8Manager man;
	LinkedList<InputStream> downloaded = new LinkedList<InputStream>();
	class DownloadTs extends Thread {
		@Override
		public void run() {
			super.run();
			InputStream temp = null;

			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			mBwSet = Integer.parseInt(mSharedPreferences.getString(KEY_BW, String.valueOf(mBwSet)));
			mIsAuto = mSharedPreferences.getBoolean(KEY_IS_AUTO, mIsAuto);
			String filePath = man.getNext(	mBwSet, mIsAuto, getApplicationContext());
			if (filePath.equals("")){
				temp = null;
			}
			else {
				try {
					temp = new FileInputStream(filePath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			downloaded.add(temp);
		}
	}
	// One thread per client
	class WorkerThread extends Thread implements Runnable {

		private final Socket mClient;
		private final OutputStream mOutput;
		private final BufferedReader mInput;
		private DatagramSocket mRTPsocket;
		private Timer mTimer;
		private TimerTask mTask;

		// Each client has an associated session
		private Session mSession;

		private int imagenb;
		byte[] buf;
		private DatagramPacket senddp;
		private VideoStream video;

		//private MyPacketizer mPacketizer2 = null;
		private H264Packetizer mPacketizer = null;
		private EventRegistration mEventRegistration = null;
//		private AACADTSPacketizer mPacketizer = null;
//		private AACLATMPacketizer mPacketizer = null;

		public WorkerThread(final Socket client) throws IOException {
			mInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
			mOutput = client.getOutputStream();
			mClient = client;

			mSession = new Session();
			mPacketizer = new H264Packetizer();


			try {
				Log.d(TAG, "downloaded.add *2 st");
				mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

				man = new M3u8Manager();
				man.prepare(mSharedPreferences.getString(KEY_NAME, "test.mp4"), getApplicationContext());

				mBwSet = Integer.parseInt(mSharedPreferences.getString(KEY_BW, String.valueOf(mBwSet)));
				mIsAuto = mSharedPreferences.getBoolean(KEY_IS_AUTO, mIsAuto);
				downloaded.add(new FileInputStream(man.getNext(mBwSet, mIsAuto, getApplicationContext())));
				downloaded.add(new FileInputStream(man.getNext(mBwSet, mIsAuto, getApplicationContext())));
				Log.d(TAG, "downloaded.add *2 end");
			} catch (IOException e) {
				Log.d(TAG, e.getMessage());
				e.printStackTrace();
			}

			//mPacketizer2 = new MyPacketizer();
			try {

				Log.d(TAG, "bw : " + mBwSet + " isAuto : " + mIsAuto);
				DownloadTs  hi = new DownloadTs();
				hi.start();
				InputStream fis = downloaded.getFirst();
				downloaded.removeFirst();

				mPacketizer.setInputStream(fis);
				//mPacketizer2.setInputStream(fis);
				mPacketizer.setTimeToLive(DEFAULT_TTL);

				CallbackEvent callbackEvent = new CallbackEvent(){
					@Override
					public void callbackMethod() {
						Log.d(TAG, "callbackMethod() - bw : " + mBwSet + " isAuto : " + mIsAuto);
						DownloadTs  hi = new DownloadTs();
						hi.start();
						InputStream fis = downloaded.getFirst();
						downloaded.removeFirst();

						mPacketizer.setInputStream(fis);
					}
				};
				EventRegistration eventRegistration = new EventRegistration(callbackEvent);
				mPacketizer.setEventRegistration(eventRegistration);
				Log.d(TAG, "bw : " + mBwSet + " isAuto : " + mIsAuto);
			}
			catch (Exception e) {
				Log.e(HHTAG, "mPacketizer : " + e.getMessage());
			}
		}

		public void run() {
			Request request;
			Response response;

			Log.i(HHTAG, "Connection from "+mClient.getInetAddress().getHostAddress());

			while (!Thread.interrupted()) {

				request = null;
				response = null;

				// Parse the request
				try {
					request = Request.parseRequest(mInput);
				} catch (SocketException e) {
					// Client has left
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
					}
					catch (Exception e) {
						// This alerts the main thread that something has gone wrong in this thread
						postError(e, ERROR_START_FAILED);
						Log.e(TAG,e.getMessage()!=null?e.getMessage():"An error occurred");
						e.printStackTrace();
						response = new Response(request);
					}
				}

				// We always send a response
				// The client will receive an "INTERNAL SERVER ERROR" if an exception has been thrown at some point
				try {
					response.send(mOutput);
				} catch (IOException e) {
					Log.e(TAG,"Response was not sent properly");
					break;
				}

			}

			// Streaming stops when client disconnects
			boolean streaming = isStreaming();
			mSession.syncStop();
			if (streaming && !isStreaming()) {
				postMessage(MESSAGE_STREAMING_STOPPED);
			}
			mSession.release();

			try {
				mClient.close();
			} catch (IOException ignore) {}

			Log.i(TAG, "Client disconnected");

		}

		public Response processRequest(Request request) throws IllegalStateException, IOException {
			Response response = new Response(request);

            //Ask for authorization unless this is an OPTIONS request
            if(!isAuthorized(request) && !request.method.equalsIgnoreCase("OPTIONS"))
            {
                response.attributes = "WWW-Authenticate: Basic realm=\""+SERVER_NAME+"\"\r\n";
                response.status = Response.STATUS_UNAUTHORIZED;
            }
            else
            {
			    /* ********************************************************************************** */
			    /* ********************************* Method DESCRIBE ******************************** */
			    /* ********************************************************************************** */
                if (request.method.equalsIgnoreCase("DESCRIBE")) {

						String requestContent = getSessionDescription(mClient.getInetAddress());

						String requestAttributes =
								"Content-Base: rtsp://" + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/\r\n" +
										"Content-Type: application/sdp\r\n";

						response.attributes = requestAttributes;
						response.content = requestContent;

						// If no exception has been thrown, we reply with OK
						response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************* Method OPTIONS ********************************* */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("OPTIONS")) {
                    response.status = Response.STATUS_OK;
                    response.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************** Method SETUP ********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("SETUP")) {

					Pattern p;
					Matcher m;
					int p2, p1, trackId=0;

					p = Pattern.compile("trackId=(\\w+)", Pattern.CASE_INSENSITIVE);
					m = p.matcher(request.uri);
					if (m.find()) trackId = Integer.parseInt(m.group(1));

					p = Pattern.compile("client_port=(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
					m = p.matcher(request.headers.get("transport"));
					if (m.find()) {
						p1 = Integer.parseInt(m.group(1));
						p2 = Integer.parseInt(m.group(2));
						srcPort[trackId][0] = p1;
						srcPort[trackId][1] = p2;
					}
					else {
						p1 = srcPort[trackId][0];
						p2 = srcPort[trackId][1];
					}


					if (mPacketizer != null) {
						Log.e(HHTAG, "mPacketizer.setDestination("+mClient.getInetAddress()+", "+p1+", "+p2+")");
						mPacketizer.setDestination(mClient.getInetAddress(), p1, p2);
					}
					else {
						Log.e(HHTAG, "mPacketizer == null");
					}

					response.attributes = "Transport: RTP/AVP/UDP;" +  "unicast" +
//							";destination=" + mSession.getDestination() +
							";destination=" +  mClient.getLocalAddress().getHostAddress() +
							";client_port=" + p1 + "-" + p2 +
							";server_port=" + DEFAULT_VIDEO_PORT + "-" + (DEFAULT_VIDEO_PORT+1) +
//							";ssrc=" + Integer.toHexString(ssrc) +
//							";mode=play"
							"\r\n" +
							"Session: " + "1185d20035702ca" + "\r\n";
//							"Cache-Control: no-cache\r\n";

					// If no exception has been thrown, we reply with OK
					response.status = Response.STATUS_OK;

                }

                /* ********************************************************************************** */
                /* ********************************** Method PLAY *********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("PLAY")) {
                    String requestAttributes = "RTP-Info: ";
                    //if (mSession.trackExists(0))
                        requestAttributes += "url=rtsp://" + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/trackId=" + 1 + ";seq=0,";
                    //if (mSession.trackExists(1))
                    //    requestAttributes += "url=rtsp://" + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/trackID=" + 1 + ";seq=0,";
                    requestAttributes = requestAttributes.substring(0, requestAttributes.length() - 1) + "\r\n";
					requestAttributes += "Session: " + "1185d20035702ca" + "\r\n";
                    response.attributes = requestAttributes;


					mPacketizer.start();
					//mPacketizer2.start();

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                }

                /* ********************************************************************************** */
                /* ********************************** Method PAUSE ********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("PAUSE")) {
					mPacketizer.stop();
					//mPacketizer2.stop();
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************* Method TEARDOWN ******************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("TEARDOWN")) {
					mPacketizer.stop();
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************* Unknown method ? ******************************* */
                /* ********************************************************************************** */
                else {
                    Log.e(TAG, "Command unknown: " + request);
                    response.status = Response.STATUS_BAD_REQUEST;
                }
            }
			return response;

		}

        /**
         * Check if the request is authorized
         * @param request
         * @return true or false
         */
        private boolean isAuthorized(Request request)
        {
            String auth = request.headers.get("authorization");
            if(mUsername == null || mPassword == null || mUsername.isEmpty())
                return true;

            if(auth != null && !auth.isEmpty())
            {
                String received = auth.substring(auth.lastIndexOf(" ")+1);
                String local = mUsername+":"+mPassword;
                String localEncoded = Base64.encodeToString(local.getBytes(),Base64.NO_WRAP);
                if(localEncoded.equals(received))
                    return true;
            }

            return false;
        }
	}
	public String getSessionDescription(InetAddress clientIP) {
		StringWriter writer1 = new StringWriter();
		StringWriter writer2 = new StringWriter();

		// Write the body first so we can get the size later
		writer2.write("v=0" + CRLF);
		writer2.write("o=HyeokH 1234 1234 IN IP4 " + clientIP + CRLF);
		writer2.write("s=Nonamed" + CRLF);
		writer2.write("t=0 0" + CRLF);
		writer2.write("m=video " + DEFAULT_VIDEO_PORT + " RTP/AVP " + MJPEG_TYPE + CRLF);
		//writer2.write("a=control:streamid=" + RTSP_ID + CRLF);
		//writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
//		writer2.write("a=framesize:" + MJPEG_TYPE + " 1920-1440" + CRLF);
		writer2.write("a=framesize:" + MJPEG_TYPE + " 480-360" + CRLF);
		writer2.write("a=rtpmap:"+ MJPEG_TYPE +" H264/90000" + CRLF);
		//writer2.write("a=fmtp:" + MJPEG_TYPE + " profile-level-id=428016" + CRLF);
		writer2.write("a=control:trackId=1" + CRLF);

		String body = writer2.toString();

		return body;
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
			if ((line = input.readLine())==null) throw new SocketException("Client disconnected");
			matcher = regexMethod.matcher(line);
			matcher.find();
			request.method = matcher.group(1);
			request.uri = matcher.group(2);

			Log.d(HHTAG,request.method+" "+request.uri);

			// Parsing headers of the request
			while ( (line = input.readLine()) != null && line.length()>3 ) {
				matcher = rexegHeader.matcher(line);
				matcher.find();

				Log.d(HHTAG,matcher.group(1).toLowerCase(Locale.US) + ":" + matcher.group(2));
				request.headers.put(matcher.group(1).toLowerCase(Locale.US),matcher.group(2));
			}
			if (line==null) throw new SocketException("Client disconnected");

			// It's not an error, it's just easier to follow what's happening in logcat with the request in red

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

}
