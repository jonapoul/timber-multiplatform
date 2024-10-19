package timber.log

import android.os.Build
import android.util.Log
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLog.LogItem
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TimberTest {
    @Before
    @After
    fun setUpAndTearDown() {
        Timber.uprootAll()
    }

    // NOTE: This test references the line number. Keep it at the top so it does not change.
    @Test
    fun debugTreeCanAlterCreatedTag() {
        Timber.plant(object : AndroidDebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String {
                return super.createStackElementTag(element) + ':'.toString() + element.lineNumber
            }
        })

        Timber.d("Test")

        assertLog()
            .hasDebugMessage("TimberTest:40", "Test")
            .hasNoMoreMessages()
    }

    @Test
    fun recursion() {
        val timber: Tree = Timber

        assertThrows(IllegalArgumentException::class.java) {
            Timber.plant(timber)
        }

        assertThrows(IllegalArgumentException::class.java) {
            Timber.plant(*arrayOf(timber))
        }
    }

    @Test
    fun treeCount() {
        // inserts trees and checks if the amount of returned trees matches.
        assertEquals(expected = 0, actual = Timber.treeCount)
        for (i in 1 until 50) {
            Timber.plant(AndroidDebugTree())
            assertEquals(expected = i, actual = Timber.treeCount)
        }
        Timber.uprootAll()
        assertEquals(expected = 0, actual = Timber.treeCount)
    }

    @Test
    fun forestReturnsAllPlanted() {
        val tree1 = AndroidDebugTree()
        val tree2 = AndroidDebugTree()
        Timber.plant(tree1)
        Timber.plant(tree2)

        assertEquals(expected = listOf(tree1, tree2), actual = Timber.forest())
    }

    @Test
    fun forestReturnsAllTreesPlanted() {
        val tree1 = AndroidDebugTree()
        val tree2 = AndroidDebugTree()
        Timber.plant(tree1, tree2)

        assertEquals(expected = listOf(tree1, tree2), actual = Timber.forest())
    }

    @Test
    fun uprootThrowsIfMissing() {
        assertThrows(IllegalArgumentException::class.java) {
            Timber.uproot(AndroidDebugTree())
        }
    }

    @Test
    fun uprootRemovesTree() {
        val tree1 = AndroidDebugTree()
        val tree2 = AndroidDebugTree()
        Timber.plant(tree1)
        Timber.plant(tree2)
        Timber.d("First")
        Timber.uproot(tree1)
        Timber.d("Second")

        assertLog()
            .hasDebugMessage("TimberTest", "First")
            .hasDebugMessage("TimberTest", "First")
            .hasDebugMessage("TimberTest", "Second")
            .hasNoMoreMessages()
    }

    @Test
    fun uprootAllRemovesAll() {
        val tree1 = AndroidDebugTree()
        val tree2 = AndroidDebugTree()
        Timber.plant(tree1)
        Timber.plant(tree2)
        Timber.d("First")
        Timber.uprootAll()
        Timber.d("Second")

        assertLog()
            .hasDebugMessage("TimberTest", "First")
            .hasDebugMessage("TimberTest", "First")
            .hasNoMoreMessages()
    }

    @Test
    fun noArgsDoesNotFormat() {
        Timber.plant(AndroidDebugTree())
        Timber.d("te%st")

        assertLog()
            .hasDebugMessage("TimberTest", "te%st")
            .hasNoMoreMessages()
    }

    @Test
    fun debugTreeTagGeneration() {
        Timber.plant(AndroidDebugTree())
        Timber.d("Hello, world!")

        assertLog()
            .hasDebugMessage("TimberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    internal inner class ThisIsAReallyLongClassName {
        fun run() {
            Timber.d("Hello, world!")
        }
    }

    @Config(sdk = [25])
    @Test
    fun debugTreeTagTruncation() {
        Timber.plant(AndroidDebugTree())

        ThisIsAReallyLongClassName().run()

        assertLog()
            .hasDebugMessage("TimberTest\$ThisIsAReall", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Config(sdk = [26])
    @Test
    fun debugTreeTagNoTruncation() {
        Timber.plant(AndroidDebugTree())

        ThisIsAReallyLongClassName().run()

        assertLog()
            .hasDebugMessage("TimberTest\$ThisIsAReallyLongClassName", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda") // Lambdas != anonymous classes.
    @Test
    fun debugTreeTagGenerationStripsAnonymousClassMarker() {
        Timber.plant(AndroidDebugTree())
        object : Runnable {
            override fun run() {
                Timber.d("Hello, world!")

                object : Runnable {
                    override fun run() {
                        Timber.d("Hello, world!")
                    }
                }.run()
            }
        }.run()

        assertLog()
            .hasDebugMessage("TimberTest\$debugTreeTag", "Hello, world!")
            .hasDebugMessage("TimberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda") // Lambdas != anonymous classes.
    @Test
    fun debugTreeTagGenerationStripsAnonymousClassMarkerWithInnerSAMLambda() {
        Timber.plant(AndroidDebugTree())
        object : Runnable {
            override fun run() {
                Timber.d("Hello, world!")

                Runnable { Timber.d("Hello, world!") }.run()
            }
        }.run()

        assertLog()
            .hasDebugMessage("TimberTest\$debugTreeTag", "Hello, world!")
            .hasDebugMessage("TimberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda") // Lambdas != anonymous classes.
    @Test
    fun debugTreeTagGenerationStripsAnonymousClassMarkerWithOuterSAMLambda() {
        Timber.plant(AndroidDebugTree())

        Runnable {
            Timber.d("Hello, world!")

            object : Runnable {
                override fun run() {
                    Timber.d("Hello, world!")
                }
            }.run()
        }.run()

        assertLog()
            .hasDebugMessage("TimberTest", "Hello, world!")
            .hasDebugMessage("TimberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    // NOTE: this will fail on some future version of Kotlin when lambdas are compiled using invokedynamic
    // Fix will be to expect the tag to be "TimberTest" as opposed to "TimberTest\$debugTreeTag"
    @Test
    fun debugTreeTagGenerationStripsAnonymousLambdaClassMarker() {
        Timber.plant(AndroidDebugTree())

        val outer = {
            Timber.d("Hello, world!")

            val inner = {
                Timber.d("Hello, world!")
            }

            inner()
        }

        outer()

        assertLog()
            .hasDebugMessage("TimberTest\$debugTreeTag", "Hello, world!")
            .hasDebugMessage("TimberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Test
    fun debugTreeTagGenerationForSAMLambdasUsesClassName() {
        Timber.plant(AndroidDebugTree())

        Runnable {
            Timber.d("Hello, world!")

            Runnable {
                Timber.d("Hello, world!")
            }.run()
        }.run()

        assertLog()
            .hasDebugMessage("TimberTest", "Hello, world!")
            .hasDebugMessage("TimberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    private class ClassNameThatIsReallyReallyReallyLong {
        init {
            Timber.i("Hello, world!")
        }
    }

    @Test
    fun debugTreeGeneratedTagIsLoggable() {
        Timber.plant(object : AndroidDebugTree() {
            private val MAX_TAG_LENGTH = 23

            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                try {
                    assertTrue(Log.isLoggable(tag, priority))
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        assertTrue(tag!!.length <= MAX_TAG_LENGTH)
                    }
                } catch (e: IllegalArgumentException) {
                    fail(e.message)
                }

                super.log(priority, tag, message, t)
            }
        })
        ClassNameThatIsReallyReallyReallyLong()
        assertLog()
            .hasInfoMessage("TimberTest\$ClassNameTha", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Test
    fun debugTreeCustomTag() {
        Timber.plant(AndroidDebugTree())
        Timber.tag("Custom").d("Hello, world!")

        assertLog()
            .hasDebugMessage("Custom", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Test
    fun messageWithException() {
        Timber.plant(AndroidDebugTree())
        val datThrowable = truncatedThrowable(NullPointerException::class.java)
        Timber.e(datThrowable, "OMFG!")

        assertExceptionLogged(Log.ERROR, "OMFG!", "java.lang.NullPointerException")
    }

    @Test
    fun exceptionOnly() {
        Timber.plant(AndroidDebugTree())

        Timber.v(truncatedThrowable(IllegalArgumentException::class.java))
        assertExceptionLogged(
            Log.VERBOSE,
            null,
            "java.lang.IllegalArgumentException",
            "TimberTest",
            0
        )

        Timber.i(truncatedThrowable(NullPointerException::class.java))
        assertExceptionLogged(Log.INFO, null, "java.lang.NullPointerException", "TimberTest", 1)

        Timber.d(truncatedThrowable(UnsupportedOperationException::class.java))
        assertExceptionLogged(
            Log.DEBUG, null, "java.lang.UnsupportedOperationException", "TimberTest",
            2
        )

        Timber.w(truncatedThrowable(UnknownHostException::class.java))
        assertExceptionLogged(Log.WARN, null, "java.net.UnknownHostException", "TimberTest", 3)

        Timber.e(truncatedThrowable(ConnectException::class.java))
        assertExceptionLogged(Log.ERROR, null, "java.net.ConnectException", "TimberTest", 4)

        Timber.wtf(truncatedThrowable(AssertionError::class.java))
        assertExceptionLogged(Log.ASSERT, null, "java.lang.AssertionError", "TimberTest", 5)
    }

    @Test
    fun exceptionOnlyCustomTag() {
        Timber.plant(AndroidDebugTree())

        Timber.tag("Custom").v(truncatedThrowable(IllegalArgumentException::class.java))
        assertExceptionLogged(Log.VERBOSE, null, "java.lang.IllegalArgumentException", "Custom", 0)

        Timber.tag("Custom").i(truncatedThrowable(NullPointerException::class.java))
        assertExceptionLogged(Log.INFO, null, "java.lang.NullPointerException", "Custom", 1)

        Timber.tag("Custom").d(truncatedThrowable(UnsupportedOperationException::class.java))
        assertExceptionLogged(
            Log.DEBUG,
            null,
            "java.lang.UnsupportedOperationException",
            "Custom",
            2
        )

        Timber.tag("Custom").w(truncatedThrowable(UnknownHostException::class.java))
        assertExceptionLogged(Log.WARN, null, "java.net.UnknownHostException", "Custom", 3)

        Timber.tag("Custom").e(truncatedThrowable(ConnectException::class.java))
        assertExceptionLogged(Log.ERROR, null, "java.net.ConnectException", "Custom", 4)

        Timber.tag("Custom").wtf(truncatedThrowable(AssertionError::class.java))
        assertExceptionLogged(Log.ASSERT, null, "java.lang.AssertionError", "Custom", 5)
    }

    @Test
    fun exceptionFromSpawnedThread() {
        Timber.plant(AndroidDebugTree())
        val datThrowable = truncatedThrowable(NullPointerException::class.java)
        val latch = CountDownLatch(1)
        object : Thread() {
            override fun run() {
                Timber.e(datThrowable, "OMFG!")
                latch.countDown()
            }
        }.start()
        latch.await()
        assertExceptionLogged(
            Log.ERROR,
            "OMFG!",
            "java.lang.NullPointerException",
            "TimberTest\$exceptionFro"
        )
    }

    @Test
    fun nullMessageWithThrowable() {
        Timber.plant(AndroidDebugTree())
        val datThrowable = truncatedThrowable(NullPointerException::class.java)
        Timber.e(datThrowable, null)

        assertExceptionLogged(Log.ERROR, "", "java.lang.NullPointerException")
    }

    @Test
    fun chunkAcrossNewlinesAndLimit() {
        Timber.plant(AndroidDebugTree())
        Timber.d(
            'a'.repeat(3000) + '\n'.toString() + 'b'.repeat(6000) + '\n'.toString() + 'c'.repeat(
                3000
            )
        )

        assertLog()
            .hasDebugMessage("TimberTest", 'a'.repeat(3000))
            .hasDebugMessage("TimberTest", 'b'.repeat(4000))
            .hasDebugMessage("TimberTest", 'b'.repeat(2000))
            .hasDebugMessage("TimberTest", 'c'.repeat(3000))
            .hasNoMoreMessages()
    }

    @Test
    fun nullMessageWithoutThrowable() {
        Timber.plant(AndroidDebugTree())
        Timber.d(null as String?)

        assertLog().hasNoMoreMessages()
    }

    @Test
    fun logMessageCallback() {
        val logs = ArrayList<String>()
        Timber.plant(object : AndroidDebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                logs.add("$priority $tag $message")
            }
        })

        Timber.v("Verbose")
        Timber.tag("Custom").v("Verbose")
        Timber.d("Debug")
        Timber.tag("Custom").d("Debug")
        Timber.i("Info")
        Timber.tag("Custom").i("Info")
        Timber.w("Warn")
        Timber.tag("Custom").w("Warn")
        Timber.e("Error")
        Timber.tag("Custom").e("Error")
        Timber.wtf("Assert")
        Timber.tag("Custom").wtf("Assert")

        assertEquals(
            actual = logs,
            expected = listOf(
                "2 TimberTest Verbose",
                "2 Custom Verbose",
                "3 TimberTest Debug",
                "3 Custom Debug",
                "4 TimberTest Info",
                "4 Custom Info",
                "5 TimberTest Warn",
                "5 Custom Warn",
                "6 TimberTest Error",
                "6 Custom Error",
                "7 TimberTest Assert",
                "7 Custom Assert"
            ),
        )
    }

    @Test
    fun logAtSpecifiedPriority() {
        Timber.plant(AndroidDebugTree())

        Timber.log(Log.VERBOSE, "Hello, World!")
        Timber.log(Log.DEBUG, "Hello, World!")
        Timber.log(Log.INFO, "Hello, World!")
        Timber.log(Log.WARN, "Hello, World!")
        Timber.log(Log.ERROR, "Hello, World!")
        Timber.log(Log.ASSERT, "Hello, World!")

        assertLog()
            .hasVerboseMessage("TimberTest", "Hello, World!")
            .hasDebugMessage("TimberTest", "Hello, World!")
            .hasInfoMessage("TimberTest", "Hello, World!")
            .hasWarnMessage("TimberTest", "Hello, World!")
            .hasErrorMessage("TimberTest", "Hello, World!")
            .hasAssertMessage("TimberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test
    fun formatting() {
        Timber.plant(AndroidDebugTree())
        Timber.v("Hello, %s!", "World")
        Timber.d("Hello, %s!", "World")
        Timber.i("Hello, %s!", "World")
        Timber.w("Hello, %s!", "World")
        Timber.e("Hello, %s!", "World")
        Timber.wtf("Hello, %s!", "World")

        assertLog()
            .hasVerboseMessage("TimberTest", "Hello, World!")
            .hasDebugMessage("TimberTest", "Hello, World!")
            .hasInfoMessage("TimberTest", "Hello, World!")
            .hasWarnMessage("TimberTest", "Hello, World!")
            .hasErrorMessage("TimberTest", "Hello, World!")
            .hasAssertMessage("TimberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test
    fun isLoggableControlsLogging() {
        Timber.plant(object : AndroidDebugTree() {
            @Suppress("OverridingDeprecatedMember") // Explicitly testing deprecated variant.
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority == Log.INFO
            }
        })
        Timber.v("Hello, World!")
        Timber.d("Hello, World!")
        Timber.i("Hello, World!")
        Timber.w("Hello, World!")
        Timber.e("Hello, World!")
        Timber.wtf("Hello, World!")

        assertLog()
            .hasInfoMessage("TimberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test
    fun isLoggableTagControlsLogging() {
        Timber.plant(object : AndroidDebugTree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return "FILTER" == tag
            }
        })
        Timber.tag("FILTER").v("Hello, World!")
        Timber.d("Hello, World!")
        Timber.i("Hello, World!")
        Timber.w("Hello, World!")
        Timber.e("Hello, World!")
        Timber.wtf("Hello, World!")

        assertLog()
            .hasVerboseMessage("FILTER", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test
    fun logsUnknownHostExceptions() {
        Timber.plant(AndroidDebugTree())
        Timber.e(truncatedThrowable(UnknownHostException::class.java), null)

        assertExceptionLogged(Log.ERROR, "", "UnknownHostException")
    }

    @Test
    fun tagIsClearedWhenNotLoggable() {
        Timber.plant(object : AndroidDebugTree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority >= Log.WARN
            }
        })
        Timber.tag("NotLogged").i("Message not logged")
        Timber.w("Message logged")

        assertLog()
            .hasWarnMessage("TimberTest", "Message logged")
            .hasNoMoreMessages()
    }

    @Test
    fun logsWithCustomFormatter() {
        Timber.plant(object : AndroidDebugTree() {
            override fun formatMessage(message: String, vararg args: Any?): String {
                @Suppress("ImplicitDefaultLocale")
                return String.format("Test formatting: $message", *args)
            }
        })
        Timber.d("Test message logged. %d", 100)

        assertLog()
            .hasDebugMessage("TimberTest", "Test formatting: Test message logged. 100")
    }

    private fun <T : Throwable> truncatedThrowable(throwableClass: Class<T>): T {
        val throwable = throwableClass.newInstance()
        val stackTrace = throwable.stackTrace
        val traceLength = if (stackTrace.size > 5) 5 else stackTrace.size
        throwable.stackTrace = stackTrace.copyOf(traceLength)
        return throwable
    }

    private fun Char.repeat(number: Int) = toString().repeat(number)

    private fun assertExceptionLogged(
        logType: Int,
        message: String?,
        exceptionClassname: String,
        tag: String? = null,
        index: Int = 0
    ) {
        val logs = getLogs()
        assertEquals(expected = index + 1, actual = logs.size)
        val log = logs[index]
        assertEquals(expected = logType, actual = log.type)
        assertEquals(expected = tag ?: "TimberTest", actual = log.tag)

        if (message != null) {
            assertTrue(log.msg.startsWith(message))
        }

        assertTrue(log.msg.contains(exceptionClassname))
        // We use a low-level primitive that Robolectric doesn't populate.
        assertNull(log.throwable)
    }

    private fun assertLog(): LogAssert {
        return LogAssert(getLogs())
    }

    private fun getLogs() = ShadowLog.getLogs().filter { it.tag != ROBOLECTRIC_INSTRUMENTATION_TAG }

    private class LogAssert(private val items: List<LogItem>) {
        private var index = 0

        fun hasVerboseMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.VERBOSE, tag, message)
        }

        fun hasDebugMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.DEBUG, tag, message)
        }

        fun hasInfoMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.INFO, tag, message)
        }

        fun hasWarnMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.WARN, tag, message)
        }

        fun hasErrorMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.ERROR, tag, message)
        }

        fun hasAssertMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.ASSERT, tag, message)
        }

        private fun hasMessage(priority: Int, tag: String, message: String): LogAssert {
            val item = items[index++]
            assertEquals(expected = priority, actual = item.type)
            assertEquals(expected = tag, actual = item.tag)
            assertEquals(expected = message, actual = item.msg)
            return this
        }

        fun hasNoMoreMessages() {
            assertEquals(expected = index, actual = items.size)
        }
    }

    private companion object {
        private const val ROBOLECTRIC_INSTRUMENTATION_TAG = "MonitoringInstr"
    }
}
