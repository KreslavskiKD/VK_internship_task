package com.example.vk_video_loader

import android.util.Log
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.SocketFactory


/**
 * **Restricted Socket Factory**
 *
 *
 * OkHttp buffers to the network interface but the network interface's default
 * buffer size is sometimes set very high e.g. 512Kb which makes tracking
 * upload progress impossible as the upload content is sitting in the network
 * interface buffer waiting to be transmitted.
 * Re: https://github.com/square/okhttp/issues/1078
 *
 *
 * So here, we create socket factory that forces all sockets to have a restricted
 * send buffer size. So that further down the chain in OkHttps' RequestBody we can
 * track the actual progress to the nearest [mSendBufferSize] unit.
 *
 *
 * Example usage with OkHttpClient 2.x:
 * <pre>
 * okHttpClient.setSocketFactory(new RestrictedSocketFactory(16 * 1024));
</pre> *
 *
 *
 * Example usage with OkHttpClient 3.x:
 * <pre>
 * okHttpClientBuilder.socketFactory(new RestrictedSocketFactory(16 * 1024))
</pre> *
 *
 *
 * Created by Simon Lightfoot <simon></simon>@devangels.london> on 04/04/2016.
 */

class RestrictedSocketFactory(private val mSendBufferSize: Int) : SocketFactory() {
    @Throws(IOException::class)
    override fun createSocket(): Socket {
        return updateSendBufferSize(Socket())
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket {
        return updateSendBufferSize(Socket(host, port))
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        return updateSendBufferSize(Socket(host, port, localHost, localPort))
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        return updateSendBufferSize(Socket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        return updateSendBufferSize(Socket(address, port, localAddress, localPort))
    }

    @Throws(IOException::class)
    private fun updateSendBufferSize(socket: Socket): Socket {
        socket.sendBufferSize = mSendBufferSize
        return socket
    }

    companion object {
        private val TAG = RestrictedSocketFactory::class.java.simpleName
    }

    init {
        try {
            val socket = Socket()
            Log.w(TAG, "Changing SO_SNDBUF on new sockets from ${socket.sendBufferSize} to $mSendBufferSize.")
        } catch (e: SocketException) {
            //
        }
    }
}