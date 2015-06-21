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

package org.jetbrains.kotlin.idea.project;

import com.intellij.openapi.project.Project;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingFunctionShortNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingPropertyShortNameIndex;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.lazy.ElementResolver;
import org.jetbrains.kotlin.resolve.lazy.ProbablyNothingCallableNames;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.LazyResolveStorageManager;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.types.DynamicTypesSettings;

import java.util.Collection;

public class ResolveElementCache extends ElementResolver implements ResolveTaskManager {
    private final Project project;
    private final CachedValue<MemoizedFunctionToNotNull<JetElement, BindingContext>> additionalResolveCache;

    public ResolveElementCache(ResolveSession resolveSession, Project project) {
        super(resolveSession);
        this.project = project;

        final Function1<JetElement, BindingContext> countCachedTrace = new Function1<JetElement, BindingContext>() {
            @Override
            public BindingContext invoke(JetElement jetElement) {
                return performElementAdditionalResolve(jetElement, jetElement, BodyResolveMode.FULL);
            }
        };

        // Recreate internal cache after change of modification count
        this.additionalResolveCache =
                CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<MemoizedFunctionToNotNull<JetElement, BindingContext>>() {
                            @Nullable
                            @Override
                            public Result<MemoizedFunctionToNotNull<JetElement, BindingContext>> compute() {
                                ResolveSession resolveSession = ResolveElementCache.this.getResolveSession();
                                LazyResolveStorageManager manager = resolveSession.getStorageManager();
                                MemoizedFunctionToNotNull<JetElement, BindingContext> elementsCacheFunction =
                                        manager.createSoftlyRetainedMemoizedFunction(countCachedTrace);

                                return Result.create(elementsCacheFunction,
                                                     PsiModificationTracker.MODIFICATION_COUNT,
                                                     resolveSession.getExceptionTracker());
                            }
                        },
                        false);
    }

    @NotNull
    @Override
    public BindingContext getElementAdditionalResolve(@NotNull JetElement jetElement) {
        return additionalResolveCache.getValue().invoke(jetElement);
    }

    @Override
    public boolean hasElementAdditionalResolveCached(@NotNull JetElement jetElement) {
        if (!additionalResolveCache.hasUpToDateValue()) return false;
        return additionalResolveCache.getValue().isComputed(jetElement);
    }

    @NotNull
    @Override
    public AdditionalCheckerProvider getAdditionalCheckerProvider(@NotNull JetFile jetFile) {
        return TargetPlatformDetector.getPlatform(jetFile).getAdditionalCheckerProvider();
    }

    @NotNull
    @Override
    public DynamicTypesSettings getDynamicTypesSettings(@NotNull JetFile jetFile) {
        return TargetPlatformDetector.getPlatform(jetFile).getDynamicTypesSettings();
    }

    @NotNull
    @Override
    protected ProbablyNothingCallableNames probablyNothingCallableNames() {
        return new ProbablyNothingCallableNames() {
            @NotNull
            @Override
            public Collection<String> functionNames() {
                // we have to add hardcoded-names until we have Kotlin compiled classes in caches
                return JetProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project);
            }

            @NotNull
            @Override
            public Collection<String> propertyNames() {
                return JetProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project);
            }
        };
    }

    @NotNull
    @Override
    public BodyResolveResult resolveFunctionBody(@NotNull JetNamedFunction function) {
        JetScope declarationScope = getResolveSession().getScopeProvider().getResolutionScopeForDeclaration(function);
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) getResolveSession().resolveToDescriptor(function);

        BodyResolveContext resolveQueryContext =
                new BodyResolveContext(DataFlowInfo.EMPTY, getResolveSession().getTrace(), functionDescriptor, declarationScope);

        DelegatingBindingTrace trace = (DelegatingBindingTrace) getElementAdditionalResolve(function);

        return new BodyResolveResult(trace, resolveQueryContext);
    }
}
