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
package com.celements.migrations;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

public class MigrationManagerBridgeTest extends AbstractComponentTest {

  private XWikiContext context;
  private XWiki xwiki;
  private MigrationManagerBridge migManagerBridge;

  @Before
  public void setUp_MigrationManagerBridgeTest() throws Exception {
    context = getContext();
    xwiki = createMock(XWiki.class);
    context.setWiki(xwiki);
    XWikiConfig configMock = createMock(XWikiConfig.class);
    expect(xwiki.getConfig()).andReturn(configMock).anyTimes();
    expect(configMock.getProperty(eq("xwiki.store.migration.version"))).andReturn("2345")
        .anyTimes();
    replay(xwiki, configMock);
    migManagerBridge = new MigrationManagerBridge(context);
  }

  @Test
  public void testGetAllMigrationsXWikiContext() throws Exception {
    try {
      migManagerBridge.getAllMigrations(context);
      fail("Expecting UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testSetDBVersionXWikiDBVersionXWikiContext() throws Exception {
    try {
      migManagerBridge.setDBVersion(new XWikiDBVersion(123), context);
      fail("Expecting UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testStartMigrationsXWikiContext() throws Exception {
    ISubSystemMigrationCoordinator coordinaterMock = registerComponentMock(
        ISubSystemMigrationCoordinator.class);
    coordinaterMock.startSubSystemMigrations(same(context));
    expectLastCall().once();
    replay(coordinaterMock);
    migManagerBridge.startMigrations(context);
    verify(xwiki, coordinaterMock);
  }

}
