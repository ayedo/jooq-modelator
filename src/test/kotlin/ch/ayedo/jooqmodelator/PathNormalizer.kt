package ch.ayedo.jooqmodelator

import java.nio.file.Path
import java.util.*

interface PathNormalizer {
    fun normalize(path: Path): Deque<String>
    fun writePath(queue: Deque<String>): String
}