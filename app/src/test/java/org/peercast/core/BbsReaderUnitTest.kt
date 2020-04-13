package org.peercast.core

import okio.Buffer
import org.junit.Assert
import org.junit.Test
import org.peercast.core.yt.createBbsReader
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
        val fdqn = "hibino.ddo.jp";
        val category = "bbs/peca";
        val id = "1586620166";
        val board_num = ""
        //val fdqn = ""; val category = "game"; val board_num = "48946"; val id = "1586620512"

        val r = createBbsReader(fdqn, category, board_num)
        //r.boardCgi().toJson().let(::println)
        val b = Buffer()
        r.threadCgi(id, 55).toJson(b)

        b.writeTo(System.out)

    }

    @Test
    fun addition_isCorrect() {
        Assert.assertEquals(4, 2 + 2.toLong())
    }
}