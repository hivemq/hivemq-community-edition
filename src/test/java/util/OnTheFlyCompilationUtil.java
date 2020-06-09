/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package util;

import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Various utilities for compilation of Java classes on the fly
 *
 * @author Dominik Obermaier
 * @author Georg Held
 */
public class OnTheFlyCompilationUtil {

    public static File compileJavaFile(final File javaFile, final File toFolder) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(toFolder));


        // Compile the file
        compiler.getTask(null, fileManager, null, null, null,
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(javaFile))).call();
        fileManager.close();

        final Collection<File> files = FileUtils.listFiles(toFolder, new SuffixFileFilter("class"), TrueFileFilter.INSTANCE);

        return Iterables.getOnlyElement(files);
    }


    public static ClassLoader compile(final StringJavaFileObject... toCompile) throws Exception {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final MemClassLoader classLoader = new MemClassLoader();
        final JavaFileManager fileManager = new MemJavaFileManager(compiler, classLoader);


        final Collection<? extends JavaFileObject> units = Arrays.asList(toCompile);
        final JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, units);
        task.call();
        fileManager.close();

        classLoader.persist();

        return classLoader;
    }


    /*
        Utils for the compiler API for in-memory compilation
     */


    public static class StringJavaFileObject extends SimpleJavaFileObject {
        private final CharSequence code;

        public StringJavaFileObject(final String name, final CharSequence code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return code;
        }
    }

    static class MemClassLoader extends ClassLoader {
        private final Map<String, MemJavaFileObject> classFiles = new HashMap<>();
        private final File tempDir;


        public MemClassLoader() throws Exception {
            super(ClassLoader.getSystemClassLoader());
            final File tempDir = Files.createTempDir();
            tempDir.deleteOnExit();
            this.tempDir = tempDir;

        }

        public void addClassFile(final MemJavaFileObject memJavaFileObject) throws IOException {
            classFiles.put(memJavaFileObject.getClassName(), memJavaFileObject);
        }

        public void persist() throws Exception {
            for (final Map.Entry<String, MemJavaFileObject> objectEntry : classFiles.entrySet()) {

                final MemJavaFileObject value = objectEntry.getValue();
                final File file = new File(tempDir, value.getClassName() + ".class");
                Files.write(value.getClassBytes(), file);
            }
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            final MemJavaFileObject fileObject = classFiles.get(name);

            if (fileObject != null) {
                final byte[] bytes = fileObject.getClassBytes();
                return defineClass(name, bytes, 0, bytes.length);
            }

            return super.findClass(name);
        }

        @Override
        public URL getResource(final String name) {
            final String[] list = tempDir.list(new NameFileFilter(name));
            if (list.length == 0) {
                return super.getResource(name);
            } else {
                try {
                    return new File(tempDir, list[0]).toURI().toURL();
                } catch (final MalformedURLException e) {
                    return null;
                }
            }
        }
    }

    static class MemJavaFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        private final String className;

        MemJavaFileObject(final String className) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.CLASS.extension),
                    Kind.CLASS);
            this.className = className;
        }

        String getClassName() {
            return className;
        }

        byte[] getClassBytes() {
            return baos.toByteArray();
        }

        @Override
        public OutputStream openOutputStream() {
            return baos;
        }


    }

    static class MemJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final MemClassLoader classLoader;

        public MemJavaFileManager(final JavaCompiler compiler, final MemClassLoader classLoader) {
            super(compiler.getStandardFileManager(null, null, null));

            this.classLoader = classLoader;
        }


        @Override
        public JavaFileObject getJavaFileForOutput(final Location location,
                                                   final String className,
                                                   final JavaFileObject.Kind kind,
                                                   final FileObject sibling) throws IOException {
            final MemJavaFileObject fileObject = new MemJavaFileObject(className);
            classLoader.addClassFile(fileObject);
            return fileObject;
        }
    }
}
