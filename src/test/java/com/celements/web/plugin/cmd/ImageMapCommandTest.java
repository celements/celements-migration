package com.celements.web.plugin.cmd;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

public class ImageMapCommandTest extends AbstractBridgedComponentTestCase {

  private ImageMapCommand imgMapCmd;
  private XWikiContext context;
  private XWiki xwiki;

  @Before
  public void setUp_ImageMapCommandTest() throws Exception {
    context = getContext();
    xwiki = createMock(XWiki.class);
    context.setWiki(xwiki);
    imgMapCmd = new ImageMapCommand(context);
  }

  @Test
  public void testGetImageUseMaps_simple() {
    String rteContent = "<img "
      + " src=\"/download/Content_attachments/FileBaseDoc/grundriss.png\""
      + " usemap=\"#objektwahlImg\">";
    replayAll();
    List<String> useMaps = imgMapCmd.getImageUseMaps(rteContent);
    assertNotNull(useMaps);
    assertEquals(Arrays.asList("objektwahlImg"), useMaps);
    assertEquals(1, useMaps.size());
    verifyAll();
  }

  @Test
  public void testGetImageUseMaps() {
    String rteContent = "<p>\n<img style=\"border-style: initial; border-color: initial;"
      + " border-image: initial; border-width: 0px;\""
      + " src=\"/download/Content_attachments/FileBaseDoc/grundriss.png\""
      + " border=\"0\" usemap=\"#objektwahlImg\">\n</p>";
    replayAll();
    List<String> useMaps = imgMapCmd.getImageUseMaps(rteContent);
    assertNotNull(useMaps);
    assertEquals(Arrays.asList("objektwahlImg"), useMaps);
    assertEquals(1, useMaps.size());
    verifyAll();
  }

  private void replayAll(Object ... mocks) {
    replay(xwiki);
    replay(mocks);
  }

  private void verifyAll(Object ... mocks) {
    verify(xwiki);
    verify(mocks);
  }

}