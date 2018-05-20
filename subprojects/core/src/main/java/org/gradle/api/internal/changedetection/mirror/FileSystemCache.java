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

package org.gradle.api.internal.changedetection.mirror;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.UncheckedIOException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

@SuppressWarnings("Since15")
public class FileSystemCache {
    private static final String FILE_PATH_SEPARATORS = File.separatorChar != '/' ? ("/" + File.separator) : File.separator;

    private final FileSystemNode root = new DefaultFileSystemNode(null);

    public FileSystemNode addNode(File file) {
        String[] pathSegments = getPathSegments(file);
        return root.add(pathSegments, 0);
    }

    public FileSystemNode addTree(File file) {
        final FileSystemNode rootDir = addNode(file);
        try {
            Files.walkFileTree(file.toPath(), new FileVisitor<Path>() {
                private FileSystemNode currentPos;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (currentPos == null) {
                        currentPos = rootDir;
                        return FileVisitResult.CONTINUE;
                    }
                    String dirName = dir.getFileName().toString();
                    FileSystemNode child = currentPos.getChildren().get(dirName);
                    if (child == null) {
                        child = new DefaultFileSystemNode(currentPos);
                        currentPos.getChildren().put(dirName, child);
                    }
                    currentPos = child;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    currentPos.getChildren().put(file.getFileName().toString(), new DefaultFileSystemNode(currentPos));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    currentPos = currentPos.getParent();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rootDir;
    }

    private String[] getPathSegments(File file) {
        return getPathSegments(file.getAbsolutePath());
    }

    private String[] getPathSegments(String absolutePath) {
        return StringUtils.split(absolutePath, FILE_PATH_SEPARATORS);
    }

    @Nullable
    public FileSystemNode getAt(File file) {
        String[] pathSegments = getPathSegments(file);
        return root.getAt(pathSegments, 0);
    }

    @Nullable
    public FileSystemNode getOrCreate(String absolutePath) {
        String[] pathSegments = getPathSegments(absolutePath);
        return root.add(pathSegments, 0);
    }
}
