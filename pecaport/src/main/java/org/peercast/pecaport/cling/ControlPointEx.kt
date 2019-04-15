package org.peercast.pecaport.cling

import kotlinx.coroutines.suspendCancellableCoroutine
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.types.ErrorCode
import timber.log.Timber
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/***
 * 失敗したら例外ではなくnullを返す。
 * */
suspend fun <R> ControlPoint.executeAwaitOrNull(factory: ActionCallbackFactory<R>) : R? {
    return try {
        executeAwait(factory)
    } catch (e: ActionException){
        Timber.w("$e")
        null
    }
}
/***
 * javaFutureを変換。失敗したら [ActionException]をスローする。
 * @throws ActionException
 * */
suspend fun <R> ControlPoint.executeAwait(factory: ActionCallbackFactory<R>) = suspendCancellableCoroutine<R> { co ->
    val callbackDelegate = object : ActionCallbackDelegate<R> {
        override fun success(result: R) {
            co.resume(result)
        }

        override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
            co.resumeWithException(ActionException(ErrorCode.ACTION_FAILED, defaultMsg))
        }
    }
    val f = execute(factory.create(callbackDelegate))
    co.invokeOnCancellation {
        f.cancel(false)
    }
    try {
        f.get()
    } catch (e: ExecutionException) {
        co.resumeWithException(e.cause ?: ActionException(ErrorCode.ACTION_FAILED, e.message))
    } catch (e: Throwable) {
        co.resumeWithException(e)
    }
}