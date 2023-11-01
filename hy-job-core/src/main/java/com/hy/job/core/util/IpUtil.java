package com.hy.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * @Author: HY
 * @Date: 2023-10-12-14:25
 * @Description:ip解析工具
 */
public class IpUtil {
    private static final Logger logger = LoggerFactory.getLogger(IpUtil.class);

    private static final String ANYHOST_VALUE = "0.0.0.0";
    private static final String LOCALHOST_VALUE = "127.0.0.1";

    //用于验证获取的 IP 地址是否符合 IPv4 地址的格式，以确定是否是有效的地址
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");



    private static volatile InetAddress LOCAL_ADDRESS = null;

    // ---------------------- valid ----------------------

    /**
     * 判断地址是否有效
     */
    private static InetAddress toValidAddress(InetAddress address){
        if(address instanceof Inet6Address){
            Inet6Address v6Address = (Inet6Address) address;
            if(isPreferIPV6Address()){
                return normalizeV6Address(v6Address);
            }
        }
        if(isValidV4Address(address)){
            return address;
        }
        return null;
    }

    //智能地选择 IPv4 或 IPv6 地址
    private static boolean isPreferIPV6Address(){
        return Boolean.getBoolean("java.net.preferIPv6Addresses");
    }

    /**
     * 判断地址是不是一个有效的ipv4地址
     * @param address
     * @return
     */
    private static boolean isValidV4Address(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        boolean result = (name != null
                && IP_PATTERN.matcher(name).matches()
                && !ANYHOST_VALUE.equals(name)
                && !LOCALHOST_VALUE.equals(name));
        return result;
    }


    /**
     * 将范围名称转换为范围标识符，以使地址变得更标准化，以便在网络通信中使用
     * @param address
     * @return
     */
    private static InetAddress normalizeV6Address(Inet6Address address){
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf('%');
        if(i > 0){
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
            }catch (UnknownHostException e){
                logger.debug("Unknown IPV6 address: ", e);
            }
        }
        return address;
    }

    /**
     * 尽力获取本地机器的有效网络地址，以便用于网络通信中。如果获取到有效地址，就返回它；如果没有获取到，就返回null，并记录相应的错误日志。
     */
    private static InetAddress getLocalAddress0(){
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            InetAddress addressItem = toValidAddress(localAddress);
            if(addressItem != null){
                return addressItem;
            }
        }catch (Throwable e){
            logger.error(e.getMessage(),e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (null == interfaces) {
                return localAddress;
            }
            while (interfaces.hasMoreElements()) {
                try {
                    NetworkInterface network = interfaces.nextElement();
                    if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
                        continue;
                    }
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            InetAddress addressItem = toValidAddress(addresses.nextElement());
                            if (addressItem != null) {
                                try {
                                    if(addressItem.isReachable(100)){
                                        return addressItem;
                                    }
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        } catch (Throwable e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        return localAddress;
    }

    // ---------------------- tool ----------------------
    /**
     * 从本地网络地址中找到一个有效的地址
     */
    private static InetAddress getLocalAddress(){
        if(LOCAL_ADDRESS != null){
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }


    /**
     * 得到ip地址
     */
    public static String getIp(){
        return getLocalAddress().getHostAddress();
    }


    /**
     *把ip和port转换为ip：port形式
     */

    public static String getIpPort(int port){
        String ip = getIp();
        return getIpPort(ip,port);
    }

    public static String getIpPort(String ip,int port){
        if(ip == null){
            return null;
        }
        return ip.concat(":").concat(String.valueOf(port));
    }

    public static Object[] parseIpPort(String address){
        String[] array = address.split(":");

        String host = array[0];
        int port = Integer.parseInt(array[1]);

        return new Object[]{host,port};
    }

}
