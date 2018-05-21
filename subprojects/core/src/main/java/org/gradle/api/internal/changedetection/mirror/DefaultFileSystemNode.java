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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class DefaultFileSystemNode implements FileSystemNode {
    private final BiMap<String, FileSystemNode> children = HashBiMap.create();
    private final FileSystemNode parent;
    private String actualPath;

    public DefaultFileSystemNode(@Nullable FileSystemNode parent) {
        this.parent = parent;
    }

    @Override
    public BiMap<String, FileSystemNode> getChildren() {
        return children;
    }

    @Override
    public FileSystemNode getParent() {
        return parent;
    }

    @Override
    public FileSystemNode add(String[] path, int current) {
        if (current == path.length) {
            return this;
        }
        FileSystemNode child = add(path[current]);
        return child.add(path, current + 1);
    }

    @Override
    public FileSystemNode add(String path) {
        FileSystemNode child = children.get(path);
        if (child == null) {
            child = new DefaultFileSystemNode(this);
            children.put(path, child);
        }
        return child;
    }

    @Override
    public void visit(@Nullable String path, Visitor visitor) {
        VisitAction visitAction = visitor.visitNode(path, this);
        if (visitAction == VisitAction.SKIP) {
            return;
        }
        for (Map.Entry<String, FileSystemNode> entry : children.entrySet()) {
            entry.getValue().visit(entry.getKey(), visitor);
        }
    }

    @Nullable
    @Override
    public FileSystemNode getAt(String[] pathSegments, int current) {
        if (pathSegments.length == current) {
            return this;
        }
        FileSystemNode child = children.get(pathSegments[current]);
        if (child != null) {
            return child.getAt(pathSegments, current + 1);
        }
        return null;
    }

    @Override
    public String getPath() {
        if (actualPath == null) {
            StringBuilder builder = new StringBuilder();
            if (parent != null) {
                builder.append(parent.getPath());
                builder.append("/");
                builder.append(parent.getChildren().inverse().get(this));
            }
            actualPath = builder.toString();
        }
        return actualPath;
    }

    @Override
    public String[] getSegments(FileSystemNode startDir) {
        Deque<String> path = new ArrayDeque<String>();
        FileSystemNode current = this;
        while (current != startDir) {
            path.addFirst(current.getParent().getChildren().inverse().get(current));
            current = current.getParent();
        }
        return path.toArray(new String[0]);
    }
}
