package com.atolcd.alfresco.filer.core.service.impl;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;
import com.atolcd.alfresco.filer.core.scope.impl.EmptyFilerScopeLoader;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class FilerRegistryImpl implements FilerRegistry {

  @CheckForNull
  private SortedSet<FilerAction> actions;
  @CheckForNull
  private Set<FilerScopeLoader> scopeLoaders;

  @Override
  public void registerAction(final FilerAction action) {
    getActions().add(action);
  }

  @Override
  public void registerScopeLoader(final FilerScopeLoader scopeLoader) {
    if (!EmptyFilerScopeLoader.class.equals(scopeLoader.getClass())) {
      getScopeLoaders().add(scopeLoader);
    }
  }

  @Override
  public SortedSet<FilerAction> getActions() {
    actions = Optional.ofNullable(actions).orElseGet(TreeSet::new);
    return actions;
  }

  @Override
  public Set<FilerScopeLoader> getScopeLoaders() {
    scopeLoaders = Optional.ofNullable(scopeLoaders).orElseGet(LinkedHashSet::new);
    return scopeLoaders;
  }
}
