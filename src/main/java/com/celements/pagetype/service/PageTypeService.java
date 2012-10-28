/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.celements.pagetype.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.pagetype.IPageTypeConfig;
import com.celements.pagetype.IPageTypeProviderRole;
import com.celements.pagetype.PageTypeReference;

@Component
public class PageTypeService implements IPageTypeRole {

  @Requirement
  Map<String, IPageTypeProviderRole> pageTypeProviders;

  public IPageTypeConfig getPageTypeConfig(String pageTypeName) {
    PageTypeReference pageTypeRef = getPageTypeRefsByConfigNames().get(pageTypeName);
    return getProviderForPageTypeRef(pageTypeRef).getPageTypeByReference(pageTypeRef);
  }

  private IPageTypeProviderRole getProviderForPageTypeRef(PageTypeReference pageTypeRef) {
    return pageTypeProviders.get(pageTypeRef.getProviderHint());
  }

  public List<String> getPageTypesConfigNamesForCategories(Set<String> catList,
      boolean onlyVisible) {
    List<String> pageTypeConfigNameList = new ArrayList<String>();
    for (PageTypeReference pageTypeRef : getPageTypeRefsForCategories(catList,
        onlyVisible)) {
      pageTypeConfigNameList.add(pageTypeRef.getConfigName());
    }
    return pageTypeConfigNameList;
  }

  public List<PageTypeReference> getPageTypeRefsForCategories(Set<String> catList,
      boolean onlyVisible) {
    if (onlyVisible) {
      Set<PageTypeReference> visiblePTSet = new HashSet<PageTypeReference>();
      for (PageTypeReference pageTypeRef : getPageTypeRefsForCategories(catList)) {
        if (getProviderForPageTypeRef(pageTypeRef).getPageTypeByReference(pageTypeRef
            ).isVisible()) {
          visiblePTSet.add(pageTypeRef);
        }
      }
      return new ArrayList<PageTypeReference>(visiblePTSet);
    } else {
      return new ArrayList<PageTypeReference>(getPageTypeRefsForCategories(catList));
    }
  }

  Map<String, PageTypeReference> getPageTypeRefsByConfigNames() {
    Map<String, PageTypeReference> pageTypeRefsMap =
      new HashMap<String, PageTypeReference>();
    for (PageTypeReference pageTypeRef : getAllPageTypeRefs()) {
      pageTypeRefsMap.put(pageTypeRef.getConfigName(), pageTypeRef);
    }
    return pageTypeRefsMap;
  }

  private Set<PageTypeReference> getAllPageTypeRefs() {
    HashSet<PageTypeReference> pageTypeRefSet = new HashSet<PageTypeReference>();
    for (IPageTypeProviderRole pageTypeProvider : pageTypeProviders.values()) {
      for (PageTypeReference pageTypeRef : pageTypeProvider.getPageTypes()) {
        pageTypeRefSet.add(pageTypeRef);
      }
    }
    return pageTypeRefSet;
  }

  private Set<PageTypeReference> getPageTypeRefsForCategories(Set<String> catList) {
    catList = new HashSet<String>(catList);
    Set<PageTypeReference> filteredPTset = new HashSet<PageTypeReference>();
    for (PageTypeReference pageTypeRef : getAllPageTypeRefs()) {
      for (String category : pageTypeRef.getCategories()) {
        if (catList.contains(category)) {
          filteredPTset.add(pageTypeRef);
        }
      }
    }
    return filteredPTset;
  }

}
