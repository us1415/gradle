/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.mirror

import org.gradle.test.fixtures.file.CleanupTestDirectory
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Subject

@CleanupTestDirectory
class FileSystemCacheTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider()

    @Subject
    private FileSystemCache fs = new FileSystemCache()

    def "regular files can be added"() {
        def regularFile = temporaryFolder.createFile("this/is/a/file.txt")
        def regularFile2 = temporaryFolder.createFile("this/is/another/file.txt")

        when:
        fs.addNode(regularFile)
        fs.addNode(regularFile2)
        fs.addNode(regularFile2)
        def folder = fs[temporaryFolder.file("this")]
        def nodes = getContents(folder)

        then:
        nodes.containsAll([regularFile.absolutePath, regularFile2.absolutePath])
        nodes.size == 4 + 2
    }

    def "dirs can be added"() {
        def dir1 = temporaryFolder.createDir("this/is/a/dir")
        def dir2 = temporaryFolder.createFile("this/is/another/dir")
        def dirInSubDir = temporaryFolder.createFile("this/is/dir/in/sub/dir")

        when:
        fs.addNode(dir1)
        fs.addNode(dir2)
        fs.addNode(dirInSubDir)
        def folder = fs[temporaryFolder.file("this")]
        def nodes = getContents(folder)

        then:
        nodes.size == 10
    }

    def "dir with contents can be added"() {
        def dir = temporaryFolder.createDir("this/is/a/dir")
        def firstFile = dir.file("file/in/dir/some.txt")
        firstFile << "First file"
        def secondFile = dir.file("file/in/another/dir/text.txt")
        secondFile << "Second file"

        when:
        def dirNode = fs.addTree(dir)
        def nodes = getContents(dirNode)

        then:
        nodes.size == 2 + 1 + 3 + 2
        nodes.containsAll([firstFile.absolutePath, secondFile.absolutePath])
    }

    List<String> getContents(FileSystemNode root) {
        def nodes = []
        root.visit(new FileSystemNode.Visitor() {
            @Override
            void visitNode(FileSystemNode node) {
                println "Node: ${node.path}"
                nodes << node.path
            }
        })
        nodes
    }
}
