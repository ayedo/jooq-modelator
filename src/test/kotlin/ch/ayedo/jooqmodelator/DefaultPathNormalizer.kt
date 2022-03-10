package ch.ayedo.jooqmodelator

import java.nio.file.Path
import java.util.*

class DefaultPathNormalizer(_separator: String) : PathNormalizer {
    private val separator: String

    init {
        separator = _separator
    }

    /**
     * Normalize path string in case win env
     */
    override fun normalize(path: Path): Deque<String> {
        val queue: Deque<String> = LinkedList<String>()
        queue.add(path.fileName.toString())
        var currentParent = path.parent
        while (Objects.nonNull(currentParent)) {
            if (Objects.isNull(currentParent.fileName) && Objects.isNull(currentParent.parent)) {
                queue.add(currentParent.root.toString() + separator)
                break
            }
            queue.add(separator + separator)
            queue.add(currentParent.fileName.toString())
            currentParent = currentParent.parent
        }
        return queue
    }

    /**
     * Write path string
     */
    override fun writePath(queue: Deque<String>): String {
        val builder = StringBuilder()
        while (!queue.isEmpty())
            builder.append(queue.pollLast())
        return builder.toString()
    }

}