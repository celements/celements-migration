package com.celements.migrations;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.annotation.Component;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.XWikiMigrationManagerInterface;

public class SubSystemHibernateMigrationManagerTest
    extends AbstractComponentTest {

  private XWikiContext context;
  private SubSystemHibernateMigrationManager subSysHibMigManager;
  private XWiki xwiki;
  private XWikiHibernateStore hbmStoreMock;
  private XWikiConfig xwikiCfg;

  @Before
  public void setUp_SubSystemHibernateMigrationManagerTest() throws Exception {
    context = getContext();
    xwiki = getWikiMock();
    hbmStoreMock = createDefaultMock(XWikiHibernateStore.class);
    expect(xwiki.getHibernateStore()).andReturn(hbmStoreMock).anyTimes();
    xwikiCfg = createDefaultMock(XWikiConfig.class);
    expect(xwiki.getConfig()).andReturn(xwikiCfg).anyTimes();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetForcedMigrations() throws Exception {
    expect(hbmStoreMock.executeRead(same(context), eq(true),
        isA(HibernateCallback.class))).andReturn(new XWikiDBVersion(100)).atLeastOnce();
    expect(xwikiCfg.getPropertyAsList(eq("xwiki.store.migration.force"))).andReturn(
        new String[] {
            "com.celements.migrations.SubSystemHibernateMigrationManagerTest$TestMigrator1",
            "com.celements.migrations.SubSystemHibernateMigrationManagerTest$TestMigrator2"
        }).atLeastOnce();
    registerComponentMock(ITestMigrator.class, "migrator-test1", new TestMigrator1());
    registerComponentMock(ITestMigrator.class, "migrator-test2", new TestMigrator2());

    replayDefault();
    subSysHibMigManager = new SubSystemHibernateMigrationManager(context, "SubSysTest",
        ITestMigrator.class);
    Map<XWikiDBVersion, ?> forcedMigrations = subSysHibMigManager.getForcedMigrations(
        context);
    assertEquals(2, forcedMigrations.size());
    verifyDefault();
  }

  @Component("migrator-test1")
  public class TestMigrator1 implements ITestMigrator {

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public XWikiDBVersion getVersion() {
      return new XWikiDBVersion(5);
    }

    @Override
    public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
        throws XWikiException {}

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion) {
      return false;
    }

  }

  @Component("migrator-test2")
  public class TestMigrator2 implements ITestMigrator {

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public XWikiDBVersion getVersion() {
      return new XWikiDBVersion(2);
    }

    @Override
    public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
        throws XWikiException {}

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion) {
      return false;
    }

  }

}
