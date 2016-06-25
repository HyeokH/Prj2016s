/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
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

package com.example.prj2016s.RtspServer.lib_spy;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

import com.example.prj2016s.R;

import net.majorkernelpanic.streaming.rtp.*;
import com.example.prj2016s.etc.CallbackEvent;
import com.example.prj2016s.etc.EventRegistration;

/**
 * 
 *   RFC 3984.
 *   
 *   H.264 streaming over RTP.
 *   
 *   Must be fed with an InputStream containing H.264 NAL units preceded by their length (4 bytes).
 *   The stream must start with mpeg4 or 3gpp header, it will be skipped.
 *   
 */
public class H264Packetizer extends AbstractPacketizer implements Runnable {

	public final static String TAG = "H264Packetizer";

	private final static int MAXPACKETSIZE = 1400;
	
	private Thread t = null;
	private int naluLength = 0;
	private long delay = 0, oldtime = 0;
	private Statistics stats = new Statistics();
	private byte[] sps = null, pps = null;
	private int count = 0;
	byte[] is2 = null;
	private int is2_start = 0;

	private int repeat = 3;
	private EventRegistration mEventRegistration = null;
	
	
	public H264Packetizer() throws IOException {
		super();
		socket.setClockFrequency(90000);
	}

	public void setEventRegistration(EventRegistration nER) {
		mEventRegistration = nER;
	};
	public void start() throws IOException {
		Log.e(TAG, "H264Packetizer start()");
		if (t == null) {
			is2 = new byte[100000];
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() throws IOException {
		Log.e(TAG, "H264Packetizer stop()");
		if (t != null) {
			is2 = null;
			t.interrupt();
			try {
				t.join(1000);
			} catch (InterruptedException e) {}
			t = null;
		}
	}

	public void setStreamParameters(byte[] pps, byte[] sps) {
		this.pps = pps;
		this.sps = sps;
	}	
	
	public void run() {
		long duration = 0, delta2 = 0;
		boolean sw = true;
		Log.d(TAG, "H264 packetizer started !");
		stats.reset();
		count = 0;

		Log.d(TAG, "skip mp4 header !");

		while (!Thread.interrupted()) {
			sw = true;
			Log.d(TAG, "H264 packetizer restarted !");
		try {
			my_fill(is2, 0, 4);
/*
			if (is.markSupported()) {
				Log.d(TAG, "is.markSupported() = true");
				is.mark(3000000);
			}
			*/

			while (!Thread.interrupted() && sw) {

				oldtime = System.nanoTime();
				// We read a NAL units from the input stream and we send them
				is2_start = 0;
				sw = my_send();
				send();
				// We measure how long it took to receive NAL units from the phone
				duration = System.nanoTime() - oldtime;

				// We regulary send RTSP Sender Report to the decoder
				/*
				delta += duration/1000000;
				if (intervalBetweenReports>0) {
					if (delta>=intervalBetweenReports) {
						// We send a Sender Report
						report.send(oldtime+duration,(ts/100)*90/10000);
					}
				}*/

				// Every 5 secondes, we send two packets containing NALU type 7 (sps) and 8 (pps)
				// Those should allow the H264 stream to be decoded even if no SDP was sent to the decoder.				
				delta2 += duration / 1000000;
				if (delta2 > 5000) {
					delta2 = 0;
					if (sps != null) {
						buffer = socket.requestBuffer();
						socket.markNextPacket();
						socket.updateTimestamp(ts);
						System.arraycopy(sps, 0, buffer, rtphl, sps.length);
						super.send(rtphl + sps.length);
					}
					if (pps != null) {
						buffer = socket.requestBuffer();
						socket.updateTimestamp(ts);
						socket.markNextPacket();
						System.arraycopy(pps, 0, buffer, rtphl, pps.length);
						super.send(rtphl + pps.length);
					}
				}

				stats.push(duration);
				// Computes the average duration of a NAL unit
				delay = stats.average();
				//Log.d(TAG,"duration: "+duration/1000000+" delay: "+delay/1000000);

			}
		} catch (IOException e) {
		} catch (InterruptedException e) {
		}

		Log.d(TAG, "H264 packetizer stopped !");

		if (mEventRegistration != null) {
			Log.d(TAG, "mEventRegistration != null");
			mEventRegistration.doWork();
			if (is == null) break;
		}
	}
	}

	private boolean my_send() throws IOException {
		//Log.d(TAG,"my_send started !");
		my_fill(is2, 0, 4);
		naluLength = 0;
		try {
			while (!Thread.interrupted()) {
				if ((int)(is2[naluLength]&0xFF) == 0 && (int)(is2[naluLength+1]&0xFF) == 0 && (int)(is2[naluLength+2]&0xFF) == 0 && (int)(is2[naluLength+3]&0xFF) == 1) {
					break;
				}
					if (is.read(is2, naluLength + 4, 1) < 0) {
						naluLength += 4;
						Log.e(TAG, "End of is");
						return false;
					/*
					if (repeat < 0) return false;
					else return true;
					*/
					}

				naluLength++;
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Exception : " + e.getMessage());
		}
		return true;
	}
	/**
	 * Reads a NAL unit in the FIFO and sends it.
	 * If it is too big, we split it in FU-A units (RFC 3984).
	 */
	private void send() throws IOException, InterruptedException {
		int sum = 1, len = 0, type;
		byte[] header = new byte[5];

		fill(header, 0, 1);
		header[0] = header[1] = header[2] = 0;
		header[3] = 1;
		header[4] = is2[0];
		// Parses the NAL unit type
		type = header[4]&0x1F;
		Log.d(TAG, "naluLength : " + naluLength + "  type : " + type);
		
		// The stream already contains NAL unit type 7 or 8, we don't need 
		// to add them to the stream ourselves
		if (type == 7 || type == 8) {
			count++;
			if (count>4) {
				sps = null;
				pps = null;
			}
		}

		// Updates the timestamp
		ts += delay;

		//Log.d(TAG,"- Nal unit length: " + naluLength + " delay: "+delay/1000000+" type: "+type);

		// Small NAL unit => Single NAL unit 
		if (naluLength<=MAXPACKETSIZE-rtphl-2) {
			buffer = socket.requestBuffer();
			buffer[rtphl] = header[4];
			len = fill(buffer, rtphl+1,  naluLength-1);
			socket.updateTimestamp(ts);
			socket.markNextPacket();
			super.send(naluLength+rtphl);
			//Log.d(TAG,"----- Single NAL unit - len:"+len+" delay: "+delay);
		}
		// Large NAL unit => Split nal unit 
		else {

			// Set FU-A header
			header[1] = (byte) (header[4] & 0x1F);  // FU header type
			header[1] += 0x80; // Start bit
			// Set FU-A indicator
			header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
			header[0] += 28;

			while (sum < naluLength) {
				buffer = socket.requestBuffer();
				buffer[rtphl] = header[0];
				buffer[rtphl+1] = header[1];
				socket.updateTimestamp(ts);
				if ((len = fill(buffer, rtphl+2,  naluLength-sum > MAXPACKETSIZE-rtphl-2 ? MAXPACKETSIZE-rtphl-2 : naluLength-sum  ))<0) return; sum += len;
				// Last packet before next NAL
				if (sum >= naluLength) {
					// End bit on
					buffer[rtphl+1] += 0x40;
					socket.markNextPacket();
				}
				super.send(len+rtphl+2);
				// Switch start bit
				header[1] = (byte) (header[1] & 0x7F); 
				//Log.d(TAG,"----- FU-A unit, sum:"+sum);
			}
		}
	}

	private int fill(byte[] buffer, int offset,int length) throws IOException {
		int sum = 0, len;

		for (sum=0;sum<length;sum++) {
			buffer[sum+offset] = is2[is2_start];
			is2_start++;
		}
		return sum;
	}

	private int my_fill(byte[] buffer, int offset,int length) throws IOException {
		int sum = 0, len;

		while (sum<length) {
			len = is.read(buffer, offset+sum, length-sum);
			if (len<0) {
				throw new IOException("End of is stream");
			}
			else sum+=len;
		}
		return sum;
	}

	private void resync() throws IOException {
		byte[] header = new byte[5];
		int type;

		Log.e(TAG,"Packetizer out of sync ! Let's try to fix that...");
		
		while (true) {

			header[0] = header[1];
			header[1] = header[2];
			header[2] = header[3];
			header[3] = header[4];
			header[4] = (byte) is.read();

			type = header[4]&0x1F;

			if (type == 5 || type == 1) {
				naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
				if (naluLength>0 && naluLength<100000) {
					oldtime = System.nanoTime();
					Log.e(TAG,"A NAL unit may have been found in the bit stream !");
					break;
				}
			}

		}

	}

}