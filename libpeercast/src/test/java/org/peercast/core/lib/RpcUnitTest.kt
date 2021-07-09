package org.peercast.core.lib

import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test
import org.peercast.core.lib.internal.BaseJsonRpcConnection
import org.peercast.core.lib.rpc.ConnectionStatus
import org.peercast.core.lib.rpc.io.JsonRpcException
import org.peercast.core.lib.test.MockJsonRpcConnection


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */

class RpcUnitTest {
    @Test
    fun testVoidCommand() = runBlocking {
        val conn = MockJsonRpcConnection("""
{
  "jsonrpc": "2.0",
  "id": 6412,
  "result": "null"
}
        """.trimIndent())
        PeerCastRpcClient(conn).stopChannel("1")
    }

    @Test
    fun testVersionInfo() = runBlocking {
        val conn = MockJsonRpcConnection("""
{
  "jsonrpc": "2.0",
  "id": 6412,
  "result": {
    "agentName": "PeerCastStation/2.3.6.0",
    "apiVersion": "1.0.0",
    "jsonrpc": "2.0"
  }
}
        """.trimIndent())
        val info = PeerCastRpcClient(conn).getVersionInfo()

        Assert.assertEquals(info.agentName, "PeerCastStation/2.3.6.0")
        Assert.assertEquals(info.apiVersion, "1.0.0")
    }

    @Test
    fun testStatus() = runBlocking {
        val conn = MockJsonRpcConnection("""
{
  "jsonrpc": "2.0",
  "id": 6412,
  "result": {"uptime":58541,
 "isFirewalled":null,
 "globalRelayEndPoint":["110.*.*.*", 7144],
 "globalDirectEndPoint":["110.*.*.*", 7144],
 "localRelayEndPoint": null,
 "localDirectEndPoint":["192.168.0.6", 7144]
 }
}
        """.trimIndent()) //false
        val s = PeerCastRpcClient(conn).getStatus()
        print(s)
        Assert.assertEquals(s.uptime, 58541)
    }

    @Test
    fun testChannelConnections() = runBlocking {
        val s = """
{
  "jsonrpc":	"2.0",
  "id":	1234,
  "result":	[
    {
      "connectionId":	1351461736,
      "type":	"source",
      "status":	"Connected",
      "sendRate":	0.0,
      "recvRate":	64698.2266,
      "protocolName":	"RTMP Source",
      "localRelays":	null,
      "localDirects":	null,
      "contentPosition":	345423,
      "agentName":	"FMLE/3.0 (compatible; FMSc/1.0)",
      "remoteEndPoint":	"127.0.0.1:64329",
      "remoteHostStatus":	[
        "local"
      ],
      "remoteName":	"rtmp://localhost/live/livestream"
    },
    {
      "connectionId":	1724473770,
      "type":	"direct",
      "status":	"Connected",
      "sendRate":	0.0,
      "recvRate":	0.0,
      "protocolName":	"HTTP Direct",
      "localRelays":	null,
      "localDirects":	null,
      "contentPosition":	330616,
      "agentName":	"VLC/2.1.5 LibVLC/2.1.5",
      "remoteEndPoint":	"192.168.0.6:64320",
      "remoteHostStatus":	[
        "local"
      ],
      "remoteName":	"192.168.0.6:64320"
    },
    {
      "connectionId":	919361550,
      "type":	"relay",
      "status":	"Connected",
      "sendRate":	60694.1758,
      "recvRate":	16.5836468,
      "protocolName":	"PCP Relay",
      "localRelays":	0,
      "localDirects":	1,
      "contentPosition":	135065042,
      "agentName":	null,
      "remoteEndPoint":	"***.***.***.***:50475",
      "remoteHostStatus":	[
        "relayFull"
      ],
      "remoteName":	"***.***.***.***:50475"
    }
  ]
}"""
        val conn = MockJsonRpcConnection(s)
        val o = PeerCastRpcClient(conn).getChannelConnections("<channelId>")
        print(o)
        Assert.assertEquals(o[0].connectionId, 1351461736)
        Assert.assertEquals(o[1].agentName, "VLC/2.1.5 LibVLC/2.1.5")
        Assert.assertEquals(o[2].contentPosition, 135065042L)
    }

    @Test
    fun testChannelInfo() = runBlocking {
        val s = """
{
  "jsonrpc":	"2.0",
  "id":	1234,
  "result":	{
    "info":	{
      "name":	"テストch",
      "url":	"",
      "genre":	"",
      "desc":	"",
      "comment":	"",
      "bitrate":	512,
      "contentType":	"FLV",
      "mimeType":	"video/x-flv"
    },
    "track":	{
      "name":	"a",
      "genre":	"b",
      "album":	"c",
      "creator":	"d",
      "url":	"e"
    },
    "yellowPages":	[

    ]
  }
}
        """.trimIndent()
        val conn = MockJsonRpcConnection(s)
        val o = PeerCastRpcClient(conn).getChannelInfo("<channelId>")
        print(o)
        Assert.assertEquals(o.info.name, "テストch")
        Assert.assertEquals(o.track.name, "a")
    }

    @Test
    fun testChannelRelayTree() = runBlocking {
        val s = """
            {
  "jsonrpc":	"2.0",
  "id":	1234,
  "result":	[
    {
      "sessionId":	"F46190087E454DE6977B957D2D74D599",
      "address":	"110.4.130.220",
      "port":	7144,
      "isFirewalled":	false,
      "localRelays":	1,
      "localDirects":	0,
      "isTracker":	false,
      "isRelayFull":	true,
      "isDirectFull":	true,
      "isReceiving":	true,
      "isControlFull":	false,
      "version":	null,
      "versionVP":	null,
      "children":	[
        {
          "sessionId":	"0078CC0C7CF28A909E3AF7C5A30FAED6",
          "address":	"***.***.***.***",
          "port":	7144,
          "isFirewalled":	false,
          "localRelays":	0,
          "localDirects":	1,
          "isTracker":	false,
          "isRelayFull":	false,
          "isDirectFull":	true,
          "isReceiving":	true,
          "isControlFull":	false,
          "version":	1218,
          "versionVP":	27,
          "versionEX":	"IM45",
          "children":	[

          ]
        }
      ]
    }
  ]
}
        """.trimIndent()
        val conn = MockJsonRpcConnection(s)
        val o = PeerCastRpcClient(conn).getChannelRelayTree("<channelId>")
        print(o)
        Assert.assertEquals(o[0].sessionId, "F46190087E454DE6977B957D2D74D599")
        Assert.assertEquals(o[0].children[0].sessionId, "0078CC0C7CF28A909E3AF7C5A30FAED6")

    }

    @Test
    fun testChannelStatus() = runBlocking {
        val s = """
            {
  "jsonrpc":	"2.0",
  "id":	1234,
  "result":	{
    "status":	"Searching",
    "source":	"rtmp://localhost/live/livestream",
    "uptime":	166,
    "localRelays":	0,
    "localDirects":	0,
    "totalRelays":	0,
    "totalDirects":	0,
    "isBroadcasting":	true,
    "isRelayFull":	false,
    "isDirectFull":	null,
    "isReceiving":	false
  }
}
"""
        val conn = MockJsonRpcConnection(s)
        val o = PeerCastRpcClient(conn).getChannelStatus("<channelId>")
        print(o)
        Assert.assertEquals(o.status, ConnectionStatus.Searching)
        Assert.assertEquals(o.uptime, 166)
    }

    @Test
    fun testChannels() = runBlocking {
        val s = """
{
  "jsonrpc":	"2.0",
  "id":	1234,
  "result":	[
    {
      "channelId":	"A0B184CC8F166FD0BCB9618B74A2CD80",
      "status":	{
        "status":	"Searching",
        "source":	"rtmp://localhost/live/livestream",
        "uptime":	3,
        "localRelays":	0,
        "localDirects":	0,
        "totalRelays":	0,
        "totalDirects":	0,
        "isBroadcasting":	true,
        "isRelayFull":	false,
        "isDirectFull":	false,
        "isReceiving":	false
      },
      "info":	{
        "name":	"テストch",
        "url":	"",
        "genre":	"",
        "desc":	"",
        "comment":	"",
        "bitrate":	0,
        "contentType":	"?",
        "mimeType":	"application/octet-stream"
      },
      "track":	{
        "name":	"a",
        "genre":	"b",
        "album":	"c",
        "creator":	"d",
        "url":	"e"
      },
      "yellowPages":	[

      ]
    }
  ]
}
"""
        val conn = MockJsonRpcConnection(s)
        val o = PeerCastRpcClient(conn).getChannels()
        print(o)
        Assert.assertEquals(o[0].channelId, "A0B184CC8F166FD0BCB9618B74A2CD80")
        Assert.assertEquals(o[0].info.name, "テストch")
    }


    @Test
    fun testSettings() = runBlocking {
        val s = """
{
  "jsonrpc":	"2.0",
  "id":	1234,
  "result":	{
    "maxRelays":	2,
    "maxRelaysPerChannel":	1,
    "maxDirects":	2,
    "maxDirectsPerChannel":	0,
    "maxUpstreamRate":	600,
    "maxUpstreamRatePerChannel":	0,
    "channelCleaner":	{
      "mode":	3,
      "inactiveLimit":	60000
    }
  }
}
"""
        val conn = MockJsonRpcConnection(s)
        val o = PeerCastRpcClient(conn).getSettings()
        Assert.assertEquals(o.maxRelays, 2)
    }

    @Test
    fun testError() = runBlocking {
        val s = """
        {
            "error":{"message":"io-exception"},
            "jsonrpc":"2.0"
        }
        
        """.trimIndent()
        val conn = MockJsonRpcConnection(s)
        val e = kotlin.runCatching {
            PeerCastRpcClient(conn).getSettings()
        }.exceptionOrNull()
        println("$e, ${e?.cause}")
        Assert.assertTrue(e is JsonRpcException)
        Assert.assertEquals(e?.message, "io-exception")
    }
    /*
       @Test
       fun testCreateRpcRequest() {
           val s = JsonRpcUtil.createRequest("methodA", "arg1")
           Assert.assertTrue(s.startsWith("""{"jsonrpc":"2.0","method":"methodA","params":["arg1"],"id":"""))
       }
   */
}


