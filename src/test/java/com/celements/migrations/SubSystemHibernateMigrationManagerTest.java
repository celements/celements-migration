package com.celements.migrations;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.XWikiMigrationManagerInterface;
import com.xpn.xwiki.web.Utils;

public class SubSystemHibernateMigrationManagerTest
    extends AbstractBridgedComponentTestCase{

  private XWikiContext context;
  private SubSystemHibernateMigrationManager subSysHibMigManager;
  private XWiki xwiki;
  private XWikiHibernateStore hbmStoreMock;
  private XWikiConfig xwikiCfg;

  @Before
  public void setUp_SubSystemHibernateMigrationManagerTest() throws Exception {
    context = getContext();
    xwiki = getWikiMock();
    hbmStoreMock = createMockAndAddToDefault(XWikiHibernateStore.class);
    expect(xwiki.getHibernateStore()).andReturn(hbmStoreMock).anyTimes();
    xwikiCfg = createMockAndAddToDefault(XWikiConfig.class);
    expect(xwiki.getConfig()).andReturn(xwikiCfg).anyTimes();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetForcedMigrations() throws Exception {
    expect(hbmStoreMock.executeRead(same(context), eq(true),
        isA(HibernateCallback.class))).andReturn(new XWikiDBVersion(100)).atLeastOnce();
    expect(xwikiCfg.getPropertyAsList(eq("xwiki.store.migration.force"))).andReturn(
        new String[]{
          "com.celements.migrations.SubSystemHibernateMigrationManagerTest$TestMigrator1",
          "com.celements.migrations.SubSystemHibernateMigrationManagerTest$TestMigrator2"
          }).atLeastOnce();
    ComponentManager componentManager = Utils.getComponentManager();
    ComponentDescriptor<ITestMigrator> test1DescMock = createMockAndAddToDefault(
        ComponentDescriptor.class);
    expect(test1DescMock.getRole()).andReturn(ITestMigrator.class);
    expect(test1DescMock.getRoleHint()).andReturn("migrator-test1");
    ComponentDescriptor<ITestMigrator> test2DescMock = createMockAndAddToDefault(
        ComponentDescriptor.class);
    expect(test2DescMock.getRole()).andReturn(ITestMigrator.class);
    expect(test2DescMock.getRoleHint()).andReturn("migrator-test2");
    replayDefault();
    componentManager.registerComponent(test1DescMock, new TestMigrator1());
    componentManager.registerComponent(test2DescMock, new TestMigrator2());
    subSysHibMigManager = new SubSystemHibernateMigrationManager(context, "SubSysTest",
        ITestMigrator.class);
    Map<XWikiDBVersion, ?> forcedMigrations = subSysHibMigManager.getForcedMigrations(
        context);
    assertEquals(2, forcedMigrations.size());
    verifyDefault();
  }

  @Component("migrator-test1")
  public class TestMigrator1 implements ITestMigrator {

    public String getName() {
      return null;
    }

    public String getDescription() {
      return null;
    }

    public XWikiDBVersion getVersion() {
      return new XWikiDBVersion(5);
    }

    public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
        throws XWikiException {
    }

    public boolean shouldExecute(XWikiDBVersion startupVersion) {
      return false;
    }
    
  }

  @Component("migrator-test2")
  public class TestMigrator2 implements ITestMigrator {

    public String getName() {
      return null;
    }

    public String getDescription() {
      return null;
    }

    public XWikiDBVersion getVersion() {
      return new XWikiDBVersion(2);
    }

    public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
        throws XWikiException {
    }

    public boolean shouldExecute(XWikiDBVersion startupVersion) {
      return false;
    }
    
  }

}
