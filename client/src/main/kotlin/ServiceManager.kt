import java.io.Closeable
import java.io.PrintStream
import java.util.*
import java.util.concurrent.TimeUnit

class ServiceManager(private val env: String) : Closeable {
    private class ProcessHandle(env: String) : Closeable {

        private val process: Process = env.replace('\\', '/')
            .let { ProcessBuilder("$it/venv/Scripts/python.exe", "$it/main.py").start() }
        private val writer = PrintStream(process.outputStream)
        private val reader = Scanner(process.inputStream)

        fun communicate(request: Request): String {
            writer.run { println(request.value); flush() }
            return reader.nextLine()
        }

        override fun close() {
            communicate(Request.EXIT)
            process.waitFor(1, TimeUnit.SECONDS)
            writer.close(); reader.close()
        }
    }

    private enum class Request(val value: Int) {
        RECOGNIZE(1),
        EXIT(2)
    }

    sealed class RecognizeResult {
        class Succeed(val value: String) : RecognizeResult()
        class Failed(val reason: Reason) : RecognizeResult() {
            enum class Reason {
                NO_IMAGE,
                NO_CODE,
                MANY_CODES
            }
        }
    }

    private var handle: ProcessHandle? = null

    fun isUp(): Boolean {
        return handle != null
    }

    fun up() {
        if (!isUp()) {
            handle = ProcessHandle(env)
        }
    }

    fun down() {
        if (isUp()) {
            handle?.close()
            handle = null
        }
    }

    fun recognize(): RecognizeResult {
        if (!isUp()) throw IllegalStateException("service down")
        return when (val r = handle!!.communicate(Request.RECOGNIZE)) {
            ERR_NO_IMAGE -> RecognizeResult.Failed(RecognizeResult.Failed.Reason.NO_IMAGE)
            ERR_NO_CODE -> RecognizeResult.Failed(RecognizeResult.Failed.Reason.NO_CODE)
            ERR_MANY_CODES -> RecognizeResult.Failed(RecognizeResult.Failed.Reason.MANY_CODES)
            else -> {
                assert(r.startsWith("r:"))
                return RecognizeResult.Succeed(r.substring(2))
            }
        }
    }

    override fun close() {
        down()
    }

    companion object {
        private const val ERR_NO_IMAGE = "e:1"
        private const val ERR_NO_CODE = "e:2"
        private const val ERR_MANY_CODES = "e:3"
    }
}
