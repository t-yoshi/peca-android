package org.peercast.core

import okio.Buffer
import org.junit.Assert
import org.junit.Test
import timber.log.Timber

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class BbsReaderUnitTest {

    init {
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println("$tag $message")
                t?.printStackTrace()
            }
        })
    }


    @Test
    fun test1() {
        //val fdqn = "hibino.ddo.jp";        val category = "bbs/peca";        val id = "1586620166";        val board_num = ""
        //val fdqn = ""; val category = "game"; val board_num = "48946"; val id = "1586620512"
        val fdqn = "bbs.jpnkn.com";
        val category = "inshun";
        val board_num = "";
        val id = "1586690858"

        val r = org.peercast.core.ui.yt.createBbsClient(fdqn, category, board_num)

        val b = Buffer()
        //r.boardCgi().toJson(b)
        r.threadCgi(id, 55).toJson(b)

        b.writeTo(System.out)

    }

    @Test
    fun addition_isCorrect() {
        Assert.assertEquals(4, 2 + 2.toLong())
    }
}