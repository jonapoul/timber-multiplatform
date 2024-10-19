package timber.log

abstract class TaggableTree : Tree {
    @get:JvmSynthetic // Hide from public API.
    internal val explicitTag = ThreadLocal<String>()

    @get:JvmSynthetic // Hide from public API.
    internal open val tag: String?
        get() {
            val tag = explicitTag.get()
            if (tag != null) {
                explicitTag.remove()
            }
            return tag
        }

    override fun tag(tag: String): Tree {
        explicitTag.set(tag)
        return this
    }
}
