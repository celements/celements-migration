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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.xwiki.component.manager.ComponentLookupException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.AbstractXWikiMigrationManager;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.XWikiMigratorInterface;
import com.xpn.xwiki.web.Utils;

public class SubSystemHibernateMigrationManager extends AbstractXWikiMigrationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      SubSystemHibernateMigrationManager.class);

  private String subSystemName;

  private Class<? extends XWikiMigratorInterface> subMigratorInterface;

  public SubSystemHibernateMigrationManager(XWikiContext context, String subSystemName,
      Class<? extends XWikiMigratorInterface> subMigratorInterface
      ) throws XWikiException {
    super(context);
    this.subSystemName = subSystemName;
    this.subMigratorInterface = subMigratorInterface;
  }

  @Override
  protected Map<XWikiDBVersion, XWikiMigration> getForcedMigrations(
      XWikiContext context) throws Exception {
    SortedMap<XWikiDBVersion, XWikiMigration> forcedMigrations =
        new TreeMap<XWikiDBVersion, XWikiMigration>();
    List<String> forcedMigrationsArray = Arrays.asList(context.getWiki().getConfig(
        ).getPropertyAsList("xwiki.store.migration.force"));
    for (XWikiMigratorInterface migrator : getAllMigrations(context)) {
      String migratorName = migrator.getClass().getName();
      if (forcedMigrationsArray.contains(migratorName)) {
        XWikiMigration migration = new XWikiMigration(migrator, true);
        forcedMigrations.put(migrator.getVersion(), migration);
      }
    }
    return forcedMigrations;
  }

  /**
   * @return store system for execute store-specific actions.
   * @param context
   *          - used everywhere
   */
  public XWikiHibernateBaseStore getStore(XWikiContext context) {
    return context.getWiki().getHibernateStore();
  }

  public void updateSchema(XWikiContext context) {
    XWikiHibernateBaseStore store = getStore(context);
    if (store != null) {
        store.updateSchema(context, true);
    }
  }

  /** {@inheritDoc} */
  @Override
  public XWikiDBVersion getDBVersion(XWikiContext context) throws XWikiException {
    return getStore(context).executeRead(context, true,
        new HibernateCallback<XWikiDBVersion>() {
          public XWikiDBVersion doInHibernate(Session session) throws HibernateException {
            SubSystemDBVersion result = (SubSystemDBVersion) session.createCriteria(
                SubSystemDBVersion.class).add(
                Restrictions.eq("subSystemName", getSubSystemName())).uniqueResult();
            if (result == null) {
              return new XWikiDBVersion(0);
            } else {
              return new XWikiDBVersion(result.getVersion());
            }
          }
        });
  }

  /** {@inheritDoc} */
  @Override
  protected void setDBVersion(final XWikiDBVersion version, XWikiContext context
      ) throws XWikiException {
    getStore(context).executeWrite(context, true, new HibernateCallback<Object>() {
      public Object doInHibernate(Session session) throws HibernateException {
        session.createQuery("delete from " + SubSystemDBVersion.class.getName()
            + " where subSystemName='" + getSubSystemName() + "'").executeUpdate();
        session.save(new SubSystemDBVersion(getSubSystemName(), version.getVersion()));
        return null;
      }
    });
  }

  /** {@inheritDoc} */
  /* XXX Override parrent implementation to FIX update of next db version!
   */
  @Override
  protected void startMigrations(Collection migrations, XWikiContext context
      ) throws Exception {
    XWikiDBVersion curversion = getDBVersion(context);
    for (Iterator it = migrations.iterator(); it.hasNext();) {
      XWikiMigration migration = (XWikiMigration) it.next();

      if (migration.isForced || migration.migrator.shouldExecute(curversion)) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Running migration [" + migration.migrator.getName()
              + "] with version [" + migration.migrator.getVersion() + "]");
        }
        migration.migrator.migrate(this, context);
      } else {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Skipping unneeded migration [" + migration.migrator.getName()
              + "] with version [" + migration.migrator.getVersion() + "]");
        }
      }

      if (migration.migrator.getVersion().compareTo(curversion) >= 0) {
        setDBVersion(migration.migrator.getVersion().increment(), context);
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("New storage version is now [" + getDBVersion(context) + "]");
        }
      } else {
        LOGGER.info("NO new storage version set because compare is: "
            + migration.migrator.getVersion().compareTo(curversion));
      }

    }
  }

  public String getSubSystemName() {
    return subSystemName;
  }

  @Override
  protected List<? extends XWikiMigratorInterface> getAllMigrations(XWikiContext context
      ) throws XWikiException {
    List<? extends XWikiMigratorInterface> result = null;
    try {
      result = Utils.getComponentManager().lookupList(subMigratorInterface);
    } catch (ComponentLookupException exp) {
      LOGGER.error("Failed to get Migrator for subsystem [" + getSubSystemName() + "].",
          exp);
    }
    if (result != null) {
      LOGGER.debug("lookup for [" + subMigratorInterface + "] returned [" + result.size()
          + "] migrator.");
      return result;
    }
    LOGGER.debug("lookup for [" + subMigratorInterface + "] returned empty list.");
    return Collections.emptyList();
  }

  public void initDatabaseVersion(XWikiContext context) {
    try {
      List<? extends XWikiMigratorInterface> allMigrations = getAllMigrations(context);
      XWikiDBVersion maxVersion = getDBVersion(context);
      //CAUTION: equals is not implemented on XWikiDBVersion!
      if ((maxVersion == null) || (maxVersion.compareTo(new XWikiDBVersion(0)) == 0)) {
        for(XWikiMigratorInterface theMigration : allMigrations) {
          XWikiDBVersion theVersion = theMigration.getVersion();
          if ((maxVersion == null) || (theVersion.compareTo(maxVersion) > 0)) {
            maxVersion = theVersion;
          }
        }
        XWikiDBVersion newVersion = maxVersion.increment();
        LOGGER.info("init database version for subsystem [" + getSubSystemName()
            + "] with  [" + newVersion + "] .");
        setDBVersion(newVersion, context);
      } else {
        LOGGER.info("skip init database version for subsystem [" + getSubSystemName()
            + "] already found version [" + maxVersion + "] .");
      }
    } catch (XWikiException exp) {
      LOGGER.error("failed to init database version for [" + context.getDatabase() + "].",
          exp);
    }
  }

}
