package org.peercast.core.lib

import kotlinx.coroutines.runBlocking
import org.junit.Test


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class YtRpcTest {

/*

    private fun <R> execRpc(request: JsonRpcRequest, resultTypes: Type): JsonRpcResponse<R> {
        with(url.openConnection() as HttpURLConnection) {
            setRequestProperty("X-Requested-With", "XMLHttpRequest")
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 3_000
            readTimeout = 3_000
            outputStream.writer().use {
                it.write(reqAdapter.toJson(request))
            }
            return inputStream.reader().use { r ->
                val t = Types.newParameterizedType(JsonRpcResponse::class.java, resultTypes)
                val resAdapter = moshi.adapter<JsonRpcResponse<R>>( t)
                val s = r.readText()
                println(s)
                 resAdapter.fromJson(s)!!
            }
        }
    }

    var id = 0

    @Test
    fun printVersionInfo(){
        val req = JsonRpcRequest.Builder("getVersionInfo").setId(id++).build()
        val res: JsonRpcResponse<VersionInfo> = execRpc(req, VersionInfo::class.java)
        println("version: " + res.getResultOrNull())
    }

    @Test
    fun printStatus(){
        val req = JsonRpcRequest.Builder("getStatus").setId(id++).build()
        val res: JsonRpcResponse<Status> = execRpc(req, Status::class.java)
        println("status: " + res.getResultOrNull())
    }

    private fun printChannelConnections(channelId : String){
        val t = Types.newParameterizedType(List::class.java, ChannelConnection::class.java)
        val req = JsonRpcRequest.Builder("getChannelConnections").setParams(channelId).setId(id++).build()
        val res: JsonRpcResponse<List<ChannelConnection>> = execRpc(req, t)
        println("   ->")
        res.getResultOrThrow().forEach {
            println("    $it")
        }
    }

    @Test
    fun printChannels(){
        val t = Types.newParameterizedType(List::class.java, Channel::class.java)
        val req = JsonRpcRequest.Builder("getChannels").setId(id++).build()
        val res: JsonRpcResponse<List<Channel>> = execRpc(req, t)
        println("channels: ")
        res.getResultOrThrow().forEach {
            println("  $it")
            printChannelConnections(it.channelId)
        }
    }

    @Test
    fun printSettings(){
        val t = Types.newParameterizedType(List::class.java, Settings::class.java)
        val req = JsonRpcRequest.Builder("getSettings").setId(id++).build()
        val res: JsonRpcResponse<Settings> = execRpc(req, t)
        println("settings: ${res.getResultOrNull()}")
    }

*/
    @Test
    fun testRpc() {
            runBlocking {
                val conn = JsonRpcConnection("localhost", 47145)
                val client = PeerCastRpcClient(conn)
                print(client.getStatus())
            }

//        printVersionInfo()
//        for (id in 0 .. 100) {
//            printStatus()
//            printChannels()
//            println()
//
//            Thread.sleep(5000)
//        }
    }


}

