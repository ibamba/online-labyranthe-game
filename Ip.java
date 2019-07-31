import java.io.*;
import java.net.*;
import java.util.*;

public class Ip{
    public static void main(String[]args){
	try{
	    Enumeration<NetworkInterface>
		listNi=NetworkInterface.getNetworkInterfaces();
	    while(listNi.hasMoreElements()){
		NetworkInterface nic=listNi.nextElement();
		System.out.println("Network Interface :");
		System.out.println(nic.toString());
		Enumeration<InetAddress> listIa=nic.getInetAddresses();
		while(listIa.hasMoreElements()){
		    InetAddress iac=listIa.nextElement();
		    System.out.println("++++++ InetAddress :");
		    System.out.println("++++++ "+iac.toString());
		    if(iac instanceof Inet4Address){
			System.out.println("IPV4");
		    }
		    if(iac.isLoopbackAddress()){
			System.out.println("Loop Back Address");
		    }
		}
	    }
	} catch(Exception e){
	    e.printStackTrace();
	}
    }    
}
