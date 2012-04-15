package com.celements.navigation.service;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.celements.inheritor.InheritorFactory;
import com.celements.navigation.Navigation;
import com.celements.navigation.TreeNode;
import com.celements.navigation.cmd.GetMappedMenuItemsForParentCommand;
import com.celements.navigation.cmd.GetNotMappedMenuItemsForParentCommand;
import com.celements.web.plugin.cmd.PageLayoutCommand;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;

public class TreeNodeServiceTest extends AbstractBridgedComponentTestCase {

  private XWikiContext context;
  private XWiki wiki;
  private XWikiStoreInterface mockStore;
  private XWikiStoreInterface mockXStore;
  private TreeNodeService treeNodeService;
  private ITreeNodeCache mockTreeNodeCache;
  private GetNotMappedMenuItemsForParentCommand testGetNotMenuItemCommand;
  private GetMappedMenuItemsForParentCommand testGetMenuItemCommand;

  @Before
  public void setUp_TreeNodeServiceTest() throws Exception {
    context = getContext();
    wiki = createMock(XWiki.class);
    context.setWiki(wiki);
    mockStore = createMock(XWikiStoreInterface.class);
    expect(wiki.getStore()).andReturn(mockStore).anyTimes();
    treeNodeService = new TreeNodeService();
    mockXStore = createMock(XWikiStoreInterface.class);
    expect(wiki.getStore()).andReturn(mockXStore).anyTimes();
    mockTreeNodeCache = createMock(ITreeNodeCache.class);
    treeNodeService.treeNodeCache = mockTreeNodeCache;
    treeNodeService.execution = getComponentManager().lookup(Execution.class);
    testGetNotMenuItemCommand = createMock(GetNotMappedMenuItemsForParentCommand.class);
    expect(mockTreeNodeCache.getNotMappedMenuItemsForParentCmd()).andReturn(
        testGetNotMenuItemCommand).anyTimes();
    testGetMenuItemCommand = createMock(
        GetMappedMenuItemsForParentCommand.class);
    expect(mockTreeNodeCache.getMappedMenuItemsForParentCmd()).andReturn(
        testGetMenuItemCommand).anyTimes();
  }

  @Test
  public void testGetSubNodesForParent() throws Exception {
    DocumentReference docRef = new DocumentReference(context.getDatabase(), "Content",
        "MainPage");
    TreeNode treeNode = new TreeNode(docRef, "Content", 1);
    List<TreeNode> mockTreeNodeList = Arrays.asList(treeNode, null);
    expect(testGetNotMenuItemCommand.getTreeNodesForParentKey(eq("xwikidb:Content."),
        same(context))).andReturn(mockTreeNodeList);
    List<TreeNode> emptyList = Collections.emptyList();
    expect(testGetMenuItemCommand.getTreeNodesForParentKey(eq("xwikidb:Content."),
        same(context))).andReturn(emptyList);
    XWikiRightService mockRightService = createMock(XWikiRightService.class);
    expect(wiki.getRightService()).andReturn(mockRightService).anyTimes();
    expect(mockRightService.hasAccessLevel(eq("view"), eq("XWiki.XWikiGuest"),
        eq("Content.MainPage"), same(context))).andReturn(true);
    replayAll(mockRightService);
    List<TreeNode> resultList = treeNodeService.getSubNodesForParent("", "Content", "");
    assertEquals(1, resultList.size());
    assertTrue(mockTreeNodeList.contains(treeNode));
    verifyAll(mockRightService);
  }

  @Test
  public void testFetchNodesForParentKey_mergeCombinedResult() {
    context.setDatabase("myWiki");
    String parentKey = "myWiki:mySpace.myDoc";
    TreeNode menuItem2 = createTreeNode("mySpace", "myDoc2", "mySpace", "myDoc", 2);
    TreeNode menuItem3 = createTreeNode("mySpace", "myDoc1", "mySpace", "myDoc", 3);
    TreeNode menuItem1 = createTreeNode("mySpace", "myDoc1", "mySpace", "myDoc", 1);
    TreeNode menuItem5 = createTreeNode("mySpace", "myDoc5", "mySpace", "myDoc", 5);
    List<TreeNode> mappedList = Arrays.asList(menuItem1, menuItem5);
    expect(testGetMenuItemCommand.getTreeNodesForParentKey(eq(parentKey), same(context))
        ).andReturn(mappedList);
    expect(testGetNotMenuItemCommand.getTreeNodesForParentKey(eq(parentKey),
        same(context))).andReturn(Arrays.asList(menuItem2, menuItem3)).atLeastOnce();
    replayAll();
    List<TreeNode> menuItemsMerged = treeNodeService.fetchNodesForParentKey(parentKey);
    List<TreeNode> expectedList = Arrays.asList(menuItem1, menuItem2, menuItem3,
        menuItem5);
    assertEquals("result array does not match expected size.", expectedList.size(),
        menuItemsMerged.size());
    int pos = 0;
    for (TreeNode menuItem : menuItemsMerged) {
      TreeNode expectedMenuitem = expectedList.get(pos++);
      assertEquals("Array compare failed on item " + pos, expectedMenuitem.getPosition(),
          menuItem.getPosition());
      assertSame("Array compare failed on item " + pos, expectedMenuitem, menuItem);
    }
    verifyAll();
  }

  @Test
  public void testFetchNodesForParentKey_onlyOldArray() {
    context.setDatabase("myWiki");
    String parentKey = "myWiki:mySpace.myDoc";
    TreeNode menuItem2 = createTreeNode("mySpace", "myDoc2", "mySpace", "myDoc", 2);
    TreeNode menuItem3 = createTreeNode("mySpace", "myDoc1", "mySpace", "myDoc", 3);
    List<TreeNode> oldNotMappedList = Arrays.asList(menuItem2, menuItem3);
    List<TreeNode> mappedList = Collections.emptyList();
    expect(testGetMenuItemCommand.getTreeNodesForParentKey(eq(parentKey), same(context))
        ).andReturn(mappedList);
    expect(testGetNotMenuItemCommand.getTreeNodesForParentKey(eq(parentKey), same(context)
        )).andReturn(oldNotMappedList).atLeastOnce();
    replayAll();
    List<TreeNode> menuItemsMerged = treeNodeService.fetchNodesForParentKey(parentKey);
    assertSame("expecting old notMapped list.", oldNotMappedList, menuItemsMerged);
    verifyAll();
  }

  @Test
  public void testFetchNodesForParentKey_onlyNewMappedList() {
    context.setDatabase("myWiki");
    String parentKey = "myWiki:mySpace.myDoc";
    List<TreeNode> oldMenuItems = Collections.emptyList();
    TreeNode menuItem1 = createTreeNode("mySpace", "myDoc1", "mySpace", "myDoc", 1);
    TreeNode menuItem5 = createTreeNode("mySpace", "myDoc5", "mySpace", "myDoc", 5);
    List<TreeNode> mappedList = Arrays.asList(menuItem1, menuItem5);
    expect(testGetMenuItemCommand.getTreeNodesForParentKey(eq(parentKey), same(context))
        ).andReturn(mappedList);
    expect(testGetNotMenuItemCommand.getTreeNodesForParentKey(eq(parentKey),
        same(context))).andReturn(oldMenuItems).atLeastOnce();
    replayAll();
    List<TreeNode> menuItemsMerged = treeNodeService.fetchNodesForParentKey(parentKey);
    assertSame("expecting old notMapped list.", mappedList, menuItemsMerged);
    verifyAll();
  }
  
  @Test
  public void testFetchNodesForParentKey_noMenuItems_NPE() {
    context.setDatabase("myWiki");
    String parentKey = "myWiki:mySpace.myDoc";
    List<TreeNode> mappedList = Collections.emptyList();
    expect(testGetMenuItemCommand.getTreeNodesForParentKey(eq(parentKey), same(context))
        ).andReturn(mappedList);
    expect(testGetNotMenuItemCommand.getTreeNodesForParentKey(eq(parentKey), same(context)
        )).andReturn(null).atLeastOnce();
    replayAll();
    List<TreeNode> menuItemsMerged = treeNodeService.fetchNodesForParentKey(parentKey);
    assertNotNull("expecting not null.", menuItemsMerged);
    assertEquals("expecting empty list.", 0, menuItemsMerged.size());
    verifyAll();
  }


  private TreeNode createTreeNode(String docSpace, String docName, String parentDocSpace,
      String parentDocName, int pos) {
    return new TreeNode(new DocumentReference(context.getDatabase(), docSpace, docName),
        parentDocSpace + "." + parentDocName, pos);
  }
  
  private void replayAll(Object ... mocks) {
    replay(mockStore, wiki, mockTreeNodeCache, testGetNotMenuItemCommand,
        testGetMenuItemCommand);
    replay(mocks);
  }

  private void verifyAll(Object ... mocks) {
    verify(mockStore, wiki, mockTreeNodeCache, testGetNotMenuItemCommand,
        testGetMenuItemCommand);
    verify(mocks);
  }
  
  @Test
  public void testGetMaxConfiguredNavigationLevel_twoParents() throws Exception {
    InheritorFactory inheritorFact = new InheritorFactory();
    PageLayoutCommand mockPageLayoutCmd = createMock(PageLayoutCommand.class);
    inheritorFact.injectPageLayoutCmd(mockPageLayoutCmd);
    treeNodeService.injectInheritorFactory(inheritorFact);
    XWikiDocument doc = new XWikiDocument();
    doc.setFullName("MySpace.MyDocument");
    expect(mockPageLayoutCmd.getPageLayoutForDoc(eq(doc.getFullName()), same(context))
        ).andReturn(null).atLeastOnce();
    context.setDoc(doc);
    XWikiDocument webPrefDoc = new XWikiDocument();
    webPrefDoc.setFullName("MySpace.WebPreferences");
    expect(wiki.getDocument(eq("MySpace.WebPreferences"), eq(context))).andReturn(
        webPrefDoc).atLeastOnce();
    Vector<BaseObject> navObjects = new Vector<BaseObject>();
    navObjects.add(createNavObj(5, webPrefDoc));
    navObjects.add(createNavObj(4, webPrefDoc));
    navObjects.add(createNavObj(8, webPrefDoc));
    navObjects.add(createNavObj(3, webPrefDoc));
    expect(wiki.getDocument(eq(new DocumentReference("xwikidb", "MySpace", 
        "WebPreferences")), same(context))).andReturn(webPrefDoc).atLeastOnce();
    webPrefDoc.setObjects(Navigation.NAVIGATION_CONFIG_CLASS, navObjects);
    expect(wiki.getWebPreference(eq("skin"), same(context))).andReturn("Skins.MySkin"
        ).atLeastOnce();
    replay(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    int maxLevel = treeNodeService.getMaxConfiguredNavigationLevel(context);
    verify(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    assertEquals("Max to Level in navConfigs is 8.", 8, maxLevel);
  }

  @Test
  public void testGetMaxConfiguredNavigationLevel_deletedObject_NPE() throws Exception {
    InheritorFactory inheritorFact = new InheritorFactory();
    PageLayoutCommand mockPageLayoutCmd = createMock(PageLayoutCommand.class);
    inheritorFact.injectPageLayoutCmd(mockPageLayoutCmd);
    treeNodeService.injectInheritorFactory(inheritorFact);
    XWikiDocument doc = new XWikiDocument();
    doc.setFullName("MySpace.MyDocument");
    expect(mockPageLayoutCmd.getPageLayoutForDoc(eq(doc.getFullName()), same(context))
        ).andReturn(null).atLeastOnce();
    context.setDoc(doc);
    XWikiDocument webPrefDoc = new XWikiDocument();
    webPrefDoc.setFullName("MySpace.WebPreferences");
    expect(wiki.getDocument(eq("MySpace.WebPreferences"), eq(context))
        ).andReturn(webPrefDoc).atLeastOnce();
    Vector<BaseObject> navObjects = new Vector<BaseObject>();
    navObjects.add(createNavObj(5, webPrefDoc));
    navObjects.add(null); // deleting an object can lead to a null pointer
                          // in the object list
    navObjects.add(createNavObj(8, webPrefDoc));
    navObjects.add(createNavObj(3, webPrefDoc));
    webPrefDoc.setObjects(Navigation.NAVIGATION_CONFIG_CLASS, navObjects);
    expect(wiki.getDocument(eq(new DocumentReference("xwikidb", "MySpace", 
        "WebPreferences")), same(context))).andReturn(webPrefDoc).atLeastOnce();
    expect(wiki.getWebPreference(eq("skin"), same(context))).andReturn("Skins.MySkin"
      ).atLeastOnce();
    replay(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    int maxLevel = treeNodeService.getMaxConfiguredNavigationLevel(context);
    verify(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    assertEquals("Max to Level in navConfigs is 8.", 8, maxLevel);
  }

  @Test
  public void testGetMaxConfiguredNavigationLevel_noObjectFound_NPE() throws Exception {
    InheritorFactory inheritorFact = new InheritorFactory();
    PageLayoutCommand mockPageLayoutCmd = createMock(PageLayoutCommand.class);
    inheritorFact.injectPageLayoutCmd(mockPageLayoutCmd);
    treeNodeService.injectInheritorFactory(inheritorFact);
    XWikiDocument doc = new XWikiDocument();
    doc.setFullName("MySpace.MyDocument");
    expect(mockPageLayoutCmd.getPageLayoutForDoc(eq(doc.getFullName()), same(context))
        ).andReturn(null).atLeastOnce();
    context.setDoc(doc);
    XWikiDocument webPrefDoc = new XWikiDocument();
    webPrefDoc.setFullName("MySpace.WebPreferences");
    expect(wiki.getDocument(eq("MySpace.WebPreferences"), eq(context))
      ).andReturn(webPrefDoc).atLeastOnce();
    XWikiDocument xwikiPrefDoc = new XWikiDocument();
    webPrefDoc.setFullName("XWiki.XWikiPreferences");
    expect(wiki.getDocument(eq("XWiki.XWikiPreferences"), eq(context))
      ).andReturn(xwikiPrefDoc).atLeastOnce();
    XWikiDocument skinDoc = new XWikiDocument();
    skinDoc.setFullName("Skins.MySkin");
    expect(wiki.getDocument(eq("Skins.MySkin"), eq(context))
      ).andReturn(skinDoc).atLeastOnce();
    expect(wiki.getWebPreference(eq("skin"), same(context))).andReturn("Skins.MySkin"
      ).atLeastOnce();
    replay(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    int maxLevel = treeNodeService.getMaxConfiguredNavigationLevel(context);
    verify(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    assertEquals("Expecting default max level.", Navigation.DEFAULT_MAX_LEVEL, maxLevel);
  }

  @Test
  public void testGetMaxConfiguredNavigationLevel_threeParents() throws Exception {
    InheritorFactory inheritorFact = new InheritorFactory();
    PageLayoutCommand mockPageLayoutCmd = createMock(PageLayoutCommand.class);
    inheritorFact.injectPageLayoutCmd(mockPageLayoutCmd);
    treeNodeService.injectInheritorFactory(inheritorFact);
    XWikiDocument doc = new XWikiDocument();
    doc.setFullName("MySpace.MyDocument");
    expect(mockPageLayoutCmd.getPageLayoutForDoc(eq(doc.getFullName()), same(context))
        ).andReturn(null).atLeastOnce();
    context.setDoc(doc);
    XWikiDocument webPrefDoc = new XWikiDocument();
    webPrefDoc.setFullName("MySpace.WebPreferences");
    expect(wiki.getDocument(eq("MySpace.WebPreferences"), eq(context))
        ).andReturn(webPrefDoc).atLeastOnce();
    expect(wiki.getWebPreference(eq("skin"), same(context))).andReturn("Skins.MySkin"
      ).atLeastOnce();
    Vector<BaseObject> navObjects = new Vector<BaseObject>();
    navObjects.add(createNavObj(5, webPrefDoc));
    navObjects.add(createNavObj(4, webPrefDoc));
    navObjects.add(createNavObj(3, webPrefDoc));
    webPrefDoc.setObjects(Navigation.NAVIGATION_CONFIG_CLASS, navObjects);
    expect(wiki.getDocument(eq(new DocumentReference("xwikidb", "MySpace", 
        "WebPreferences")), same(context))).andReturn(webPrefDoc).atLeastOnce();
    replay(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    int maxLevel = treeNodeService.getMaxConfiguredNavigationLevel(context);
    verify(mockStore, wiki, mockXStore, mockPageLayoutCmd);
    assertEquals("Parents are a.b, b.c and c.d therefor maxlevel must be 5.",
        5, maxLevel);
  }
  
  private BaseObject createNavObj(int toLevel, XWikiDocument doc) {
    BaseObject navObj = new BaseObject();
    navObj.setClassName(Navigation.NAVIGATION_CONFIG_CLASS);
    navObj.setStringValue("menu_element_name", "mainMenu");
    navObj.setIntValue("to_hierarchy_level", toLevel);
    navObj.setName(doc.getFullName());
    navObj.setDocumentReference(doc.getDocumentReference());
    return navObj;
  }

}
