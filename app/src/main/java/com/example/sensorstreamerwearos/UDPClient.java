package com.example.sensorstreamerwearos;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPClient implements Runnable {
    private String mip;
    private int mport;
    private DatagramSocket udpSocket;
    private InetAddress serverAddr;
    private int count;
    public UDPClient(int port, String ip){
        mport = port;
        mip = ip;
        count = 0;
    }
    @Override
    public void run() {
        // create the server
        try {
            udpSocket = new DatagramSocket(mport);
            serverAddr = InetAddress.getByName(mip);
            Log.d("Udp", "Trying to send something right now!");
            byte[] buf = ("The String to Send").getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, mport);
            udpSocket.send(packet);
        } catch (SocketException e) {
            Log.e("Udp:", "Socket Error:", e);
        } catch (IOException e) {
            Log.e("Udp Send:", "IO Error:", e);
        }

        // start infinite loop to start sending messages
        while (true){
            Log.d("Udp", "Trying to send something right now, should work!" + String.valueOf(count));
            byte[] buf = ("The String to Send" + String.valueOf(count)).getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, mport);
            try {
                udpSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            SystemClock.sleep(1000);
            count += 1;

        }
    }
}
