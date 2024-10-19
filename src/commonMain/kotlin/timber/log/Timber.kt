package timber.log

import org.jetbrains.annotations.NonNls
import java.util.Collections
import java.util.Collections.unmodifiableList

/** Logging for lazy people. */
object Timber : Tree {
    /** Log a verbose message with optional format args. */
    override fun v(@NonNls message: String?, vararg args: Any?) {
        forEachTree { v(message, *args) }
    }

    /** Log a verbose exception and a message with optional format args. */
    override fun v(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
        forEachTree { v(t, message, *args) }
    }

    /** Log a verbose exception. */
    override fun v(t: Throwable?) {
        forEachTree { v(t) }
    }

    /** Log a debug message with optional format args. */
    override fun d(@NonNls message: String?, vararg args: Any?) {
        forEachTree { d(message, *args) }
    }

    /** Log a debug exception and a message with optional format args. */
    override fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
        forEachTree { d(t, message, *args) }
    }

    /** Log a debug exception. */
    override fun d(t: Throwable?) {
        forEachTree { d(t) }
    }

    /** Log an info message with optional format args. */
    override fun i(@NonNls message: String?, vararg args: Any?) {
        forEachTree { i(message, *args) }
    }

    /** Log an info exception and a message with optional format args. */
    override fun i(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
        forEachTree { i(t, message, *args) }
    }

    /** Log an info exception. */
    override fun i(t: Throwable?) {
        forEachTree { i(t) }
    }

    /** Log a warning message with optional format args. */
    override fun w(@NonNls message: String?, vararg args: Any?) {
        forEachTree { w(message, *args) }
    }

    /** Log a warning exception and a message with optional format args. */
    override fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
        forEachTree { w(t, message, *args) }
    }

    /** Log a warning exception. */
    override fun w(t: Throwable?) {
        forEachTree { w(t) }
    }

    /** Log an error message with optional format args. */
    override fun e(@NonNls message: String?, vararg args: Any?) {
        forEachTree { e(message, *args) }
    }

    /** Log an error exception and a message with optional format args. */
    override fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
        forEachTree { e(t, message, *args) }
    }

    /** Log an error exception. */
    override fun e(t: Throwable?) {
        forEachTree { e(t) }
    }

    /** Log an assert message with optional format args. */
    override fun wtf(@NonNls message: String?, vararg args: Any?) {
        forEachTree { wtf(message, *args) }
    }

    /** Log an assert exception and a message with optional format args. */
    override fun wtf(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
        forEachTree { wtf(t, message, *args) }
    }

    /** Log an assert exception. */
    override fun wtf(t: Throwable?) {
        forEachTree { wtf(t) }
    }

    /** Log at `priority` a message with optional format args. */
    override fun log(priority: Int, @NonNls message: String?, vararg args: Any?) {
        forEachTree { log(priority, message, *args) }
    }

    /** Log at `priority` an exception and a message with optional format args. */
    override fun log(priority: Int, t: Throwable?, @NonNls message: String?, vararg args: Any?) {
        forEachTree { log(priority, t, message, *args) }
    }

    /** Log at `priority` an exception. */
    override fun log(priority: Int, t: Throwable?) {
        forEachTree { log(priority, t) }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean = true

    /** Set a one-time tag for use on the next logging call. */
    override fun tag(tag: String): Tree {
        treeArray.filterIsInstance<TaggableTree>().forEach { it.explicitTag.set(tag) }
        return this
    }

    /** Add a new logging tree. */
    fun plant(tree: Tree) {
        require(tree !== this) { "Cannot plant Timber into itself." }
        synchronized(trees) {
            trees.add(tree)
            treeArray = trees.toTypedArray()
        }
    }

    /** Adds new logging trees. */
    fun plant(vararg trees: Tree) {
        for (tree in trees) {
            requireNotNull(tree) { "trees contained null" }
            require(tree !== this) { "Cannot plant Timber into itself." }
        }
        synchronized(this.trees) {
            Collections.addAll(this.trees, *trees)
            treeArray = this.trees.toTypedArray()
        }
    }

    /** Remove a planted tree. */
    fun uproot(tree: Tree) {
        synchronized(trees) {
            require(trees.remove(tree)) { "Cannot uproot tree which is not planted: $tree" }
            treeArray = trees.toTypedArray()
        }
    }

    /** Remove all planted trees. */
    fun uprootAll() {
        synchronized(trees) {
            trees.clear()
            treeArray = emptyArray()
        }
    }

    /** Return a copy of all planted [trees][Tree]. */
    fun forest(): List<Tree> {
        synchronized(trees) {
            return unmodifiableList(trees.toList())
        }
    }

    @get:[JvmStatic JvmName("treeCount")]
    val treeCount get() = treeArray.size

    // Both fields guarded by 'trees'.
    private val trees = ArrayList<Tree>()

    @Volatile
    private var treeArray = emptyArray<Tree>()

    private fun forEachTree(call: Tree.() -> Unit) {
        treeArray.forEach { it.call() }
    }
}
