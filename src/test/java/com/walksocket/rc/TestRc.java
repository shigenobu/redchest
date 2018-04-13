package com.walksocket.rc;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestRc {

  @Test
  public void test() throws RcServer.RcServerException, RcClient.RcClientException {
    RcLogger.setVerbose(true);
    RcDate.setAddMilliSeconds(32400000);

    RcServer server = new RcServer(new SeverCallback());
    server.start();

    int size = 5;
    List<RcClient> clients = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      clients.add(new RcClient(new ClientCallback(), "127.0.0.1", 8710));
      clients.get(i).connect();
    }

    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (int i = 0; i < clients.size(); i++) {
      clients.get(i).disconnect();
    }
    server.shutdown();
  }

  public class SeverCallback implements RcCallback {

    @Override
    public void onOpen(RcSession session) {
      RcLogger.debug(() -> String.format("server onOpen:%s %s", session.getRemoteAddress(), session));
//      session.setIdleMilliSeconds(3000);

      byte[] message = "hello".getBytes(StandardCharsets.UTF_8);
      try {
        session.send(message);
      } catch (RcSession.RcSendException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onMessage(RcSession session, byte[] message) {
      RcLogger.debug(() -> String.format("server onMessage:%s %s", session, new String(message)));

      byte[] msg = "hi client".getBytes(StandardCharsets.UTF_8);
      try {
        session.send(msg);
      } catch (RcSession.RcSendException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onClose(RcSession session, RcCloseReason reason) {
      RcLogger.debug(() -> String.format("server onClose:%s %s %s", session.getRemoteAddress(), session, reason));
    }
  }

  public class ClientCallback implements RcCallback {

    @Override
    public void onOpen(RcSession session) {
      RcLogger.debug(() -> String.format("cleint onOpen:%s %s", session.getLocalAddress(), session));

      session.setValue("cnt", 0);
    }

    @Override
    public void onMessage(RcSession session, byte[] message) {
      RcLogger.debug(() -> String.format("client onMessage:%s %s", session, new String(message)));

      int cnt = session.getValue("cnt", Integer.class);
      if (cnt < 3) {
        byte[] msg = ("hi server " + cnt).getBytes(StandardCharsets.UTF_8);
        try {
          session.send(msg);
//          try {
//            Thread.sleep(4000);
//          } catch (InterruptedException e) {
//            e.printStackTrace();
//          }
        } catch (RcSession.RcSendException e) {
          e.printStackTrace();
        }
        session.setValue("cnt", cnt + 1);
      } else {
        session.close();
      }
    }

    @Override
    public void onClose(RcSession session, RcCloseReason reason) {
      RcLogger.debug(() -> String.format("client onClose:%s %s %s", session.getLocalAddress(), session, reason));
    }
  }
}
