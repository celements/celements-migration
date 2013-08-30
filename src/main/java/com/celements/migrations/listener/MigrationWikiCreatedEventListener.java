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
package com.celements.migrations.listener;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.celements.migrations.ISubSystemMigrationCoordinator;
import com.xpn.xwiki.XWikiContext;

@Component("celements.migrations.WikiCreatedEventListener")
public class MigrationWikiCreatedEventListener implements EventListener {

  private static Log LOGGER = LogFactory.getFactory().getInstance(
      MigrationWikiCreatedEventListener.class);

  @Requirement
  ISubSystemMigrationCoordinator migrationCoordinator;

  @Requirement
  private Execution execution;

  protected XWikiContext getContext() {
    return (XWikiContext)execution.getContext().getProperty("xwikicontext");
  }

  public String getName() {
    return "celements.migrations.WikiCreatedEventListener";
  }

  public List<Event> getEvents() {
    LOGGER.trace("MigrationWikiCreatedEventListener getEvents called.");
    return Arrays.<Event>asList(new WikiCreatedEvent());
  }

  public void onEvent(Event event, Object source, Object data) {
    LOGGER.debug("enter onEvent in WikiCreatedEventListener.");
    String saveDbName = getContext().getDatabase();
    try {
      WikiEvent wikiEvent = (WikiEvent) event;
      String newDbName = wikiEvent.getWikiId();
      getContext().setDatabase(newDbName);
      LOGGER.info("received wikiEvent [" + wikiEvent.getClass() + "] for wikiId ["
          + newDbName + "] now executing initDatabaseVersions.");
      migrationCoordinator.initDatabaseVersions(getContext());
    } finally {
      LOGGER.debug("finishing onEvent in WikiCreatedEventListener for wikiId ["
          + getContext().getDatabase() + "].");
      getContext().setDatabase(saveDbName);
    }
  }

}
