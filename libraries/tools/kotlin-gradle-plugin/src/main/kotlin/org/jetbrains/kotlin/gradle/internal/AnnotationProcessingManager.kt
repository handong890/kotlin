/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import java.util.zip.ZipFile

public class AnnotationProcessingManager(
        private val task: AbstractCompile,
        private val javaTask: JavaCompile,
        private val taskQualifier: String,
        private val aptFiles: Set<File>,
        private val aptOutputDir: File,
        private val aptWorkingDir: File,
        private val coreClassLoader: ClassLoader,
        private val androidVariant: Any? = null) {

    private val project = task.getProject()

    private companion object {
        val JAVA_FQNAME_PATTERN = "^([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$".toRegex()
        val WRAPPERS_DIRECTORY = "wrappers"
        val GEN_ANNOTATION = "__gen/annotation"

        private val ANDROID_APT_PLUGIN_ID = "com.neenbedankt.android-apt"
    }

    fun getAnnotationFile(): File {
        if (!aptWorkingDir.exists()) aptWorkingDir.mkdirs()
        return File(aptWorkingDir, "$WRAPPERS_DIRECTORY/annotations.$taskQualifier.txt")
    }

    fun setupKapt() {
        if (aptFiles.isEmpty()) return

        if (project.getPlugins().findPlugin("com.neenbedankt.android-apt") != null) {
            project.getLogger().warn("Please do not use `$ANDROID_APT_PLUGIN_ID` with kapt.")
        }

        val annotationProcessorFqNames = lookupAnnotationProcessors(aptFiles)

        val stubOutputDir = File(aptWorkingDir, WRAPPERS_DIRECTORY)
        generateAnnotationProcessorStubs(javaTask, annotationProcessorFqNames, stubOutputDir)

        val processorPath = setOf(stubOutputDir) + aptFiles
        setProcessorPath(javaTask, processorPath.joinToString(File.pathSeparator))
        javaTask.appendClasspath(stubOutputDir)

        addGeneratedSourcesOutputToCompilerArgs(javaTask, aptOutputDir)

        appendAnnotationsFileLocationArgument()
        appendAdditionalComplerArgs()
    }

    fun afterJavaCompile() {
        val generatedFile = File(javaTask.getDestinationDir(), "$GEN_ANNOTATION/Cl.class")
        if (generatedFile.exists()) {
            generatedFile.delete()
        } else {
            project.getLogger().kotlinDebug("kapt: Java file stub was not found at $generatedFile")
        }
    }

    fun generateJavaHackFile() {
        val javaAptSourceDir = File(aptWorkingDir, "java_src")
        val javaHackPackageDir = File(javaAptSourceDir, GEN_ANNOTATION)

        javaHackPackageDir.mkdirs()

        val javaHackClFile = File(javaHackPackageDir, "Cl.java")
        javaHackClFile.writeText(
                "package __gen.annotation;" +
                "class Cl { @__gen.KotlinAptAnnotation boolean v; }")

        project.getLogger().kotlinDebug("kapt: Java file stub generated: $javaHackClFile")
        javaTask.source(javaAptSourceDir)
    }

    private fun appendAnnotationsFileLocationArgument() {
        javaTask.modifyCompilerArguments { list ->
            list.add("-Akapt.annotations=" + getAnnotationFile())
        }
    }

    private fun appendAdditionalComplerArgs() {
        val kaptExtension = project.getExtensions().getByType(javaClass<KaptExtension>())
        val args = kaptExtension.getAdditionalArguments(project, androidVariant, getAndroidExtension())
        if (args.isEmpty()) return

        javaTask.modifyCompilerArguments { list ->
            list.addAll(args)
        }
    }

    private fun generateAnnotationProcessorStubs(javaTask: JavaCompile, processorFqNames: Set<String>, outputDir: File) {
        val aptAnnotationFile = invokeCoreKaptMethod("generateKotlinAptAnnotation", outputDir) as File
        project.getLogger().kotlinDebug("kapt: Stub annotation generated: $aptAnnotationFile")

        val stubOutputPackageDir = File(outputDir, "__gen")
        stubOutputPackageDir.mkdirs()

        for (processorFqName in processorFqNames) {
            val wrapperFile = invokeCoreKaptMethod("generateAnnotationProcessorWrapper",
                    processorFqName,
                    "__gen",
                    stubOutputPackageDir,
                    getProcessorStubClassName(processorFqName),
                    taskQualifier) as File

            project.getLogger().kotlinDebug("kapt: Wrapper for $processorFqName generated: $wrapperFile")
        }

        val annotationProcessorWrapperFqNames = processorFqNames
                .map { fqName -> "__gen." + getProcessorStubClassName(fqName) }
                .joinToString(",")

        addWrappersToCompilerArgs(javaTask, annotationProcessorWrapperFqNames)
    }

    private fun JavaCompile.appendClasspath(file: File) = setClasspath(getClasspath() + project.files(file))

    private fun addWrappersToCompilerArgs(javaTask: JavaCompile, wrapperFqNames: String) {
        javaTask.addCompilerArgument("-processor") { prevValue ->
            if (prevValue != null) "$prevValue,$wrapperFqNames" else wrapperFqNames
        }
    }

    private fun getAndroidExtension(): BaseExtension? {
        try {
            return project.getExtensions().getByName("android") as BaseExtension
        } catch (e: UnknownDomainObjectException) {
            return null
        }
    }

    private fun addGeneratedSourcesOutputToCompilerArgs(javaTask: JavaCompile, outputDir: File) {
        outputDir.mkdirs()

        javaTask.addCompilerArgument("-s") { prevValue ->
            if (prevValue != null)
                javaTask.getLogger().warn("Destination for generated sources was modified by kapt. Previous value = $prevValue")
            outputDir.getAbsolutePath()
        }
    }

    private fun setProcessorPath(javaTask: JavaCompile, path: String) {
        javaTask.addCompilerArgument("-processorpath") { prevValue ->
            if (prevValue != null)
                javaTask.getLogger().warn("Processor path was modified by kapt. Previous value = $prevValue")
            path
        }
    }

    private fun getProcessorStubClassName(processorFqName: String): String {
        return "AnnotationProcessorWrapper_${taskQualifier}_${processorFqName.replace('.', '_')}"
    }

    private inline fun JavaCompile.addCompilerArgument(name: String, value: (String?) -> String) {
        modifyCompilerArguments { args ->
            val argIndex = args.indexOfFirst { name == it }

            if (argIndex >= 0 && args.size() > (argIndex + 1)) {
                args[argIndex + 1] = value(args[argIndex + 1])
            }
            else {
                args.add(name)
                args.add(value(null))
            }
        }
    }

    private inline fun JavaCompile.modifyCompilerArguments(modifier: (MutableList<String>) -> Unit) {
        val compilerArgs: List<Any> = getOptions().getCompilerArgs()
        val newCompilerArgs = compilerArgs.mapTo(arrayListOf<String>()) { it.toString() }
        modifier(newCompilerArgs)
        getOptions().setCompilerArgs(newCompilerArgs)
    }

    private fun lookupAnnotationProcessors(files: Set<File>): Set<String> {
        fun withZipFile(file: File, job: (ZipFile) -> Unit) {
            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(file)
                job(zipFile)
            }
            catch (e: IOException) {
                // Do nothing (do not continue to search for annotation processors on error)
            }
            catch (e: IllegalStateException) {
                // ZipFile was already closed for some reason
            }
            finally {
                try {
                    zipFile?.close()
                }
                catch (e: IOException) {}
            }
        }

        val annotationProcessors = hashSetOf<String>()

        fun processLines(lines: Sequence<String>) {
            for (line in lines) {
                if (line.isBlank() || !JAVA_FQNAME_PATTERN.matcher(line).matches()) continue
                annotationProcessors.add(line)
            }
        }

        for (file in files) {
            withZipFile(file) { zipFile ->
                val entry = zipFile.getEntry("META-INF/services/javax.annotation.processing.Processor")
                if (entry != null) {
                    zipFile.getInputStream(entry).reader().useLines { lines ->
                        processLines(lines)
                    }
                }
            }
        }

        project.getLogger().kotlinDebug("kapt: Discovered annotation processors: ${annotationProcessors.joinToString()}")
        return annotationProcessors
    }

    private fun invokeCoreKaptMethod(methodName: String, vararg args: Any): Any {
        val array = arrayOfNulls<Class<*>>(args.size())
        args.forEachIndexed { i, arg -> array[i] = arg.javaClass }
        val method = getCoreKaptPackageClass().getMethod(methodName, *array)
        return method.invoke(null, *args)
    }

    private fun getCoreKaptPackageClass(): Class<*> {
        return Class.forName("org.jetbrains.kotlin.gradle.tasks.kapt.KaptPackage", false, coreClassLoader)
    }

}