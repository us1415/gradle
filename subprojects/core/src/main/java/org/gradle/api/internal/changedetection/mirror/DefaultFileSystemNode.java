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

public class DefaultFileSystemNode implements FileSystemNode {
    private final BiMap<String, FileSystemNode> children = HashBiMap.create();
    private final FileSystemNode parent;

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
        String currentPath = path[current];
        FileSystemNode child = children.get(currentPath);
        if (child == null) {
            child = new DefaultFileSystemNode(this);
            children.put(currentPath, child);
        }
        return child.add(path, current + 1);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.visitNode(this);
        for (FileSystemNode child : children.values()) {
            child.visit(visitor);
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
        StringBuilder builder = new StringBuilder();
        if (parent != null) {
            builder.append(parent.getPath());
            builder.append("/");
            builder.append(parent.getChildren().inverse().get(this));
        }
        return builder.toString();
    }
}
